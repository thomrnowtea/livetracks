package com.thomrnowtea.livetracks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SongDurationTest {
    @Test
    fun `master track duration includes latest stem entry offset`() {
        val short = Track.create("short", "Short", sourceUri = "content://short")
            .copy(sourceMetadata = SourceMetadata(2, 48_000, 480_000), startOffsetFrames = 48_000)
        val long = Track.create("long", "Long", sourceUri = "content://long")
            .copy(sourceMetadata = SourceMetadata(2, 44_100, 1_984_500))

        assertEquals(45.0, MasterTrack("master", "Song", listOf(short, long)).durationSeconds(), 0.0001)
    }

    @Test
    fun `master track inherits safe metronome template until overridden`() {
        val project = Project("project", "Set", listOf(MasterTrack("master", "Song")))
        val settings = project.playlist.single().metronome(project.defaultMetronome)

        assertFalse(settings.enabled)
        assertFalse(settings.mainEnabled)
        assertEquals(-12f, settings.gainDb)
    }

    @Test
    fun `empty stem metadata reserves timeline duration without a source file`() {
        val empty = Track.create("empty", "Interludio")
            .copy(sourceMetadata = SourceMetadata(1, TIMELINE_SAMPLE_RATE, 1_440_000))

        assertNull(empty.sourceUri)
        assertEquals(30.0, empty.durationSeconds(), 0.0001)
        assertEquals(30.0, MasterTrack("master", "Song", listOf(empty)).durationSeconds(), 0.0001)
    }

    @Test
    fun `split creates two contiguous source ranges at the timeline cursor`() {
        val track = Track.create("stem", "Guitar.wav", sourceUri = "content://guitar")
            .copy(sourceMetadata = SourceMetadata(2, 48_000, 480_000), startOffsetFrames = 48_000)

        val (left, right) = requireNotNull(track.splitAtTimelineFrame(144_000, "stem-b", "Guitar · B.wav"))

        assertEquals(96_000L, left.sourceEndFrameExclusive)
        assertEquals(96_000L, right.sourceStartFrame)
        assertEquals(144_000L, right.startOffsetFrames)
        assertEquals(2.0, left.durationSeconds(), 0.0001)
        assertEquals(8.0, right.durationSeconds(), 0.0001)
    }

    @Test
    fun `extracted clip becomes the next independent master with its own metronome`() {
        val original = Track.create("stem-a", "Guitar B.wav", TrackType.CLICK, sourceUri = "content://guitar")
            .copy(sourceMetadata = SourceMetadata(2, 48_000, 240_000), sourceStartFrame = 48_000, startOffsetFrames = 144_000)
        val companion = Track.create("stem-b", "Drums.wav")
        val sourceMetronome = MetronomeSettings(enabled = true, bpm = 132.0, numerator = 3)
        val project = Project(
            "project",
            "Show",
            playlist = listOf(MasterTrack(
                "source",
                "Song",
                listOf(companion, original),
                metronomeOverride = sourceMetronome,
                tempoGridVisible = false,
                clickReferenceTrackId = original.id,
            )),
        )

        val (updated, extracted) = requireNotNull(project.extractTrackAsMaster("source", "stem-a", "extract", "Guitar pickup"))

        assertEquals(listOf("source", "extract"), updated.playlist.map(MasterTrack::id))
        assertEquals(listOf("stem-b"), updated.playlist.first().tracks.map(Track::id))
        assertEquals("stem-a", extracted.tracks.single().id)
        assertEquals(0L, extracted.tracks.single().startOffsetFrames)
        assertEquals(sourceMetronome, extracted.metronomeOverride)
        assertEquals(false, extracted.tempoGridVisible)
        assertEquals("stem-a", extracted.clickReferenceTrackId)
        assertNull(updated.playlist.first().clickReferenceTrackId)
    }

    @Test
    fun `marker voice lead uses the master beat duration`() {
        val metronome = MetronomeSettings(bpm = 120.0, denominator = 4)
        val marker = TimelineMarker("chorus", "Chorus", positionFrames = 192_000, voiceLeadBeats = 2)

        assertEquals(24_000L, metronome.beatDurationFrames())
        assertEquals(144_000L, marker.voiceCueStartFrames(metronome))
    }

    @Test
    fun `musical grid spacing follows tempo signature and denominator changes`() {
        val commonTime = MetronomeSettings(enabled = true, bpm = 128.0, numerator = 4, denominator = 4)
        val compoundTime = MetronomeSettings(enabled = true, bpm = 120.0, numerator = 6, denominator = 8)

        assertEquals(22_500L, commonTime.beatDurationFrames())
        assertEquals(90_000L, commonTime.barDurationFrames())
        assertEquals(12_000L, compoundTime.beatDurationFrames())
        assertEquals(72_000L, compoundTime.barDurationFrames())
    }
}
