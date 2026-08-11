package com.thomrnowtea.livetracks.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToLong

const val SILENCE_DB: Float = Float.NEGATIVE_INFINITY
const val TIMELINE_SAMPLE_RATE: Int = 48_000

enum class TrackType { MUSIC, CLICK, CUE, VIDEO_AUDIO_DISABLED, OTHER }

data class SourceMetadata(
    val channelCount: Int,
    val sampleRate: Int,
    val durationFrames: Long,
)

data class Track(
    val id: String,
    val name: String,
    val sourceUri: String?,
    val sourceMetadata: SourceMetadata?,
    /** First source frame rendered by this clip. */
    val sourceStartFrame: Long = 0,
    /** Exclusive source end; null uses the source's full duration. */
    val sourceEndFrameExclusive: Long? = null,
    /** Absolute entry point on the master-track timeline, expressed at 48 kHz. */
    val startOffsetFrames: Long,
    val gainDb: Float,
    val pan: Float,
    val muted: Boolean,
    val soloed: Boolean,
    val enabled: Boolean,
    val mainSendDb: Float,
    val monitorSendDb: Float,
    val type: TrackType,
) {
    init {
        require(pan in -1f..1f)
        require(startOffsetFrames >= 0)
        require(sourceStartFrame >= 0)
        require(sourceEndFrameExclusive == null || sourceEndFrameExclusive > sourceStartFrame)
    }

    fun startSeconds(): Double = startOffsetFrames.toDouble() / TIMELINE_SAMPLE_RATE

    fun playableSourceFrames(): Long = sourceMetadata?.let { metadata ->
        val start = sourceStartFrame.coerceAtMost(metadata.durationFrames)
        val end = (sourceEndFrameExclusive ?: metadata.durationFrames).coerceIn(start, metadata.durationFrames)
        end - start
    } ?: 0L

    fun durationSeconds(): Double = sourceMetadata?.let { playableSourceFrames().toDouble() / it.sampleRate } ?: 0.0

    fun splitAtTimelineFrame(
        timelineFrame: Long,
        newTrackId: String,
        newTrackName: String,
    ): Pair<Track, Track>? {
        val metadata = sourceMetadata ?: return null
        if (timelineFrame <= startOffsetFrames) return null
        val clipEndTimeline = startOffsetFrames +
            (playableSourceFrames().toDouble() * TIMELINE_SAMPLE_RATE / metadata.sampleRate).roundToLong()
        if (timelineFrame >= clipEndTimeline) return null
        val sourceDelta = ((timelineFrame - startOffsetFrames).toDouble() * metadata.sampleRate / TIMELINE_SAMPLE_RATE).roundToLong()
        val splitSourceFrame = (sourceStartFrame + sourceDelta).coerceAtMost(sourceEndFrameExclusive ?: metadata.durationFrames)
        if (splitSourceFrame <= sourceStartFrame || splitSourceFrame >= (sourceEndFrameExclusive ?: metadata.durationFrames)) return null
        return copy(sourceEndFrameExclusive = splitSourceFrame) to copy(
            id = newTrackId,
            name = newTrackName,
            sourceStartFrame = splitSourceFrame,
            startOffsetFrames = timelineFrame,
        )
    }

    companion object {
        fun create(id: String, name: String, type: TrackType = TrackType.MUSIC, sourceUri: String? = null) = Track(
            id = id,
            name = name,
            sourceUri = sourceUri,
            sourceMetadata = null,
            sourceStartFrame = 0,
            sourceEndFrameExclusive = null,
            startOffsetFrames = 0,
            gainDb = 0f,
            pan = 0f,
            muted = false,
            soloed = false,
            enabled = true,
            mainSendDb = if (type == TrackType.CLICK || type == TrackType.CUE) SILENCE_DB else 0f,
            monitorSendDb = if (type == TrackType.MUSIC) -6f else 0f,
            type = type,
        )
    }
}

data class MetronomeSettings(
    val enabled: Boolean = false,
    val bpm: Double = 120.0,
    val numerator: Int = 4,
    val denominator: Int = 4,
    val gainDb: Float = -12f,
    val mainEnabled: Boolean = false,
) {
    init {
        require(bpm in 20.0..400.0)
        require(numerator in 1..16)
        require(denominator in setOf(2, 4, 8, 16))
        require(gainDb in -60f..6f)
    }
}

/** One playable playlist item containing synchronized, independently mixed stems. */
data class MasterTrack(
    val id: String,
    val name: String,
    val tracks: List<Track> = emptyList(),
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    /** Null means this item inherits the project's metronome template. */
    val metronomeOverride: MetronomeSettings? = null,
) {
    init {
        require(name.isNotBlank())
        require(gainDb in -60f..6f)
        require(pan in -1f..1f)
        require(tracks.map(Track::id).distinct().size == tracks.size)
    }

    fun metronome(default: MetronomeSettings): MetronomeSettings = metronomeOverride ?: default

    fun durationSeconds(): Double = tracks.maxOfOrNull { it.startSeconds() + it.durationSeconds() } ?: 0.0
}

data class Project(
    val id: String,
    val name: String,
    val playlist: List<MasterTrack> = emptyList(),
    val masterGainDb: Float = 0f,
    val masterPan: Float = 0f,
    val defaultMetronome: MetronomeSettings = MetronomeSettings(),
) {
    init {
        require(name.isNotBlank())
        require(masterGainDb in -60f..6f)
        require(masterPan in -1f..1f)
        require(playlist.map(MasterTrack::id).distinct().size == playlist.size)
    }
}

sealed interface OutputMode {
    data object SingleMix : OutputMode
    data class StereoSplit(val inverted: Boolean = false) : OutputMode
    data class UsbMultichannel(val outputChannels: IntArray) : OutputMode
    data object AdvancedMultipleDevices : OutputMode
}

enum class SafetyStatus { SAFE, WARNING, UNSAFE }

object AudioMath {
    fun dbToLinear(db: Float): Float = when {
        db == SILENCE_DB || db <= -120f -> 0f
        else -> 10.0.pow(db / 20.0).toFloat()
    }

    fun panGains(pan: Float): Pair<Float, Float> {
        require(pan in -1f..1f)
        val angle = (pan + 1.0) * PI / 4.0
        return cos(angle).toFloat() to sin(angle).toFloat()
    }

    fun mainMono(left: Float, right: Float): Float = (left + right) * 0.5f
    fun monitorMono(left: Float, right: Float): Float = (left + right) / sqrt(2f)
}
