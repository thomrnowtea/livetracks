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

enum class TimelineMarkerKind { INTRO, VERSE, PRE_CHORUS, CHORUS, BRIDGE, SOLO, BREAKDOWN, OUTRO, CUSTOM }

data class TimelineMarker(
    val id: String,
    val label: String,
    /** Absolute marker location on the master-track timeline, expressed at 48 kHz. */
    val positionFrames: Long,
    val kind: TimelineMarkerKind = TimelineMarkerKind.CUSTOM,
    val voiceCueEnabled: Boolean = true,
    /** How many metronome beats before the marker the pre-rendered voice cue starts. */
    val voiceLeadBeats: Int = 2,
) {
    init {
        require(label.isNotBlank())
        require(positionFrames >= 0)
        require(voiceLeadBeats in 0..16)
    }
}

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
        require(numerator in 1..32)
        require(denominator in 1..32)
        require(gainDb in -60f..6f)
    }

    fun beatDurationSeconds(): Double = 60.0 / bpm * 4.0 / denominator

    fun beatDurationFrames(): Long = (beatDurationSeconds() * TIMELINE_SAMPLE_RATE).roundToLong().coerceAtLeast(1)

    fun barDurationSeconds(): Double = beatDurationSeconds() * numerator

    fun barDurationFrames(): Long = beatDurationFrames() * numerator
}

/** Returns the tempo represented by two consecutive taps, or null outside 20..400 BPM. */
fun tapTempoBpm(previousTapMillis: Long, currentTapMillis: Long): Double? {
    val intervalMillis = currentTapMillis - previousTapMillis
    if (previousTapMillis <= 0L || intervalMillis !in 150L..3_000L) return null
    return (60_000.0 / intervalMillis).coerceIn(20.0, 400.0)
}

/** One playable playlist item containing synchronized, independently mixed stems. */
data class MasterTrack(
    val id: String,
    val name: String,
    val tracks: List<Track> = emptyList(),
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    /** Null is accepted only for legacy persisted data and is migrated to a per-track copy on load. */
    val metronomeOverride: MetronomeSettings? = null,
    val markers: List<TimelineMarker> = emptyList(),
    /** Whether the musical beat/downbeat overlay is visible in this timeline. */
    val tempoGridVisible: Boolean = true,
    /** Optional user stem used as the audible click instead of the native synthesised click. */
    val clickReferenceTrackId: String? = null,
) {
    init {
        require(name.isNotBlank())
        require(gainDb in -60f..6f)
        require(pan in -1f..1f)
        require(tracks.map(Track::id).distinct().size == tracks.size)
        require(markers.map(TimelineMarker::id).distinct().size == markers.size)
        require(clickReferenceTrackId == null || tracks.any {
            it.id == clickReferenceTrackId && it.type == TrackType.CLICK && it.mainSendDb == SILENCE_DB
        })
    }

    fun metronome(default: MetronomeSettings): MetronomeSettings = metronomeOverride ?: default

    fun clickReferenceTrack(): Track? = clickReferenceTrackId?.let { id -> tracks.firstOrNull { it.id == id } }

    fun durationSeconds(): Double = maxOf(
        tracks.maxOfOrNull { it.startSeconds() + it.durationSeconds() } ?: 0.0,
        markers.maxOfOrNull { it.positionFrames.toDouble() / TIMELINE_SAMPLE_RATE } ?: 0.0,
    )
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

    fun extractTrackAsMaster(
        sourceMasterId: String,
        trackId: String,
        newMasterId: String,
        newMasterName: String,
    ): Pair<Project, MasterTrack>? {
        val sourceIndex = playlist.indexOfFirst { it.id == sourceMasterId }
        val source = playlist.getOrNull(sourceIndex) ?: return null
        val extracted = source.tracks.firstOrNull { it.id == trackId } ?: return null
        val newMaster = MasterTrack(
            id = newMasterId,
            name = newMasterName.trim().ifBlank { extracted.name },
            tracks = listOf(extracted.copy(startOffsetFrames = 0)),
            metronomeOverride = source.metronome(defaultMetronome),
            tempoGridVisible = source.tempoGridVisible,
            clickReferenceTrackId = extracted.id.takeIf { source.clickReferenceTrackId == trackId },
        )
        val updated = playlist.toMutableList().apply {
            this[sourceIndex] = source.copy(
                tracks = source.tracks.filterNot { it.id == trackId },
                clickReferenceTrackId = source.clickReferenceTrackId.takeUnless { it == trackId },
            )
            add(sourceIndex + 1, newMaster)
        }
        return copy(playlist = updated) to newMaster
    }
}

fun TimelineMarker.voiceCueStartFrames(metronome: MetronomeSettings): Long =
    (positionFrames - voiceLeadBeats * metronome.beatDurationFrames()).coerceAtLeast(0)

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
