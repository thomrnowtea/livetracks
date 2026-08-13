#include "AudioEngine.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <sys/stat.h>
#include <unistd.h>

namespace {
constexpr int64_t kMaximumWavBytes = 512LL * 1024LL * 1024LL;
constexpr double kTwoPi = 6.28318530717958647692;

uint16_t readLe16(const uint8_t* value) {
    return static_cast<uint16_t>(value[0] | (value[1] << 8U));
}

uint32_t readLe32(const uint8_t* value) {
    return static_cast<uint32_t>(value[0]) |
        (static_cast<uint32_t>(value[1]) << 8U) |
        (static_cast<uint32_t>(value[2]) << 16U) |
        (static_cast<uint32_t>(value[3]) << 24U);
}

bool readExact(int fd, int64_t offset, void* target, size_t size) {
    auto* destination = static_cast<uint8_t*>(target);
    size_t complete = 0;
    while (complete < size) {
        const ssize_t result = pread(fd, destination + complete, size - complete, offset + complete);
        if (result <= 0) return false;
        complete += static_cast<size_t>(result);
    }
    return true;
}

float limited(float sample) {
    return std::clamp(sample, -0.98F, 0.98F);
}
}

AudioEngine::AudioEngine() {
    for (int32_t index = 0; index < kMaxTracks; ++index) {
        trackGains_[index].store(1.0F, std::memory_order_relaxed);
        trackPans_[index].store(0.0F, std::memory_order_relaxed);
        mainSends_[index].store(1.0F, std::memory_order_relaxed);
        monitorSends_[index].store(0.5F, std::memory_order_relaxed);
        trackMuted_[index].store(false, std::memory_order_relaxed);
        trackSoloed_[index].store(false, std::memory_order_relaxed);
        trackSoloSafe_[index].store(false, std::memory_order_relaxed);
        trackPeaks_[index].store(0.0F, std::memory_order_relaxed);
        trackStartOffsets_[index].store(0, std::memory_order_relaxed);
        trackSourceStartFrames_[index].store(0, std::memory_order_relaxed);
        trackSourceEndFrames_[index].store(-1, std::memory_order_relaxed);
    }
}

bool AudioEngine::open(int32_t requestedSampleRate, int32_t requestedChannels) {
    std::scoped_lock lock(controlMutex_);
    if (stream_) return true;
    transportFrames_.store(0, std::memory_order_relaxed);
    playing_.store(false, std::memory_order_relaxed);
    setError("");

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(requestedChannels)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormatConversionAllowed(true)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
        ->setDataCallback(this)
        ->setErrorCallback(this);
    if (requestedSampleRate > 0) builder.setSampleRate(requestedSampleRate);
    auto result = builder.openStream(stream_);
    if (result != oboe::Result::OK || !stream_) {
        setError(std::string("Open failed: ") + oboe::convertToText(result));
        stream_.reset();
        return false;
    }
    sampleRate_.store(stream_->getSampleRate(), std::memory_order_relaxed);
    channelCount_.store(stream_->getChannelCount(), std::memory_order_relaxed);
    framesPerBurst_.store(stream_->getFramesPerBurst(), std::memory_order_relaxed);
    open_.store(true, std::memory_order_release);
    return true;
}

void AudioEngine::close() {
    std::scoped_lock lock(controlMutex_);
    playing_.store(false, std::memory_order_relaxed);
    open_.store(false, std::memory_order_release);
    if (stream_) {
        stream_->requestStop();
        stream_->close();
        stream_.reset();
    }
    sampleRate_.store(0, std::memory_order_relaxed);
    channelCount_.store(0, std::memory_order_relaxed);
    framesPerBurst_.store(0, std::memory_order_relaxed);
}

