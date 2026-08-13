package com.thomrnowtea.livetracks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapTempoTest {
    @Test
    fun `tempo uses the interval between consecutive taps`() {
        assertEquals(120.0, tapTempoBpm(1_000L, 1_500L)!!, 0.0001)
        assertEquals(100.0, tapTempoBpm(1_500L, 2_100L)!!, 0.0001)
    }

    @Test
    fun `first and out of range taps do not update tempo`() {
        assertNull(tapTempoBpm(0L, 1_000L))
        assertNull(tapTempoBpm(1_000L, 1_100L))
        assertNull(tapTempoBpm(1_000L, 4_100L))
    }
}
