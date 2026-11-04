package com.ruvo.feature.orb

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.guide.RuvoGuide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Optional live answers through the Claude API, used only when Kumkum adds her own key to local.properties
 * (RUVO_CLAUDE_API_KEY). It sends a short summary of the numbers, never raw transactions or SMS text.
 * The engine's numbers are passed in; the model is told to explain them, not to calculate new ones.
 * Any failure falls back to the scripted answer, so the demo never breaks.
 */
object LiveRuvo {
    private const val MODEL = "claude-haiku-5-5"

    val enabled: Boolean get() = BuildConfig.RUVO_CLAUDE_API_KEY.isNotBlank()

    suspend fun answer(question: String, ctx: RuvoGuide.Context, history: List<Pair<Boolean, String>>): String? {
        if (!enabled) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 15_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                    setRequestProperty("x-api-key", BuildConfig.RUVO_CLAUDE_API_KEY)
                    setRequestProperty("anthropic-version", "2023-06-01")
                }
                val messages = JSONArray()
                history.takeLast(6).forEach { (fromUser, text) ->
                    messages.put(JSONObject().put("role", if (fromUser) "user" else "assistant").put("content", text))
                }
                messages.put(JSONObject().put("role", "user").put("content", question))
                val body = JSONObject()
                    .put("model", MODEL)
                    .put("max_tokens", 300)
                    .put("system", systemPrompt(ctx))
                    .put("messages", messages)
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                if (conn.responseCode !in 200..299) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val content = json.getJSONArray("content")
                (0 until content.length()).joinToString("") { i ->
                    content.getJSONObject(i).optString("text")
                }.trim().ifEmpty { null }
            }.getOrNull()
        }
    }

    /** A summary only: totals and the engine's results. No merchants, no messages, no account numbers. */
    private fun systemPrompt(c: RuvoGuide.Context): String {
        val r = c.result
        val week = c.input.transactions.filter { it.countsAsSpending && java.time.temporal.ChronoUnit.DAYS.between(it.date, r.today) in 0..6 }
            .groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.yourShare } }
        val numbers = buildString {
            appendLine("Runway: ${if (r.unlimited) "no end" else IndianFormat.days(r.runwayDays)}")
            appendLine("Days to next money: ${r.daysToNextIncome ?: "irregular income"}")
            appendLine("Buffer: ${r.bufferDays ?: "n/a"} days")
            appendLine("Balance ${IndianFormat.rupees(r.balance)}, Fixed ${IndianFormat.rupees(r.fixedReserved)}, Future ${IndianFormat.rupees(r.futureProtected)}, Fuel ${IndianFormat.rupees(r.fuel)}")
            appendLine("Usual daily spend ${IndianFormat.rupees(r.dailySpend)}, safe daily to next money ${IndianFormat.rupees(r.safeDaily)}")
            appendLine("This week by category: " + week.entries.joinToString { "${it.key} ${IndianFormat.rupees(it.value)}" })
            appendLine("Goals: " + c.input.goals.joinToString { "${it.name} ${(it.progress * 100).toInt()}% of ${IndianFormat.rupees(it.target)}" })
            appendLine("Owed to the student: ${IndianFormat.rupees(c.owedToYou)}")
        }
        return """
            You are Ruvo, a money companion for Indian students aged 18 to 25. Your voice is a mature, warm and
            insightful Indian woman speaking English: calm, plain words, never preachy, never shaming, light humour
            only when things are going well. Keep answers under 80 words.
            Rules: use only the numbers below and never compute new totals; if a number is not here, say you do not
            have it. Never recommend specific investments, funds, stocks, loans or cards, and give no tax or legal
            advice. Never tell the student they cannot buy something; show the consequence in days and leave the
            choice with them. Use Indian number grouping (₹1,25,000).
            The student's numbers right now:
            $numbers
        """.trimIndent()
    }
}

/**
 * Spoken replies with the phone's own text-to-speech, Indian English by default. Asking to speak before the
 * engine has started queues the request instead of dropping it. [speakingId] says which answer is being read
 * so its button can offer Stop, and [status] says when the phone has no usable voice, so the screen can say so.
 */
class RuvoSpeech(context: Context) {
    enum class Status { STARTING, READY, UNAVAILABLE }

    var status by mutableStateOf(Status.STARTING)
        private set
    var speakingId by mutableStateOf<String?>(null)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var pending: Triple<String, String, String>? = null
    private var current: Locale? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { result ->
        main.post {
            if (result != TextToSpeech.SUCCESS) { status = Status.UNAVAILABLE; pending = null; return@post }
            if (useLanguage("en-IN") == null) { status = Status.UNAVAILABLE; pending = null; return@post }
            tts.setSpeechRate(0.96f)
            tts.setPitch(1.02f)
            status = Status.READY
            pending?.let { (text, lang, id) -> pending = null; speak(text, lang, id) }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { speakingId = id } }
            override fun onDone(id: String?) { main.post { if (speakingId == id) speakingId = null } }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { main.post { if (speakingId == id) speakingId = null } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { if (speakingId == id) speakingId = null } }
        })
    }

    /** Switches to [tag] if the phone has it, else Indian English, else any English. Returns the locale used. */
    private fun useLanguage(tag: String): Locale? {
        val wanted = listOf(Locale.forLanguageTag(tag), Locale("en", "IN"), Locale.UK, Locale.US)
        for (l in wanted) {
            if (l == current) return l
            if (tts.isLanguageAvailable(l) < TextToSpeech.LANG_AVAILABLE) continue
            if (tts.setLanguage(l) < TextToSpeech.LANG_AVAILABLE) continue
            // Prefer an installed voice for that language that works offline.
            tts.voices?.filter { it.locale.language == l.language && it.locale.country == l.country && !it.isNetworkConnectionRequired }
                ?.minByOrNull { it.latency }?.let { tts.voice = it }
            current = l
            return l
        }
        return null
    }

    /** Reads [text] aloud. Returns false only when the phone cannot speak at all. */
    fun speak(text: String, languageTag: String = "en-IN", id: String = text.hashCode().toString()): Boolean {
        when (status) {
            Status.UNAVAILABLE -> return false
            Status.STARTING -> { pending = Triple(text, languageTag, id); return true }
            Status.READY -> Unit
        }
        useLanguage(languageTag) ?: return false
        // Read the rupee sign and bullets the way a person would.
        val spoken = text.replace("₹", "rupees ").replace("·", ",")
        return tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, Bundle(), id) == TextToSpeech.SUCCESS
    }

    /** Listen / Stop on one answer. */
    fun toggle(text: String, languageTag: String, id: String): Boolean =
        if (speakingId == id) { stop(); true } else speak(text, languageTag, id)

    fun stop() {
        pending = null
        if (status == Status.READY) tts.stop()
        speakingId = null
    }

    fun shutdown() {
        main.removeCallbacksAndMessages(null)
        tts.shutdown()
    }
}
