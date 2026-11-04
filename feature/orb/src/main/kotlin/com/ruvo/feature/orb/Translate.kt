package com.ruvo.feature.orb

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Ruvo's offline answers are written in English. For the other chat languages they are translated on the phone
 * with ML Kit: each language is downloaded once (about 30 MB), then works without internet. The text being
 * translated never leaves the phone. Any failure returns null so the chat falls back to English.
 */
object OnDeviceTranslate {
    private val codes = mapOf(
        "hi-IN" to TranslateLanguage.HINDI, "mr-IN" to TranslateLanguage.MARATHI, "ta-IN" to TranslateLanguage.TAMIL,
        "te-IN" to TranslateLanguage.TELUGU, "bn-IN" to TranslateLanguage.BENGALI, "gu-IN" to TranslateLanguage.GUJARATI,
    )
    private val translators = mutableMapOf<String, Translator>()

    fun supports(tag: String) = tag in codes

    private fun translator(from: String, to: String): Translator =
        translators.getOrPut("$from>$to") {
            Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build())
        }

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
        addOnCanceledListener { cont.resume(null) }
    }

    /** Waits for a task and says only whether it worked (a download succeeds with no value, so null is not failure). */
    private suspend fun Task<*>.succeeded(): Boolean = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(true) }
        addOnFailureListener { cont.resume(false) }
        addOnCanceledListener { cont.resume(false) }
    }

    /** Makes sure the language is on the phone, downloading it once if needed. True when ready. */
    suspend fun prepare(tag: String): Boolean {
        val code = codes[tag] ?: return false
        val conditions = DownloadConditions.Builder().build()
        return translator(TranslateLanguage.ENGLISH, code).downloadModelIfNeeded(conditions).succeeded() &&
            translator(code, TranslateLanguage.ENGLISH).downloadModelIfNeeded(conditions).succeeded()
    }

    /** English text into [tag]'s language, or null if that isn't possible right now. */
    suspend fun fromEnglish(text: String, tag: String): String? {
        val code = codes[tag] ?: return null
        if (!prepare(tag)) return null
        return translator(TranslateLanguage.ENGLISH, code).translate(text).awaitOrNull()
    }

    /** A question typed or spoken in [tag]'s language, into English so Ruvo can understand it. */
    suspend fun toEnglish(text: String, tag: String): String? {
        val code = codes[tag] ?: return null
        if (!prepare(tag)) return null
        return translator(code, TranslateLanguage.ENGLISH).translate(text).awaitOrNull()
    }
}