int32_t AudioEngine::loadWavTrack(int32_t trackIndex, int fd) {
    if (trackIndex < 0 || trackIndex >= kMaxTracks || fd < 0 || isPlaying()) return -1;
    struct stat fileInfo{};
    if (fstat(fd, &fileInfo) != 0 || fileInfo.st_size < 44 || fileInfo.st_size > kMaximumWavBytes) return -2;
    uint8_t riff[12]{};
    if (!readExact(fd, 0, riff, sizeof(riff)) ||
        std::memcmp(riff, "RIFF", 4) != 0 || std::memcmp(riff + 8, "WAVE", 4) != 0) return -3;

    uint16_t format = 0;
    uint16_t channels = 0;
    uint32_t sourceRate = 0;
    uint16_t bits = 0;
    int64_t dataOffset = -1;
    uint32_t dataSize = 0;
    int64_t offset = 12;
    while (offset + 8 <= fileInfo.st_size) {
        uint8_t header[8]{};
        if (!readExact(fd, offset, header, sizeof(header))) return -4;
        const uint32_t chunkSize = readLe32(header + 4);
        const int64_t contentOffset = offset + 8;
        if (contentOffset + chunkSize > fileInfo.st_size) return -4;
        if (std::memcmp(header, "fmt ", 4) == 0 && chunkSize >= 16) {
            uint8_t fmt[16]{};
            if (!readExact(fd, contentOffset, fmt, sizeof(fmt))) return -4;
            format = readLe16(fmt);
            channels = readLe16(fmt + 2);
            sourceRate = readLe32(fmt + 4);
            bits = readLe16(fmt + 14);
        } else if (std::memcmp(header, "data", 4) == 0) {
            dataOffset = contentOffset;
            dataSize = chunkSize;
        }
        offset = contentOffset + chunkSize + (chunkSize & 1U);
    }
    if (dataOffset < 0 || channels < 1 || channels > 2 || sourceRate == 0) return -5;
    const bool supportedPcm = format == 1 && (bits == 8 || bits == 16 || bits == 24 || bits == 32);
    const bool supportedFloat = format == 3 && bits == 32;
    if (!supportedPcm && !supportedFloat) return -6;
    const int32_t bytesPerSample = bits / 8;
    const int64_t sampleCount = dataSize / bytesPerSample;
    if (sampleCount <= 0 || sampleCount % channels != 0) return -7;

    std::vector<uint8_t> bytes(dataSize);
    if (!readExact(fd, dataOffset, bytes.data(), bytes.size())) return -8;
    WavTrack decoded;
    decoded.samples.resize(static_cast<size_t>(sampleCount));
    decoded.channels = channels;
    decoded.sampleRate = static_cast<int32_t>(sourceRate);
    decoded.frameCount = sampleCount / channels;
    for (int64_t sample = 0; sample < sampleCount; ++sample) {
        const uint8_t* source = bytes.data() + sample * bytesPerSample;
        float value = 0.0F;
        if (format == 3) {
            std::memcpy(&value, source, sizeof(float));
        } else if (bits == 8) {
            value = (static_cast<int32_t>(source[0]) - 128) / 128.0F;
        } else if (bits == 16) {
            value = static_cast<int16_t>(readLe16(source)) / 32768.0F;
        } else if (bits == 24) {
            int32_t integer = source[0] | (source[1] << 8U) | (source[2] << 16U);
            if ((integer & 0x800000) != 0) integer |= ~0xFFFFFF;
            value = integer / 8388608.0F;
        } else {
            value = static_cast<int32_t>(readLe32(source)) / 2147483648.0F;
        }
        decoded.samples[static_cast<size_t>(sample)] = std::clamp(value, -1.0F, 1.0F);
    }

    std::scoped_lock lock(controlMutex_);
    tracks_[trackIndex] = std::move(decoded);
    loadedTrackCount_.store(std::max(loadedTrackCount_.load(), trackIndex + 1), std::memory_order_release);
    recalculateDurationLocked();
    return 0;
}

void AudioEngine::clearTracks() {
    if (isPlaying()) return;
    std::scoped_lock lock(controlMutex_);
    loadedTrackCount_.store(0, std::memory_order_release);
    durationFrames_.store(0, std::memory_order_relaxed);
    minimumTimelineDurationFrames_.store(0, std::memory_order_relaxed);
    for (auto& track : tracks_) track = WavTrack{};
    for (auto& peak : trackPeaks_) peak.store(0.0F, std::memory_order_relaxed);
    for (auto& soloSafe : trackSoloSafe_) soloSafe.store(false, std::memory_order_relaxed);
    for (auto& offset : trackStartOffsets_) offset.store(0, std::memory_order_relaxed);
    for (auto& start : trackSourceStartFrames_) start.store(0, std::memory_order_relaxed);
    for (auto& end : trackSourceEndFrames_) end.store(-1, std::memory_order_relaxed);
}

