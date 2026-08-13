package com.thomrnowtea.livetracks.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.thomrnowtea.livetracks.domain.SourceMetadata
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

data class CachedWaveform(
    val sourceUri: String,
    val sourceSize: Long,
    val sourceModifiedAt: Long,
    val metadata: SourceMetadata,
    val peaks: List<Float>,
)

/** Versioned, non-realtime waveform storage. Corrupt or stale entries are ignored. */
class WaveformCache(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.filesDir, "waveforms-v$WAVEFORM_CACHE_SCHEMA").apply { mkdirs() }

    fun read(sourceUri: String): CachedWaveform? = runCatching {
        val file = cacheFile(sourceUri)
        if (!file.isFile) return null
        val entry = file.inputStream().buffered().use(WaveformCacheCodec::read)
        if (entry.sourceUri != sourceUri) return null
        val (size, modifiedAt) = fingerprint(sourceUri)
        if (size >= 0 && entry.sourceSize >= 0 && size != entry.sourceSize) return null
        if (modifiedAt >= 0 && entry.sourceModifiedAt >= 0 && modifiedAt != entry.sourceModifiedAt) return null
        entry
    }.getOrNull()

    fun write(sourceUri: String, metadata: SourceMetadata, peaks: List<Float>) {
        if (peaks.isEmpty() || peaks.size > MAX_WAVEFORM_PEAKS) return
        runCatching {
            directory.mkdirs()
            val target = cacheFile(sourceUri)
            val temporary = File(directory, "${target.name}.tmp")
            val (size, modifiedAt) = fingerprint(sourceUri)
            temporary.outputStream().buffered().use { output ->
                WaveformCacheCodec.write(CachedWaveform(sourceUri, size, modifiedAt, metadata, peaks), output)
            }
            if (target.exists() && !target.delete()) error("Cannot replace waveform cache")
            if (!temporary.renameTo(target)) {
                temporary.delete()
                error("Cannot commit waveform cache")
            }
        }
    }

    private fun fingerprint(sourceUri: String): Pair<Long, Long> = runCatching {
        resolver.query(
            Uri.parse(sourceUri),
            arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use -1L to -1L
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
            val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else -1L
            size to modified
        } ?: (-1L to -1L)
    }.getOrDefault(-1L to -1L)

    private fun cacheFile(sourceUri: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(sourceUri.toByteArray(Charsets.UTF_8))
        return File(directory, digest.joinToString("") { "%02x".format(it) } + ".wave")
    }
}

internal object WaveformCacheCodec {
    fun write(entry: CachedWaveform, output: OutputStream) {
        DataOutputStream(BufferedOutputStream(output)).use { data ->
            data.writeInt(WAVEFORM_CACHE_MAGIC)
            data.writeInt(WAVEFORM_CACHE_SCHEMA)
            data.writeUTF(entry.sourceUri)
            data.writeLong(entry.sourceSize)
            data.writeLong(entry.sourceModifiedAt)
            data.writeInt(entry.metadata.channelCount)
            data.writeInt(entry.metadata.sampleRate)
            data.writeLong(entry.metadata.durationFrames)
            data.writeInt(entry.peaks.size)
            entry.peaks.forEach { data.writeFloat(it.coerceIn(0f, 1f)) }
        }
    }

    fun read(input: InputStream): CachedWaveform {
        DataInputStream(BufferedInputStream(input)).use { data ->
            require(data.readInt() == WAVEFORM_CACHE_MAGIC)
            require(data.readInt() == WAVEFORM_CACHE_SCHEMA)
            val uri = data.readUTF()
            val size = data.readLong()
            val modifiedAt = data.readLong()
            val metadata = SourceMetadata(data.readInt(), data.readInt(), data.readLong())
            val peakCount = data.readInt()
            require(peakCount in 1..MAX_WAVEFORM_PEAKS)
            val peaks = List(peakCount) { data.readFloat().also { require(it.isFinite() && it in 0f..1f) } }
            require(data.read() == -1)
            return CachedWaveform(uri, size, modifiedAt, metadata, peaks)
        }
    }
}

private const val WAVEFORM_CACHE_MAGIC = 0x4C545746 // LTWF
private const val WAVEFORM_CACHE_SCHEMA = 1
private const val MAX_WAVEFORM_PEAKS = 8_192
