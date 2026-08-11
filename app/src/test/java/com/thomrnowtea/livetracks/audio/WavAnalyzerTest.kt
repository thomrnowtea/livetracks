package com.thomrnowtea.livetracks.audio

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavAnalyzerTest {
    @Test
    fun `pcm waveform analysis returns source metadata and real peaks`() {
        val frames = shortArrayOf(0, 0, 16_384, -16_384, 32_767, -32_768, 8_192, -8_192)
        val dataBytes = frames.size * Short.SIZE_BYTES
        val wav = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1.toShort()); putShort(2.toShort())
            putInt(1_000); putInt(1_000 * 2 * Short.SIZE_BYTES); putShort((2 * Short.SIZE_BYTES).toShort()); putShort(16.toShort())
            put("data".toByteArray()); putInt(dataBytes); frames.forEach { putShort(it) }
        }.array()
        val file = Files.createTempFile("livetracks-waveform", ".wav").toFile().apply {
            writeBytes(wav)
            deleteOnExit()
        }

        val result = FileInputStream(file).use { WavAnalyzer.analyze(it, maximumBuckets = 4) }

        assertEquals(2, result.metadata.channelCount)
        assertEquals(1_000, result.metadata.sampleRate)
        assertEquals(4L, result.metadata.durationFrames)
        assertEquals(4, result.peaks.size)
        assertTrue(result.peaks[2] > .99f)
        assertTrue(result.peaks[1] in .49f..0.51f)
    }
}
