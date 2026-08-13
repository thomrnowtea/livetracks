package com.thomrnowtea.livetracks.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineSnapTest {
    @Test
    fun `snap follows the visible 250 millisecond grid`() {
        assertEquals(
            12_000L,
            snapTimelineFrames(
                proposedFrames = 13_100L,
                gridStepFrames = 12_000L,
            ),
        )
    }

    @Test
    fun `snap follows the visible 10 millisecond grid`() {
        assertEquals(
            12_480L,
            snapTimelineFrames(
                proposedFrames = 12_300L,
                gridStepFrames = 480L,
            ),
        )
    }

    @Test
    fun `nearby magnetic target still lands on the visible time grid`() {
        assertEquals(
            12_000L,
            snapTimelineFrames(
                proposedFrames = 12_700L,
                gridStepFrames = 12_000L,
                magneticTargets = listOf(12_750L, 48_000L),
                magneticToleranceFrames = 100L,
            ),
        )
    }

    @Test
    fun `distant magnetic target does not override the time grid`() {
        assertEquals(
            12_000L,
            snapTimelineFrames(
                proposedFrames = 13_100L,
                gridStepFrames = 12_000L,
                magneticTargets = listOf(18_000L),
                magneticToleranceFrames = 500L,
            ),
        )
    }

    @Test
    fun `disabled snap preserves free movement`() {
        assertEquals(
            13_137L,
            snapTimelineFrames(
                proposedFrames = 13_137L,
                gridStepFrames = 12_000L,
                magneticTargets = listOf(13_150L),
                magneticToleranceFrames = 100L,
                enabled = false,
            ),
        )
    }

    @Test
    fun `enabled snap never leaves a stem between grid units`() {
        val result = snapTimelineFrames(
            proposedFrames = 18_050L,
            gridStepFrames = 4_800L,
            magneticTargets = listOf(18_025L),
            magneticToleranceFrames = 100L,
        )

        assertEquals(0L, result % 4_800L)
    }
}
