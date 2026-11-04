package com.ruvo.feature.orb

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Chip
import com.ruvo.design.IconCircle
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.engine.guide.RuvoGuide
import kotlinx.coroutines.launch

private data class Bubble(val fromUser: Boolean, val text: String, val suggestions: List<String> = emptyList(), val language: String = "en-IN")

/** Languages for dictation and, with live answers, for replies. Shown in their own script. */
private val LANGUAGES = listOf(
    "en-IN" to ("English" to "English"), "hi-IN" to ("हिन्दी" to "Hindi"), "mr-IN" to ("मराठी" to "Marathi"),
    "ta-IN" to ("தமிழ்" to "Tamil"), "te-IN" to ("తెలుగు" to "Telugu"), "bn-IN" to ("বাংলা" to "Bengali"), "gu-IN" to ("ગુજરાતી" to "Gujarati"),
)

/** Talking to Ruvo. Scripted answers by default; live answers when a key is set; spoken replies optional. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatSheet(onDismiss: () -> Unit, firstQuestion: String? = null) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val speech = remember { RuvoSpeech(context) }
    DisposableEffect(Unit) { onDispose { speech.shutdown() } }
    val ctx = RuvoGuide.Context(state.input, state.runway, state.owedToYou, state.displayName.takeIf { it != "there" } ?: "")
    val bubbles = remember {
        mutableStateListOf(Bubble(false, RuvoGuide.answer("hi", ctx).text, RuvoGuide.starterQuestions))
    }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val lang = state.settings.chatLanguage
    val langName = LANGUAGES.firstOrNull { it.first == lang }?.second ?: ("English" to "English")
    // Answers can only be in another language when live answers are on; scripted answers are English.
    val answerLang = if (LiveRuvo.enabled) lang else "en-IN"
    val dictate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) input = heard
    }
    val canDictate = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    fun ask(q: String) {
        if (q.isBlank()) return
        bubbles += Bubble(true, q)
        input = ""
        thinking = true
        scope.launch {
            val scripted = RuvoGuide.answer(q, ctx)
            val asked = if (answerLang.startsWith("en")) q else "$q\n\n(Reply in ${langName.second}, simply and warmly.)"
            val live = LiveRuvo.answer(asked, ctx, bubbles.dropLast(1).map { it.fromUser to it.text })
            val text = live ?: scripted.text
            val spokenIn = if (live != null) answerLang else "en-IN"
            bubbles += Bubble(false, text, scripted.suggestions, spokenIn)
            thinking = false
            if (state.settings.spokenReplies) speech.speak(text, spokenIn)
        }
    }
    LaunchedEffect(firstQuestion) { firstQuestion?.let { ask(it) } }

    RuvoSheet("Ruvo", onDismiss, eyebrow = if (LiveRuvo.enabled) "Live answers" else "Works offline") {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            bubbles.forEachIndexed { i, b ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (b.fromUser) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(topStart = Radius.inner, topEnd = Radius.inner, bottomStart = if (b.fromUser) Radius.inner else 4.dp, bottomEnd = if (b.fromUser) 4.dp else Radius.inner))
                            .background(if (b.fromUser) c.ink else c.surface)
                            .padding(horizontal = Space.md, vertical = Space.sm)
                            .semantics { if (!b.fromUser) liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(b.text, style = Ruvo.type.body, color = if (b.fromUser) c.canvas else c.ink)
                    }
                }
                if (!b.fromUser && i > 0) {
                    Text(
                        "Listen", Modifier.clip(RoundedCornerShape(Radius.chip)).clickable(role = Role.Button, onClickLabel = "Read this answer aloud") { speech.speak(b.text, b.language) }
                            .padding(horizontal = Space.xs, vertical = 4.dp),
                        style = Ruvo.type.label, color = c.inkSoft,
                    )
                }
                if (!b.fromUser && i == bubbles.lastIndex && b.suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        b.suggestions.forEach { s -> Chip(s, false, { ask(s) }) }
                    }
                }
            }
            if (thinking) Text("Ruvo is thinking", style = Ruvo.type.caption, color = c.inkFaint)
        }
        Spacer(Modifier.height(Space.md))
        // Language: changes dictation, and replies too when live answers are on.
        Chip("Language · ${langName.first}", picking, { picking = !picking })
        if (picking) {
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                LANGUAGES.forEach { (tag, names) ->
                    Chip(names.first, lang == tag, { scope.launch { RuvoGraph.repository.setChatLanguage(tag) }; picking = false })
                }
            }
        }
        if (!lang.startsWith("en") && !LiveRuvo.enabled) {
            Text(
                "You can speak in ${langName.second}. Offline answers stay in English; with live answers on, Ruvo replies in ${langName.second}.",
                Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint,
            )
        }
        Spacer(Modifier.height(Space.sm))
        Row(Modifier.fillMaxWidth().imePadding(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { RuvoField(input, { input = it }, if (lang.startsWith("en")) "Ask about your money" else "Ask in ${langName.second}") }
            Spacer(Modifier.size(Space.xs))
            IconCircle(
                RuvoIcons.Mic, if (canDictate) "Speak your question" else "Speech input is not available on this phone",
                {
                    if (canDictate) runCatching {
                        dictate.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Ruvo"),
                        )
                    }
                },
                tint = if (canDictate) c.ink else c.inkFaint,
            )
            Spacer(Modifier.size(Space.xs))
            IconCircle(RuvoIcons.Send, "Send", { ask(input) }, background = c.fuel, tint = c.onAccent)
        }
        Spacer(Modifier.height(Space.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ruvo explains your own numbers. It never recommends specific investments or loans.",
                Modifier.weight(1f), style = Ruvo.type.caption, color = c.inkFaint,
            )
            IconCircle(
                RuvoIcons.Speaker, if (state.settings.spokenReplies) "Turn off spoken replies" else "Turn on spoken replies",
                { scope.launch { RuvoGraph.repository.setSpokenReplies(!state.settings.spokenReplies) } },
                tint = if (state.settings.spokenReplies) c.fuel else c.inkFaint,
            )
        }
    }
}
