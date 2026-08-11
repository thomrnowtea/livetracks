package com.thomrnowtea.livetracks.audio

internal object NativeAudioBridge {
    init {
        System.loadLibrary("livetracks_audio")
    }

    external fun openOutput(requestedSampleRate: Int, requestedChannels: Int): Boolean
    external fun closeOutput()
    external fun setToneEnabled(enabled: Boolean)
    external fun loadWavTrack(trackIndex: Int, fileDescriptor: Int): Int
    external fun clearTracks()
    external fun resetTransport()
    external fun seekTransport(frame: Long)
    external fun setTrackGain(trackIndex: Int, gain: Float)
    external fun setTrackPan(trackIndex: Int, pan: Float)
    external fun setTrackMuted(trackIndex: Int, muted: Boolean)
    external fun setTrackSoloed(trackIndex: Int, soloed: Boolean)
    external fun setTrackSends(trackIndex: Int, mainSend: Float, monitorSend: Float)
    external fun setTrackStartOffset(trackIndex: Int, outputFrames: Long)
    external fun setTrackSourceRange(trackIndex: Int, startFrame: Long, endFrameExclusive: Long)
    external fun setTimelineDuration(outputFrames: Long)
    external fun setMasterGainPan(gain: Float, pan: Float)
    external fun setOutputMode(mode: Int)
    external fun configureMetronome(
        enabled: Boolean,
        bpm: Double,
        numerator: Int,
        denominator: Int,
        gain: Float,
        mainEnabled: Boolean,
    )
    external fun trackMetadata(trackIndex: Int): LongArray
    external fun trackPeaks(): FloatArray
    external fun panic()
    external fun diagnostics(): LongArray
    external fun lastError(): String
}

data class EngineDiagnostics(
    val outputOpen: Boolean = false,
    val toneEnabled: Boolean = false,
    val requestedSampleRate: Int = 0,
    val actualSampleRate: Int = 0,
    val requestedChannels: Int = 2,
    val actualChannels: Int = 0,
    val framesPerBurst: Int = 0,
    val bufferSizeFrames: Int = 0,
    val xRuns: Int = 0,
    val renderedFrames: Long = 0,
    val durationFrames: Long = 0,
    val lastError: String = "",
)

class NativeAudioController {
    fun open(requestedSampleRate: Int): EngineDiagnostics {
        NativeAudioBridge.openOutput(requestedSampleRate, 2)
        return diagnostics(requestedSampleRate)
    }

    fun setToneEnabled(enabled: Boolean) = NativeAudioBridge.setToneEnabled(enabled)
    fun loadWavTrack(trackIndex: Int, fileDescriptor: Int): Int = NativeAudioBridge.loadWavTrack(trackIndex, fileDescriptor)
    fun clearTracks() = NativeAudioBridge.clearTracks()
    fun resetTransport() = NativeAudioBridge.resetTransport()
    fun seekTransport(frame: Long) = NativeAudioBridge.seekTransport(frame)
    fun setTrackGain(trackIndex: Int, gain: Float) = NativeAudioBridge.setTrackGain(trackIndex, gain)
    fun setTrackPan(trackIndex: Int, pan: Float) = NativeAudioBridge.setTrackPan(trackIndex, pan)
    fun setTrackMuted(trackIndex: Int, muted: Boolean) = NativeAudioBridge.setTrackMuted(trackIndex, muted)
    fun setTrackSoloed(trackIndex: Int, soloed: Boolean) = NativeAudioBridge.setTrackSoloed(trackIndex, soloed)
    fun setTrackSends(trackIndex: Int, mainSend: Float, monitorSend: Float) = NativeAudioBridge.setTrackSends(trackIndex, mainSend, monitorSend)
    fun setTrackStartOffset(trackIndex: Int, outputFrames: Long) = NativeAudioBridge.setTrackStartOffset(trackIndex, outputFrames)
    fun setTrackSourceRange(trackIndex: Int, startFrame: Long, endFrameExclusive: Long) =
        NativeAudioBridge.setTrackSourceRange(trackIndex, startFrame, endFrameExclusive)
    fun setTimelineDuration(outputFrames: Long) = NativeAudioBridge.setTimelineDuration(outputFrames)
    fun setMasterGainPan(gain: Float, pan: Float) = NativeAudioBridge.setMasterGainPan(gain, pan)
    fun setOutputMode(stereoSplit: Boolean) = NativeAudioBridge.setOutputMode(if (stereoSplit) 1 else 0)
    fun configureMetronome(
        enabled: Boolean,
        bpm: Double,
        numerator: Int,
        denominator: Int,
        gain: Float,
        mainEnabled: Boolean,
    ) = NativeAudioBridge.configureMetronome(enabled, bpm, numerator, denominator, gain, mainEnabled)
    fun trackMetadata(trackIndex: Int): LongArray = NativeAudioBridge.trackMetadata(trackIndex)
    fun trackPeaks(): FloatArray = NativeAudioBridge.trackPeaks()
    fun panic() = NativeAudioBridge.panic()
    fun close() = NativeAudioBridge.closeOutput()

    fun diagnostics(requestedSampleRate: Int): EngineDiagnostics {
        val raw = NativeAudioBridge.diagnostics()
        return EngineDiagnostics(
            outputOpen = raw.getOrElse(0) { 0 } != 0L,
            toneEnabled = raw.getOrElse(1) { 0 } != 0L,
            requestedSampleRate = requestedSampleRate,
            actualSampleRate = raw.getOrElse(2) { 0 }.toInt(),
            requestedChannels = 2,
            actualChannels = raw.getOrElse(3) { 0 }.toInt(),
            framesPerBurst = raw.getOrElse(4) { 0 }.toInt(),
            bufferSizeFrames = raw.getOrElse(5) { 0 }.toInt(),
            xRuns = raw.getOrElse(6) { 0 }.toInt(),
            renderedFrames = raw.getOrElse(7) { 0 },
            durationFrames = raw.getOrElse(8) { 0 },
            lastError = NativeAudioBridge.lastError(),
        )
    }
}