void AudioEngine::setPlaying(bool enabled) {
    std::scoped_lock lock(controlMutex_);
    if (!stream_ || !isOpen() || durationFrames_.load(std::memory_order_relaxed) <= 0) {
        playing_.store(false, std::memory_order_relaxed);
        return;
    }
    if (!enabled) {
        playing_.store(false, std::memory_order_relaxed);
        const auto state = stream_->getState();
        if (state == oboe::StreamState::Started || state == oboe::StreamState::Starting) {
            stream_->requestPause();
        }
        return;
    }
    playing_.store(true, std::memory_order_relaxed);
    const auto result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        playing_.store(false, std::memory_order_relaxed);
        setError(std::string("Start failed: ") + oboe::convertToText(result));
    }
}

void AudioEngine::resetTransport() {
    setPlaying(false);
    transportFrames_.store(0, std::memory_order_relaxed);
    for (auto& peak : trackPeaks_) peak.store(0.0F, std::memory_order_relaxed);
}

void AudioEngine::seekTransport(int64_t frame) noexcept {
    transportFrames_.store(
        std::clamp<int64_t>(frame, 0, durationFrames_.load(std::memory_order_relaxed)),
        std::memory_order_relaxed);
    for (auto& peak : trackPeaks_) peak.store(0.0F, std::memory_order_relaxed);
}

void AudioEngine::setTrackGain(int32_t index, float value) noexcept { if (index >= 0 && index < kMaxTracks) trackGains_[index].store(std::clamp(value, 0.0F, 2.0F)); }
void AudioEngine::setTrackPan(int32_t index, float value) noexcept { if (index >= 0 && index < kMaxTracks) trackPans_[index].store(std::clamp(value, -1.0F, 1.0F)); }
void AudioEngine::setTrackMuted(int32_t index, bool value) noexcept { if (index >= 0 && index < kMaxTracks) trackMuted_[index].store(value); }
void AudioEngine::setTrackSoloed(int32_t index, bool value) noexcept { if (index >= 0 && index < kMaxTracks) trackSoloed_[index].store(value); }
void AudioEngine::setTrackSoloSafe(int32_t index, bool value) noexcept { if (index >= 0 && index < kMaxTracks) trackSoloSafe_[index].store(value); }
void AudioEngine::setTrackSends(int32_t index, float main, float monitor) noexcept {
    if (index < 0 || index >= kMaxTracks) return;
    mainSends_[index].store(std::clamp(main, 0.0F, 2.0F));
    monitorSends_[index].store(std::clamp(monitor, 0.0F, 2.0F));
}
void AudioEngine::setTrackStartOffset(int32_t index, int64_t outputFrames) {
    if (index < 0 || index >= kMaxTracks) return;
    trackStartOffsets_[index].store(std::max<int64_t>(0, outputFrames), std::memory_order_relaxed);
    std::scoped_lock lock(controlMutex_);
    recalculateDurationLocked();
}
void AudioEngine::setTrackSourceRange(int32_t index, int64_t startFrame, int64_t endFrameExclusive) {
    if (index < 0 || index >= kMaxTracks) return;
    trackSourceStartFrames_[index].store(std::max<int64_t>(0, startFrame), std::memory_order_relaxed);
    trackSourceEndFrames_[index].store(endFrameExclusive, std::memory_order_relaxed);
    std::scoped_lock lock(controlMutex_);
    recalculateDurationLocked();
}
void AudioEngine::setTimelineDuration(int64_t outputFrames) {
    minimumTimelineDurationFrames_.store(std::max<int64_t>(0, outputFrames), std::memory_order_relaxed);
    std::scoped_lock lock(controlMutex_);
    recalculateDurationLocked();
}
void AudioEngine::setMasterGainPan(float gain, float pan) noexcept {
    masterGain_.store(std::clamp(gain, 0.0F, 2.0F), std::memory_order_relaxed);
    masterPan_.store(std::clamp(pan, -1.0F, 1.0F), std::memory_order_relaxed);
}
void AudioEngine::setOutputMode(int32_t mode) noexcept { outputMode_.store(mode == 1 ? 1 : 0); }
void AudioEngine::configureMetronome(bool enabled, double bpm, int32_t numerator,
                                    int32_t denominator, float gain, bool mainEnabled) noexcept {
    metronomeEnabled_.store(enabled, std::memory_order_relaxed);
    metronomeBpm_.store(std::clamp(bpm, 20.0, 400.0), std::memory_order_relaxed);
    metronomeNumerator_.store(std::clamp(numerator, 1, 32), std::memory_order_relaxed);
    metronomeDenominator_.store(std::clamp(denominator, 1, 32), std::memory_order_relaxed);
    metronomeGain_.store(std::clamp(gain, 0.0F, 2.0F), std::memory_order_relaxed);
    metronomeMainEnabled_.store(mainEnabled, std::memory_order_relaxed);
}
void AudioEngine::panic() { setPlaying(false); }

