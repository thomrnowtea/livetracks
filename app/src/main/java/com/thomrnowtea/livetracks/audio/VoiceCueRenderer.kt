package com.thomrnowtea.livetracks.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.thomrnowtea.livetracks.data.AppLanguage
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class VoiceCueException(message: String) : Exception(message)

/** Renders guide speech ahead of playback; no TTS work ever reaches the realtime audio callback. */
class VoiceCueRenderer(context: Context) {
    private val appContext = context.applicationContext
    private val outputDirectory = File(appContext.filesDir, "voice_cues").apply { mkdirs() }

    fun cueFile(markerId: String): File = File(outputDirectory, "${safeId(markerId)}.wav")

    fun remove(markerId: String) {
        cueFile(markerId).delete()
    }

    suspend fun render(markerId: String, text: String, language: AppLanguage): File = withContext(Dispatchers.IO) {
        val destination = cueFile(markerId)
        val temporary = File(outputDirectory, ".${safeId(markerId)}-${UUID.randomUUID()}.tmp")
        val initialized = CompletableDeferred<Int>()
        val tts = withContext(Dispatchers.Main) {
            TextToSpeech(appContext) { status -> initialized.complete(status) }
        }
        try {
            if (withTimeout(INIT_TIMEOUT_MS) { initialized.await() } != TextToSpeech.SUCCESS) {
                throw VoiceCueException("El motor de voz de Android no pudo iniciarse")
            }
            val locale = if (language == AppLanguage.SPANISH) Locale("es") else Locale.ENGLISH
            val offlineVoice = tts.voices
                ?.filter { !it.isNetworkConnectionRequired && it.locale.language == locale.language }
                ?.maxByOrNull { it.quality }
            val languageResult = if (offlineVoice != null) tts.setVoice(offlineVoice) else tts.setLanguage(locale)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw VoiceCueException("No hay una voz offline instalada para este idioma")
            }
            tts.setSpeechRate(DEFAULT_SPEECH_RATE)
            val utteranceId = "cue-${UUID.randomUUID()}"
            val completed = CompletableDeferred<Unit>()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) { if (id == utteranceId) completed.complete(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { if (id == utteranceId) completed.completeExceptionally(VoiceCueException("No se pudo generar el cue de voz")) }
                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId) completed.completeExceptionally(VoiceCueException("No se pudo generar el cue de voz ($errorCode)"))
                }
            })
            val queued = tts.synthesizeToFile(text.trim(), Bundle(), temporary, utteranceId)
            if (queued != TextToSpeech.SUCCESS) throw VoiceCueException("El cue de voz no pudo ponerse en cola")
            withTimeout(SYNTHESIS_TIMEOUT_MS) { completed.await() }
            if (!temporary.isFile || temporary.length() < 44) throw VoiceCueException("El motor de voz no generó audio válido")
            FileInputStream(temporary).use { WavAnalyzer.analyze(it, maximumBuckets = 32) }
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            destination
        } finally {
            tts.shutdown()
            temporary.delete()
        }
    }

    private fun safeId(value: String): String = value.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        .take(80).ifBlank { UUID.randomUUID().toString() }

    companion object {
        private const val INIT_TIMEOUT_MS = 10_000L
        private const val SYNTHESIS_TIMEOUT_MS = 30_000L
        private const val DEFAULT_SPEECH_RATE = 1.08f
    }
}
