package com.thomrnowtea.livetracks.audio

import com.thomrnowtea.livetracks.domain.SourceMetadata
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class WavAnalysis(
    val metadata: SourceMetadata,
    val peaks: List<Float>,
)

/** Non-realtime RIFF/WAV metadata and peak-envelope reader used by the Timeline. */
object WavAnalyzer {
    private const val MAX_FILE_BYTES = 512L * 1024L * 1024L
    private const val DEFAULT_BUCKETS = 8_192
    private const val FRAMES_PER_READ = 8_192

    fun analyze(input: FileInputStream, maximumBuckets: Int = DEFAULT_BUCKETS): WavAnalysis {
        require(maximumBuckets > 0)
        val channel = input.channel
        require(channel.size() in 44..MAX_FILE_BYTES) { "Unsupported WAV size" }
        val riff = readAt(channel, 0, 12)
        require(riff.ascii(4) == "RIFF" && riff.apply { position(8) }.ascii(4) == "WAVE") { "Not a RIFF/WAVE file" }

        var format = 0
        var channelCount = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataOffset = -1L
        var dataSize = 0L
        var offset = 12L
        while (offset + 8 <= channel.size()) {
            val header = readAt(channel, offset, 8)
            val tag = header.ascii(4)
            val chunkSize = header.int.toLong() and 0xFFFF_FFFFL
            val contentOffset = offset + 8
            require(contentOffset + chunkSize <= channel.size()) { "Corrupt WAV chunk" }
            when (tag) {
                "fmt " -> if (chunkSize >= 16) {
                    val value = readAt(channel, contentOffset, 16)
                    format = value.short.toInt() and 0xFFFF
                    channelCount = value.short.toInt() and 0xFFFF
                    sampleRate = value.int
                    value.position(14)
                    bitsPerSample = value.short.toInt() and 0xFFFF
                }
                "data" -> {
                    dataOffset = contentOffset
                    dataSize = chunkSize
                }
            }
            offset = contentOffset + chunkSize + (chunkSize and 1L)
        }

        require(dataOffset >= 0 && channelCount in 1..2 && sampleRate > 0) { "Incomplete WAV header" }
        val pcm = format == 1 && bitsPerSample in setOf(8, 16, 24, 32)
        val float = format == 3 && bitsPerSample == 32
        require(pcm || float) { "Unsupported WAV encoding" }
        val bytesPerSample = bitsPerSample / 8
        val bytesPerFrame = bytesPerSample * channelCount
        val frameCount = dataSize / bytesPerFrame
        require(frameCount > 0) { "Empty WAV data" }

        val bucketCount = min(maximumBuckets.toLong(), frameCount).toInt()
        val peaks = FloatArray(bucketCount)
        val buffer = ByteBuffer.allocateDirect(FRAMES_PER_READ * bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(dataOffset)
        var frame = 0L
        while (frame < frameCount) {
            val framesThisRead = min(FRAMES_PER_READ.toLong(), frameCount - frame).toInt()
            val bytesThisRead = framesThisRead * bytesPerFrame
            buffer.clear()
            buffer.limit(bytesThisRead)
            while (buffer.hasRemaining()) require(channel.read(buffer) > 0) { "Truncated WAV data" }
            buffer.flip()
            repeat(framesThisRead) {
                var peak = 0f
                repeat(channelCount) {
                    peak = max(peak, abs(readSample(buffer, format, bitsPerSample)))
                }
                val bucket = min(bucketCount - 1, ((frame * bucketCount) / frameCount).toInt())
                peaks[bucket] = max(peaks[bucket], peak.coerceIn(0f, 1f))
                frame++
            }
        }
        return WavAnalysis(SourceMetadata(channelCount, sampleRate, frameCount), peaks.toList())
    }

    private fun readAt(channel: FileChannel, offset: Long, size: Int): ByteBuffer {
        val value = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        channel.position(offset)
        while (value.hasRemaining()) require(channel.read(value) > 0) { "Truncated WAV header" }
        value.flip()
        return value
    }

    private fun ByteBuffer.ascii(length: Int): String {
        val bytes = ByteArray(length)
        get(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    private fun readSample(buffer: ByteBuffer, format: Int, bits: Int): Float = when {
        format == 3 -> buffer.float.coerceIn(-1f, 1f)
        bits == 8 -> ((buffer.get().toInt() and 0xFF) - 128) / 128f
        bits == 16 -> buffer.short / 32_768f
        bits == 24 -> {
            val low = buffer.get().toInt() and 0xFF
            val middle = buffer.get().toInt() and 0xFF
            val high = buffer.get().toInt() and 0xFF
            var value = low or (middle shl 8) or (high shl 16)
            if (value and 0x80_0000 != 0) value = value or -0x100_0000
            value / 8_388_608f
        }
        else -> buffer.int / 2_147_483_648f
    }
}
