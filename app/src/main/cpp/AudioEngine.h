#pragma once

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include <oboe/Oboe.h>

class AudioEngine final : public oboe::AudioStreamDataCallback,
                          public oboe::AudioStreamErrorCallback {
public:
    static constexpr int32_t kMaxTracks = 48;

    AudioEngine();
    bool open(int32_t requestedSampleRate, int32_t requestedChannels);
    void close();
    int32_t loadWavTrack(int32_t trackIndex, int fileDescriptor);
    void clearTracks();
    void setPlaying(bool enabled);
    void resetTransport();
    void seekTransport(int64_t frame) noexcept;
    void setTrackGain(int32_t trackIndex, float gain) noexcept;
    void setTrackPan(int32_t trackIndex, float pan) noexcept;
    void setTrackMuted(int32_t trackIndex, bool muted) noexcept;
    void setTrackSoloed(int32_t trackIndex, bool soloed) noexcept;
    void setTrackSoloSafe(int32_t trackIndex, bool soloSafe) noexcept;
    void setTrackSends(int32_t trackIndex, float mainSend, float monitorSend) noexcept;
    void setTrackStartOffset(int32_t trackIndex, int64_t outputFrames);
    void setTrackSourceRange(int32_t trackIndex, int64_t startFrame, int64_t endFrameExclusive);
    void setTimelineDuration(int64_t outputFrames);
    void setMasterGainPan(float gain, float pan) noexcept;
    void setOutputMode(int32_t mode) noexcept;
    void configureMetronome(bool enabled, double bpm, int32_t numerator,
                            int32_t denominator, float gain, bool mainEnabled) noexcept;
    void panic();

    bool isOpen() const noexcept { return open_.load(std::memory_order_acquire); }
    bool isPlaying() const noexcept { return playing_.load(std::memory_order_relaxed); }
    int32_t sampleRate() const noexcept { return sampleRate_.load(std::memory_order_relaxed); }
    int32_t channelCount() const noexcept { return channelCount_.load(std::memory_order_relaxed); }
    int32_t framesPerBurst() const noexcept { return framesPerBurst_.load(std::memory_order_relaxed); }
    int32_t bufferSizeFrames() const;
    int32_t xRunCount() const;
    int64_t transportFrames() const noexcept { return transportFrames_.load(std::memory_order_relaxed); }
    int64_t durationFrames() const noexcept { return durationFrames_.load(std::memory_order_relaxed); }
    std::array<int64_t, 3> trackMetadata(int32_t trackIndex) const noexcept;
    std::array<float, kMaxTracks> trackPeaks() const noexcept;
    std::string lastError() const;

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void*, int32_t) override;
    void onErrorAfterClose(oboe::AudioStream*, oboe::Result) override;

private:
    struct WavTrack {
        std::vector<float> samples;
        int32_t channels = 0;
        int32_t sampleRate = 0;
        int64_t frameCount = 0;
    };

    void setError(const std::string& message);
    void recalculateDurationLocked() noexcept;

    mutable std::mutex controlMutex_;
    mutable std::mutex errorMutex_;
    std::shared_ptr<oboe::AudioStream> stream_;
    std::string lastError_;
    std::array<WavTrack, kMaxTracks> tracks_{};
    std::array<std::atomic<float>, kMaxTracks> trackGains_{};
    std::array<std::atomic<float>, kMaxTracks> trackPans_{};
    std::array<std::atomic<float>, kMaxTracks> mainSends_{};
    std::array<std::atomic<float>, kMaxTracks> monitorSends_{};
    std::array<std::atomic<bool>, kMaxTracks> trackMuted_{};
    std::array<std::atomic<bool>, kMaxTracks> trackSoloed_{};
    std::array<std::atomic<bool>, kMaxTracks> trackSoloSafe_{};
    std::array<std::atomic<float>, kMaxTracks> trackPeaks_{};
    std::array<std::atomic<int64_t>, kMaxTracks> trackStartOffsets_{};
    std::array<std::atomic<int64_t>, kMaxTracks> trackSourceStartFrames_{};
    std::array<std::atomic<int64_t>, kMaxTracks> trackSourceEndFrames_{};
    std::atomic<int32_t> loadedTrackCount_{0};
    std::atomic<int32_t> outputMode_{0};
    std::atomic<float> masterGain_{1.0F};
    std::atomic<float> masterPan_{0.0F};
    std::atomic<bool> metronomeEnabled_{false};
    std::atomic<double> metronomeBpm_{120.0};
    std::atomic<int32_t> metronomeNumerator_{4};
    std::atomic<int32_t> metronomeDenominator_{4};
    std::atomic<float> metronomeGain_{0.25118864F};
    std::atomic<bool> metronomeMainEnabled_{false};
    std::atomic<bool> open_{false};
    std::atomic<bool> playing_{false};
    std::atomic<int32_t> sampleRate_{0};
    std::atomic<int32_t> channelCount_{0};
    std::atomic<int32_t> framesPerBurst_{0};
    std::atomic<int64_t> transportFrames_{0};
    std::atomic<int64_t> durationFrames_{0};
    std::atomic<int64_t> minimumTimelineDurationFrames_{0};
};
