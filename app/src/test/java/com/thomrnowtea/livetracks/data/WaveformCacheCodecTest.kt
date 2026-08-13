package com.thomrnowtea.livetracks.data

import com.thomrnowtea.livetracks.domain.SourceMetadata
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveformCacheCodecTest {
    @Test
    fun `round trip preserves fingerprint metadata and peaks`() {
        val expected = CachedWaveform(
            sourceUri = "content://audio/demo.wav",
            sourceSize = 123_456,
            sourceModifiedAt = 987_654,
            metadata = SourceMetadata(channelCount = 2, sampleRate = 48_000, durationFrames = 96_000),
            peaks = listOf(0f, .25f, .75f, 1f),
        )
        val bytes = ByteArrayOutputStream().also { WaveformCacheCodec.write(expected, it) }.toByteArray()
        assertEquals(expected, WaveformCacheCodec.read(ByteArrayInputStream(bytes)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `corrupt cache is rejected`() {
        WaveformCacheCodec.read(ByteArrayInputStream(byteArrayOf(0, 1, 2, 3)))
    }
}