void AudioEngine::recalculateDurationLocked() noexcept {
    const int32_t outputRate = sampleRate_.load(std::memory_order_relaxed);
    int64_t duration = minimumTimelineDurationFrames_.load(std::memory_order_relaxed);
    if (outputRate > 0) {
        const int32_t count = loadedTrackCount_.load(std::memory_order_relaxed);
        for (int32_t index = 0; index < count; ++index) {
            const auto& track = tracks_[index];
            if (track.sampleRate <= 0) continue;
            const int64_t sourceStart = std::clamp<int64_t>(
                trackSourceStartFrames_[index].load(std::memory_order_relaxed), 0, track.frameCount);
            const int64_t configuredEnd = trackSourceEndFrames_[index].load(std::memory_order_relaxed);
            const int64_t sourceEnd = configuredEnd < 0
                ? track.frameCount
                : std::clamp<int64_t>(configuredEnd, sourceStart, track.frameCount);
            const int64_t playableFrames = sourceEnd - sourceStart;
            if (playableFrames <= 0) continue;
            const int64_t audioFrames = static_cast<int64_t>(std::ceil(
                static_cast<double>(playableFrames) * outputRate / track.sampleRate));
            duration = std::max(duration, trackStartOffsets_[index].load(std::memory_order_relaxed) + audioFrames);
        }
    }
    durationFrames_.store(duration, std::memory_order_relaxed);
}

std::array<int64_t, 3> AudioEngine::trackMetadata(int32_t index) const noexcept {
    if (index < 0 || index >= kMaxTracks) return {};
    const auto& track = tracks_[index];
    return {track.channels, track.sampleRate, track.frameCount};
}

std::array<float, AudioEngine::kMaxTracks> AudioEngine::trackPeaks() const noexcept {
    std::array<float, kMaxTracks> result{};
    for (int32_t i = 0; i < kMaxTracks; ++i) result[i] = trackPeaks_[i].load();
    return result;
}

