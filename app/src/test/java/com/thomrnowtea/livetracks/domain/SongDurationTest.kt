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
}
