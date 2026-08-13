#include "AudioEngine.h"

#include <jni.h>

namespace {
AudioEngine engine;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_openOutput(
    JNIEnv*,
    jobject,
    jint requestedSampleRate,
    jint requestedChannels
) {
    return engine.open(requestedSampleRate, requestedChannels) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_closeOutput(JNIEnv*, jobject) {
    engine.close();
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setToneEnabled(
    JNIEnv*,
    jobject,
    jboolean enabled
) {
    engine.setPlaying(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_loadWavTrack(
    JNIEnv*, jobject, jint trackIndex, jint fileDescriptor
) {
    return engine.loadWavTrack(trackIndex, fileDescriptor);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_clearTracks(JNIEnv*, jobject) {
    engine.clearTracks();
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_resetTransport(JNIEnv*, jobject) {
    engine.resetTransport();
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_seekTransport(
    JNIEnv*, jobject, jlong frame
) {
    engine.seekTransport(frame);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackGain(
    JNIEnv*, jobject, jint trackIndex, jfloat gain
) {
    engine.setTrackGain(trackIndex, gain);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackPan(
    JNIEnv*, jobject, jint trackIndex, jfloat pan
) {
    engine.setTrackPan(trackIndex, pan);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackMuted(
    JNIEnv*, jobject, jint trackIndex, jboolean muted
) {
    engine.setTrackMuted(trackIndex, muted == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackSoloed(
    JNIEnv*, jobject, jint trackIndex, jboolean soloed
) {
    engine.setTrackSoloed(trackIndex, soloed == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackSoloSafe(
    JNIEnv*, jobject, jint trackIndex, jboolean soloSafe
) {
    engine.setTrackSoloSafe(trackIndex, soloSafe == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackSends(
    JNIEnv*, jobject, jint trackIndex, jfloat mainSend, jfloat monitorSend
) {
    engine.setTrackSends(trackIndex, mainSend, monitorSend);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackStartOffset(
    JNIEnv*, jobject, jint trackIndex, jlong outputFrames
) {
    engine.setTrackStartOffset(trackIndex, outputFrames);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTrackSourceRange(
    JNIEnv*, jobject, jint trackIndex, jlong startFrame, jlong endFrameExclusive
) {
    engine.setTrackSourceRange(trackIndex, startFrame, endFrameExclusive);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setTimelineDuration(
    JNIEnv*, jobject, jlong outputFrames
) {
    engine.setTimelineDuration(outputFrames);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setMasterGainPan(
    JNIEnv*, jobject, jfloat gain, jfloat pan
) {
    engine.setMasterGainPan(gain, pan);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_setOutputMode(
    JNIEnv*, jobject, jint mode
) {
    engine.setOutputMode(mode);
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_configureMetronome(
    JNIEnv*, jobject, jboolean enabled, jdouble bpm, jint numerator,
    jint denominator, jfloat gain, jboolean mainEnabled
) {
    engine.configureMetronome(
        enabled == JNI_TRUE, bpm, numerator, denominator, gain, mainEnabled == JNI_TRUE);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_trackMetadata(
    JNIEnv* env, jobject, jint trackIndex
) {
    const auto metadata = engine.trackMetadata(trackIndex);
    jlongArray result = env->NewLongArray(metadata.size());
    if (result != nullptr) env->SetLongArrayRegion(result, 0, metadata.size(), metadata.data());
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_trackPeaks(JNIEnv* env, jobject) {
    const auto peaks = engine.trackPeaks();
    jfloatArray result = env->NewFloatArray(AudioEngine::kMaxTracks);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, AudioEngine::kMaxTracks, peaks.data());
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_panic(JNIEnv*, jobject) {
    engine.panic();
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_diagnostics(JNIEnv* env, jobject) {
    constexpr jsize count = 9;
    const jlong values[count] = {
        engine.isOpen() ? 1 : 0,
        engine.isPlaying() ? 1 : 0,
        engine.sampleRate(),
        engine.channelCount(),
        engine.framesPerBurst(),
        engine.bufferSizeFrames(),
        engine.xRunCount(),
        engine.transportFrames(),
        engine.durationFrames(),
    };
    jlongArray result = env->NewLongArray(count);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, count, values);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_thomrnowtea_livetracks_audio_NativeAudioBridge_lastError(JNIEnv* env, jobject) {
    const std::string error = engine.lastError();
    return env->NewStringUTF(error.c_str());
}
