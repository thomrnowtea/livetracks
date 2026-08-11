package com.thomrnowtea.livetracks.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSafetyTest {
    @Test
    fun `click never routes to main by default`() {
        val click = Track.create("click", "Click", TrackType.CLICK)
        assertEquals(SILENCE_DB, click.mainSendDb)
        assertEquals(0f, click.monitorSendDb)
    }

    @Test
    fun `cue never routes to main by default`() {
        val cue = Track.create("cue", "Verse", TrackType.CUE)
        assertEquals(SILENCE_DB, cue.mainSendDb)
    }
}

