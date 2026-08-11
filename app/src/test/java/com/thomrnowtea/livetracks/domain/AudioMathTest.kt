package com.thomrnowtea.livetracks.domain

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMathTest {
    @Test
    fun `decibels convert to expected amplitude`() {
        assertEquals(1f, AudioMath.dbToLinear(0f), 0.00001f)
        assertEquals(0.501187f, AudioMath.dbToLinear(-6f), 0.00001f)
        assertEquals(0f, AudioMath.dbToLinear(SILENCE_DB), 0f)
    }

    @Test
    fun `equal power pan has correct endpoints and center`() {
        assertEquals(1f, AudioMath.panGains(-1f).first, 0.00001f)
        assertEquals(0f, AudioMath.panGains(-1f).second, 0.00001f)
        assertEquals(0f, AudioMath.panGains(1f).first, 0.00001f)
        assertEquals(1f, AudioMath.panGains(1f).second, 0.00001f)
        val center = AudioMath.panGains(0f)
        assertEquals(1f / sqrt(2f), center.first, 0.00001f)
        assertEquals(center.first, center.second, 0.00001f)
    }

    @Test
    fun `main mono downmix preserves headroom for correlated full scale input`() {
        val mixed = AudioMath.mainMono(1f, 1f)
        assertEquals(1f, mixed, 0f)
        assertTrue(mixed <= 1f)
    }
}

