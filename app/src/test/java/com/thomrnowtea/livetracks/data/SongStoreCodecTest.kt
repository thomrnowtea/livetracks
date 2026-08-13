package com.thomrnowtea.livetracks.data

import com.thomrnowtea.livetracks.domain.MasterTrack
import com.thomrnowtea.livetracks.domain.MetronomeSettings
import com.thomrnowtea.livetracks.domain.Project
import com.thomrnowtea.livetracks.domain.SILENCE_DB
import com.thomrnowtea.livetracks.domain.SourceMetadata
import com.thomrnowtea.livetracks.domain.Track
import com.thomrnowtea.livetracks.domain.TrackType
import com.thomrnowtea.livetracks.domain.TimelineMarker
import com.thomrnowtea.livetracks.domain.TimelineMarkerKind
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongStoreCodecTest {
    private val codec = ProjectStoreCodec()

    @Test
    fun `versioned library round trips project playlist masters and stems`() {
        val track = Track.create("stem-1", "Click cuenta", TrackType.CLICK, "content://audio/click.wav")
            .copy(sourceMetadata = SourceMetadata(1, 48_000, 960_000), sourceStartFrame = 48_000,
                sourceEndFrameExclusive = 720_000, startOffsetFrames = 24_000)
        val original = listOf(
            Project(
                id = "project-1",
                name = "Show principal",
                playlist = listOf(
                    MasterTrack("master-1", "Intro", listOf(track), gainDb = -2f, pan = .1f,
                        metronomeOverride = MetronomeSettings(true, 123.5, 7, 8, -9f, false),
                        markers = listOf(TimelineMarker("marker-1", "Estribillo", 192_000, TimelineMarkerKind.CHORUS, true, 4)),
                        tempoGridVisible = false,
                        clickReferenceTrackId = track.id),
                ),
                masterGainDb = -1f,
                masterPan = -.2f,
                defaultMetronome = MetronomeSettings(bpm = 110.0),
            ),
        )

        val decoded = codec.decode(codec.encode(original))

        assertEquals(original, decoded)
        assertEquals(SILENCE_DB, decoded.single().playlist.single().tracks.single().mainSendDb)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown schema is rejected instead of silently misread`() {
        codec.decode("LIVETRACKS\t999\n")
    }

    @Test
    fun `schema two song migrates to project with one master track`() {
        fun field(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        val legacy = """
            LIVETRACKS	2
            SONG	${field("song-1")}	${field("Legacy Show")}	128.0	4	4	true	-12.0	false
            END
        """.trimIndent()

        val project = codec.decode(legacy).single()

        assertEquals("Legacy Show", project.name)
        assertEquals(1, project.playlist.size)
        assertEquals(128.0, project.playlist.single().metronomeOverride?.bpm)
    }

    @Test
    fun `schema three stem migrates with full source range`() {
        fun field(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        val legacy = listOf(
            "LIVETRACKS\t3",
            "PROJECT\t${field("p")}\t${field("Show")}\t0.0\t0.0\tfalse\t120.0\t4\t4\t-12.0\tfalse",
            "MASTER\t${field("m")}\t${field("Song")}\t0.0\t0.0\tfalse\tfalse\t120.0\t4\t4\t-12.0\tfalse",
            "TRACK\t${field("t")}\t${field("Stem.wav")}\t-\t2\t48000\t480000\t24000\t0.0\t0.0\tfalse\tfalse\ttrue\t0.0\t-6.0\tMUSIC",
            "ENDMASTER",
            "ENDPROJECT",
        ).joinToString("\n")

        val track = codec.decode(legacy).single().playlist.single().tracks.single()

        assertEquals(0L, track.sourceStartFrame)
        assertNull(track.sourceEndFrameExclusive)
        assertEquals(10.0, track.durationSeconds(), 0.0001)
    }

    @Test
    fun `schema four master migrates with an empty marker lane`() {
        fun field(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        val legacy = listOf(
            "LIVETRACKS\t4",
            "PROJECT\t${field("p")}\t${field("Show")}\t0.0\t0.0\tfalse\t120.0\t4\t4\t-12.0\tfalse",
            "MASTER\t${field("m")}\t${field("Song")}\t0.0\t0.0\tfalse\tfalse\t120.0\t4\t4\t-12.0\tfalse",
            "ENDMASTER",
            "ENDPROJECT",
        ).joinToString("\n")

        val master = codec.decode(legacy).single().playlist.single()

        assertEquals(emptyList<TimelineMarker>(), master.markers)
        assertEquals(true, master.tempoGridVisible)
        assertNull(master.clickReferenceTrackId)
    }

    @Test
    fun `schema five master migrates with visible tempo grid and native click`() {
        fun field(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        val legacy = listOf(
            "LIVETRACKS\t5",
            "PROJECT\t${field("p")}\t${field("Show")}\t0.0\t0.0\tfalse\t120.0\t4\t4\t-12.0\tfalse",
            "MASTER\t${field("m")}\t${field("Song")}\t0.0\t0.0\tfalse\tfalse\t120.0\t4\t4\t-12.0\tfalse",
            "ENDMASTER",
            "ENDPROJECT",
        ).joinToString("\n")

        val master = codec.decode(legacy).single().playlist.single()

        assertEquals(true, master.tempoGridVisible)
        assertNull(master.clickReferenceTrackId)
    }

    @Test
    fun `null override persists as inherited project default`() {
        val project = Project("p", "Show", listOf(MasterTrack("m", "Song")), defaultMetronome = MetronomeSettings(bpm = 96.0))
        val decoded = codec.decode(codec.encode(listOf(project))).single()

        assertNull(decoded.playlist.single().metronomeOverride)
        assertEquals(96.0, decoded.playlist.single().metronome(decoded.defaultMetronome).bpm, 0.0)
    }
}
