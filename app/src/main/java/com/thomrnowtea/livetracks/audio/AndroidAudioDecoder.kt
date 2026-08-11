package com.thomrnowtea.livetracks.audio

import android.content.ContentResolver
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/** Converts Android-supported compressed audio into the PCM WAV boundary used by the native engine. */
class AndroidAudioDecoder(
    private val resolver: ContentResolver,
    private val cacheDirectory: File,
) {
    private val decodedFiles = ConcurrentHashMap<String, File>()
    private val decodeLock = Any()

    fun decodedWav(uri: Uri): File {
        decodedFiles[uri.toString()]?.takeIf(File::exists)?.let { return it }
        return synchronized(decodeLock) {
            decodedFiles[uri.toString()]?.takeIf(File::exists) ?: decode(uri).also {
                decodedFiles[uri.toString()] = it
            }
        }
    }

    private fun decode(uri: Uri): File {
        cacheDirectory.mkdirs()
        val raw = File(cacheDirectory, "${UUID.randomUUID()}.pcm")
        val wav = File(cacheDirectory, "${UUID.randomUUID()}.wav")
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                if (descriptor.declaredLength >= 0) {
                    extractor.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.declaredLength)
                } else {
                    extractor.setDataSource(descriptor.fileDescriptor)
                }
            } ?: error("No se pudo abrir el archivo")
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("El archivo no contiene una pista de audio")
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("Formato de audio desconocido")
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            var sampleRate = inputFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, 0)
            var channels = inputFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, 0)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var inputEnded = false
            var outputEnded = false
            val info = MediaCodec.BufferInfo()
            BufferedOutputStream(FileOutputStream(raw)).use { output ->
                while (!outputEnded) {
                    if (!inputEnded) {
                        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val buffer = codec.getInputBuffer(inputIndex) ?: error("Decoder sin buffer de entrada")
                            buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime.coerceAtLeast(0), 0)
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = codec.outputFormat
                            sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                            channels = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                            pcmEncoding = format.intOr(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                            require(channels in 1..2) { "Sólo se admiten archivos mono o estéreo" }
                            require(sampleRate > 0) { "Sample rate inválido" }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (outputIndex >= 0) {
                            if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                val buffer = codec.getOutputBuffer(outputIndex) ?: error("Decoder sin buffer de salida")
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                writePcm16(buffer.slice().order(ByteOrder.nativeOrder()), pcmEncoding, output)
                                require(raw.length() <= MAX_PCM_BYTES) { "El audio decodificado supera el límite actual de 512 MB" }
                            }
                            outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }
            require(raw.length() <= MAX_PCM_BYTES) { "El audio decodificado supera el límite actual de 512 MB" }
            require(sampleRate > 0 && channels in 1..2 && raw.length() > 0) { "El decoder no produjo audio PCM válido" }
            writeWav(raw, wav, sampleRate, channels)
            return wav
        } catch (error: Throwable) {
            wav.delete()
            throw error
        } finally {
            raw.delete()
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun writePcm16(input: ByteBuffer, encoding: Int, output: BufferedOutputStream) {
        when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val bytes = ByteArray(input.remaining())
                input.get(bytes)
                output.write(bytes)
            }
            AudioFormat.ENCODING_PCM_FLOAT -> while (input.remaining() >= Float.SIZE_BYTES) {
                val value = (input.float.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort().toInt()
                output.write(value and 0xFF)
                output.write(value ushr 8 and 0xFF)
            }
            AudioFormat.ENCODING_PCM_8BIT -> while (input.hasRemaining()) {
                val value = ((input.get().toInt() and 0xFF) - 128) shl 8
                output.write(value and 0xFF)
                output.write(value ushr 8 and 0xFF)
            }
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> while (input.remaining() >= 3) {
                input.get()
                output.write(input.get().toInt() and 0xFF)
                output.write(input.get().toInt() and 0xFF)
            }
            AudioFormat.ENCODING_PCM_32BIT -> while (input.remaining() >= Int.SIZE_BYTES) {
                val value = input.int shr 16
                output.write(value and 0xFF)
                output.write(value ushr 8 and 0xFF)
            }
            else -> error("Formato PCM del decoder no compatible: $encoding")
        }
    }

    private fun writeWav(raw: File, target: File, sampleRate: Int, channels: Int) {
        val dataSize = raw.length().toInt()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(sampleRate * channels * Short.SIZE_BYTES)
            putShort((channels * Short.SIZE_BYTES).toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()
        FileOutputStream(target).use { output ->
            output.write(header)
            raw.inputStream().use { it.copyTo(output) }
        }
    }

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private companion object {
        const val CODEC_TIMEOUT_US = 10_000L
        const val MAX_PCM_BYTES = 512L * 1024L * 1024L - 44L
    }
}