int32_t AudioEngine::bufferSizeFrames() const { std::scoped_lock lock(controlMutex_); return stream_ ? stream_->getBufferSizeInFrames() : 0; }
int32_t AudioEngine::xRunCount() const { std::scoped_lock lock(controlMutex_); if (!stream_) return 0; const auto value = stream_->getXRunCount(); return value ? value.value() : 0; }
std::string AudioEngine::lastError() const { std::scoped_lock lock(errorMutex_); return lastError_; }

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) {
    auto* output = static_cast<float*>(audioData);
    const int32_t outputChannels = stream->getChannelCount();
    std::fill_n(output, numFrames * outputChannels, 0.0F);
    if (!playing_.load(std::memory_order_relaxed)) {
        for (auto& peak : trackPeaks_) peak.store(peak.load() * 0.7F);
        return oboe::DataCallbackResult::Continue;
    }

    const int32_t count = loadedTrackCount_.load(std::memory_order_acquire);
    const int32_t outputRate = stream->getSampleRate();
    const int64_t startFrame = transportFrames_.load(std::memory_order_relaxed);
    std::array<float, kMaxTracks> callbackPeaks{};
    bool anySolo = false;
    for (int32_t i = 0; i < count; ++i) {
        anySolo = anySolo || (!trackSoloSafe_[i].load() && trackSoloed_[i].load());
    }

    for (int32_t frame = 0; frame < numFrames; ++frame) {
        const int64_t transportFrame = startFrame + frame;
        float mainLeft = 0.0F;
        float mainRight = 0.0F;
        float monitor = 0.0F;
        for (int32_t index = 0; index < count; ++index) {
            const auto& track = tracks_[index];
            if (track.frameCount == 0 || track.sampleRate == 0) continue;
            const int64_t relativeFrame = transportFrame - trackStartOffsets_[index].load(std::memory_order_relaxed);
            if (relativeFrame < 0) continue;
            const int64_t sourceStart = std::clamp<int64_t>(
                trackSourceStartFrames_[index].load(std::memory_order_relaxed), 0, track.frameCount);
            const int64_t configuredEnd = trackSourceEndFrames_[index].load(std::memory_order_relaxed);
            const int64_t sourceEnd = configuredEnd < 0
                ? track.frameCount
                : std::clamp<int64_t>(configuredEnd, sourceStart, track.frameCount);
            if (sourceEnd <= sourceStart) continue;
            const double sourcePosition = sourceStart + static_cast<double>(relativeFrame) * track.sampleRate / outputRate;
            const int64_t sourceFrame = static_cast<int64_t>(sourcePosition);
            if (sourceFrame >= sourceEnd) continue;
            if (trackMuted_[index].load() || (anySolo && !trackSoloed_[index].load() && !trackSoloSafe_[index].load())) continue;
            const int64_t nextFrame = std::min(sourceFrame + 1, sourceEnd - 1);
            const float fraction = static_cast<float>(sourcePosition - sourceFrame);
            auto sampleAt = [&](int64_t frameIndex, int32_t channel) {
                const int32_t selectedChannel = std::min(channel, track.channels - 1);
                return track.samples[static_cast<size_t>(frameIndex * track.channels + selectedChannel)];
            };
            float left = sampleAt(sourceFrame, 0) * (1.0F - fraction) + sampleAt(nextFrame, 0) * fraction;
            float right = sampleAt(sourceFrame, 1) * (1.0F - fraction) + sampleAt(nextFrame, 1) * fraction;
            const float gain = trackGains_[index].load();
            left *= gain;
            right *= gain;
            const float pan = trackPans_[index].load();
            if (pan < 0.0F) right *= 1.0F + pan; else left *= 1.0F - pan;
            callbackPeaks[index] = std::max(callbackPeaks[index], std::max(std::abs(left), std::abs(right)));
            const float mainSend = mainSends_[index].load();
            const float monitorSend = monitorSends_[index].load();
            mainLeft += left * mainSend;
            mainRight += right * mainSend;
            monitor += (left + right) * 0.5F * monitorSend;
        }
        const float masterGain = masterGain_.load(std::memory_order_relaxed);
        const float masterPan = masterPan_.load(std::memory_order_relaxed);
        mainLeft *= masterGain * (masterPan > 0.0F ? 1.0F - masterPan : 1.0F);
        mainRight *= masterGain * (masterPan < 0.0F ? 1.0F + masterPan : 1.0F);
        monitor *= masterGain;
        if (metronomeEnabled_.load(std::memory_order_relaxed)) {
            const double bpm = metronomeBpm_.load(std::memory_order_relaxed);
            const int32_t denominator = metronomeDenominator_.load(std::memory_order_relaxed);
            const double framesPerBeat = outputRate * 60.0 / bpm * 4.0 / denominator;
            const int64_t beatIndex = static_cast<int64_t>(std::floor(transportFrame / framesPerBeat));
            const double frameInBeat = transportFrame - beatIndex * framesPerBeat;
            const double clickFrames = outputRate * 0.035;
            if (frameInBeat >= 0.0 && frameInBeat < clickFrames) {
                const bool accent = beatIndex % metronomeNumerator_.load(std::memory_order_relaxed) == 0;
                const double seconds = frameInBeat / outputRate;
                const double frequency = accent ? 2200.0 : 1500.0;
                const float click = static_cast<float>(
                    std::sin(kTwoPi * frequency * seconds) * std::exp(-90.0 * seconds)) *
                    metronomeGain_.load(std::memory_order_relaxed);
                monitor += click;
                if (metronomeMainEnabled_.load(std::memory_order_relaxed)) {
                    mainLeft += click;
                    mainRight += click;
                }
            }
        }
        if (outputMode_.load() == 1) {
            output[frame * outputChannels] = limited((mainLeft + mainRight) * 0.5F);
            if (outputChannels > 1) output[frame * outputChannels + 1] = limited(monitor);
        } else {
            output[frame * outputChannels] = limited(mainLeft);
            if (outputChannels > 1) output[frame * outputChannels + 1] = limited(mainRight);
        }
    }
    for (int32_t i = 0; i < count; ++i) trackPeaks_[i].store(std::max(trackPeaks_[i].load() * 0.82F, callbackPeaks[i]));
    const int64_t duration = durationFrames_.load(std::memory_order_relaxed);
    const int64_t nextFrame = std::min(startFrame + numFrames, duration);
    transportFrames_.store(nextFrame, std::memory_order_relaxed);
    if (duration > 0 && nextFrame >= duration) playing_.store(false, std::memory_order_relaxed);
    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream*, oboe::Result error) {
    open_.store(false);
    playing_.store(false);
    setError(std::string("Stream closed after error: ") + oboe::convertToText(error));
}

void AudioEngine::setError(const std::string& message) { std::scoped_lock lock(errorMutex_); lastError_ = message; }
