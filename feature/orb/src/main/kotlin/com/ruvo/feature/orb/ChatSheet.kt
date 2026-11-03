package com.ruvo.feature.orb

import androidx.compose.foundation.background
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

private data class Bubble(val fromUser: Boolean, val text: String, val suggestions: List<String> = emptyList())

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

    fun ask(q: String) {
        if (q.isBlank()) return
        bubbles += Bubble(true, q)
        input = ""
        thinking = true
        scope.launch {
            val scripted = RuvoGuide.answer(q, ctx)
            val live = LiveRuvo.answer(q, ctx, bubbles.dropLast(1).map { it.fromUser to it.text })
            val text = live ?: scripted.text
            bubbles += Bubble(false, text, scripted.suggestions)
            thinking = false
            if (state.settings.spokenReplies) speech.speak(text)
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
                if (!b.fromUser && i == bubbles.lastIndex && b.suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        b.suggestions.forEach { s -> Chip(s, false, { ask(s) }) }
                    }
                }
            }
            if (thinking) Text("Ruvo is thinking", style = Ruvo.type.caption, color = c.inkFaint)
        }
        Spacer(Modifier.height(Space.md))
        Row(Modifier.fillMaxWidth().imePadding(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { RuvoField(input, { input = it }, "Ask about your money") }
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
