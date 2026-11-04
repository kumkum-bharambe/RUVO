package com.ruvo.feature.orb

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
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

    RuvoSheet("Ask Ruvo", onDismiss, eyebrow = if (LiveRuvo.enabled) "Your money guide · live" else "Your money guide · works offline") {
        // Language pill and menu: each language in its own script, with its English name.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(Radius.chip)).background(c.glass).border(1.dp, c.hairline, RoundedCornerShape(Radius.chip))
                    .clickable(role = Role.Button, onClickLabel = "Choose language") { picking = !picking }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(RuvoIcons.Message, null, Modifier.size(16.dp), tint = c.ink)
                Spacer(Modifier.size(6.dp))
                Text(langName.first, style = Ruvo.type.bodyStrong, color = c.ink)
            }
            Spacer(Modifier.weight(1f))
            IconCircle(
                RuvoIcons.Speaker, if (state.settings.spokenReplies) "Turn off spoken replies" else "Turn on spoken replies",
                { scope.launch { RuvoGraph.repository.setSpokenReplies(!state.settings.spokenReplies) } },
                tint = if (state.settings.spokenReplies) c.onAccent else c.inkFaint, background = if (state.settings.spokenReplies) c.fuel else c.glass,
            )
        }
        androidx.compose.animation.AnimatedVisibility(picking) {
            Column(
                Modifier.padding(top = Space.xs).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).border(1.dp, c.hairline, RoundedCornerShape(20.dp)).padding(6.dp),
            ) {
                LANGUAGES.forEach { (tag, names) ->
                    val on = lang == tag
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) c.fuel.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable(role = Role.RadioButton) { scope.launch { RuvoGraph.repository.setChatLanguage(tag) }; picking = false }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(names.first, style = Ruvo.type.bodyStrong, color = c.ink)
                            Text(names.second, style = Ruvo.type.caption, color = c.inkFaint)
                        }
                        if (on) androidx.compose.material3.Icon(RuvoIcons.Check, null, Modifier.size(18.dp), tint = c.fuelInk)
                    }
                }
            }
        }
        if (!lang.startsWith("en") && !LiveRuvo.enabled) {
            Text(
                "You can speak in ${langName.second}. Offline answers stay in English; with live answers on, Ruvo replies in ${langName.second}.",
                Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint,
            )
        }
        Spacer(Modifier.height(Space.md))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            bubbles.forEachIndexed { i, b ->
                if (b.fromUser) {
                    // Your message: navy (amber in Night swim), tail on the right.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            b.text,
                            Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp))
                                .background(if (c.isDark) c.fuel else c.ink).padding(horizontal = 16.dp, vertical = 12.dp),
                            style = Ruvo.type.bodyStrong, color = if (c.isDark) c.onAccent else c.surface,
                        )
                    }
                } else {
                    // Ruvo's answer: glass bubble beside a small amber orb.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(c.orbCore, c.orbGlow, c.orbDeep))),
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                                .background(c.glass).border(1.dp, c.glassEdge, RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        ) {
                            Text(b.text, style = Ruvo.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(500)), color = c.ink)
                            if (i > 0) {
                                Spacer(Modifier.height(Space.xs))
                                Row(
                                    Modifier.clip(RoundedCornerShape(Radius.chip)).clickable(role = Role.Button, onClickLabel = "Read this answer aloud") { speech.speak(b.text, b.language) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    androidx.compose.material3.Icon(RuvoIcons.Speaker, null, Modifier.size(16.dp), tint = c.inkSoft)
                                    Spacer(Modifier.size(6.dp))
                                    Text("Listen", style = Ruvo.type.label, color = c.inkSoft)
                                }
                            }
                        }
                    }
                }
                if (!b.fromUser && i == bubbles.lastIndex && b.suggestions.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        items(b.suggestions.size) { k -> Chip(b.suggestions[k], false, { ask(b.suggestions[k]) }) }
                    }
                }
            }
            if (thinking) Text("Ruvo is thinking", style = Ruvo.type.caption, color = c.inkFaint)
        }
        Spacer(Modifier.height(Space.md))
        // The pill input: the mic turns into Send as soon as there is text.
        Row(
            Modifier.fillMaxWidth().imePadding().clip(RoundedCornerShape(32.dp)).background(c.glass)
                .border(1.dp, c.hairline, RoundedCornerShape(32.dp)).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                if (input.isEmpty()) Text(if (lang.startsWith("en")) "Ask anything..." else "Ask in ${langName.second}...", style = Ruvo.type.body, color = c.inkFaint)
                androidx.compose.foundation.text.BasicTextField(
                    input, { input = it }, Modifier.fillMaxWidth(),
                    textStyle = Ruvo.type.body.copy(color = c.ink), singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.ink),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { ask(input) }),
                )
            }
            val typing = input.isNotBlank()
            Box(
                Modifier.size(46.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.fuel)
                    .clickable(role = Role.Button, onClickLabel = if (typing) "Send" else "Speak your question") {
                        if (typing) ask(input)
                        else if (canDictate) runCatching {
                            dictate.launch(
                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                                    .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Ruvo"),
                            )
                        }
                    }
                    .semantics { contentDescription = if (typing) "Send" else if (canDictate) "Speak your question" else "Speech input is not available on this phone" },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(if (typing) RuvoIcons.Send else RuvoIcons.Mic, null, Modifier.size(22.dp), tint = c.onAccent)
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "Ruvo explains your own numbers. It never recommends specific investments or loans.",
            style = Ruvo.type.caption, color = c.inkFaint,
        )
    }
}