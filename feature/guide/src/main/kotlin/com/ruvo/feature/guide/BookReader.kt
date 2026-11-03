package com.ruvo.feature.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.AmbientBackdrop
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.IconCircle
import com.ruvo.design.Motion
import com.ruvo.design.OutlineButton
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.design.reveal
import com.ruvo.engine.guide.Library
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Reads one book, page by page. Pages turn on a soft 3D hinge; the cover opens the book. The check page gives
 * gentle feedback; the last page offers one way to use the idea in Ruvo. Finishing marks the book read.
 */
@Composable
fun BookReader(bookId: String, onClose: () -> Unit, onApply: (Library.Apply) -> Unit, onAsk: (String) -> Unit) {
    val book = Library.byId(bookId) ?: run { onClose(); return }
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pageCount = book.pages.size + 1
    val pager = rememberPagerState { pageCount }
    var answer by rememberSaveable(bookId) { mutableStateOf<Int?>(null) }
    BackHandler(onBack = onClose)

    fun finish(apply: Library.Apply?) {
        scope.launch { RuvoGraph.repository.markBookRead(book.id) }
        Haptics.confirm(view)
        onClose()
        if (apply != null) onApply(apply)
    }

    AmbientBackdrop {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                IconCircle(RuvoIcons.Close, "Close book", onClose)
                Spacer(Modifier.width(Space.sm))
                // Progress: one segment per page, filling as you read.
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(pageCount) { i ->
                        val on by animateColorAsState(if (i <= pager.currentPage) c.fuel else c.ink.copy(alpha = 0.12f), label = "seg$i")
                        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(Radius.chip)).background(on))
                    }
                }
                Spacer(Modifier.width(Space.sm))
                val ideaKey = "${book.id}:${pager.currentPage}"
                val saved = ideaKey in state.settings.savedIdeas
                val canSave = pager.currentPage in 1..5
                IconCircle(
                    if (saved) RuvoIcons.Check else RuvoIcons.Spark,
                    if (saved) "Saved. Tap to remove" else "Save this idea",
                    { if (canSave) scope.launch { RuvoGraph.repository.toggleIdea(ideaKey); Haptics.tick(view) } },
                    tint = if (saved) c.onAccent else if (canSave) c.ink else c.inkFaint,
                    background = if (saved) c.fuel else c.glass,
                )
            }

            HorizontalPager(
                pager,
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.gutter),
                pageSpacing = Space.md,
            ) { page ->
                val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                Box(
                    Modifier.fillMaxSize().padding(vertical = Space.md).graphicsLayer {
                        if (!reduced) {
                            // A page hinged on its left edge: it swings away as you swipe on.
                            val o = offset.coerceIn(-1f, 1f)
                            transformOrigin = TransformOrigin(if (o > 0) 0f else 1f, 0.5f)
                            rotationY = -o * 28f
                            cameraDistance = 14f * density
                            alpha = 1f - abs(o) * 0.35f
                            val s = 1f - abs(o) * 0.06f
                            scaleX = s; scaleY = s
                        }
                    },
                ) {
                    if (page == 0) CoverPage(book, book.id in state.settings.readBooks)
                    else {
                        val (label, text) = book.pages[page - 1]
                        when (page) {
                            6 -> CheckPage(label, book.check, answer) { answer = it; Haptics.tick(view) }
                            7 -> ApplyPage(label, text, onApply = { finish(book.apply) }, onAsk = { onClose(); onAsk("Explain \"${book.title}\" in simple words") })
                            else -> TextPage(page, label, text)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (pager.currentPage > 0) {
                    GhostButton("Back", { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, Modifier.weight(1f))
                }
                val last = pager.currentPage == pageCount - 1
                PrimaryButton(
                    when {
                        pager.currentPage == 0 -> "Open the book"
                        last -> "Finish"
                        else -> "Next"
                    },
                    { if (last) finish(null) else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    Modifier.weight(if (pager.currentPage > 0) 1.6f else 1f),
                )
            }
        }
    }
}

@Composable
private fun CoverPage(book: Library.Book, read: Boolean) {
    val c = Ruvo.colors
    val lift = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { lift.animateTo(1f, Motion.gentle()) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BookCover(
            book, read, width = 200.dp, showLabel = false,
            modifier = Modifier.graphicsLayer {
                rotationZ = -4f * lift.value
                translationY = (1f - lift.value) * 40f
            },
        )
        Spacer(Modifier.height(Space.xl))
        Eyebrow(book.shelf.title, color = c.fuelInk)
        Spacer(Modifier.height(Space.xs))
        Text(book.title, Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text("${book.minutes} min read · ${book.pages.size} short pages", style = Ruvo.type.caption, color = c.inkSoft)
        if (read) {
            Spacer(Modifier.height(Space.sm))
            com.ruvo.design.Tag("You've read this", color = c.future.copy(alpha = 0.18f), content = c.futureInk)
        }
    }
}

@Composable
private fun TextPage(page: Int, label: String, text: String) {
    val c = Ruvo.colors
    RuvoCard(Modifier.fillMaxSize(), padding = androidx.compose.foundation.layout.PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text("$page", style = Ruvo.type.display, color = c.fuel)
            Spacer(Modifier.height(Space.xs))
            Eyebrow(label, color = c.inkSoft)
            Spacer(Modifier.height(Space.md))
            Text(text, Modifier.reveal(1), style = Ruvo.type.headline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(500), lineHeight = Ruvo.type.headline.lineHeight * 1.15f), color = c.ink)
            if (page == 4) {
                Spacer(Modifier.height(Space.lg))
                Text("Take a moment. There's no right answer here.", style = Ruvo.type.caption, color = c.inkFaint)
            }
        }
    }
}

@Composable
private fun CheckPage(label: String, check: Library.Check, answer: Int?, onAnswer: (Int) -> Unit) {
    val c = Ruvo.colors
    RuvoCard(Modifier.fillMaxSize(), padding = androidx.compose.foundation.layout.PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Eyebrow(label, color = c.inkSoft)
            Spacer(Modifier.height(Space.md))
            Text(check.question, style = Ruvo.type.headline, color = c.ink)
            Spacer(Modifier.height(Space.lg))
            check.options.forEachIndexed { i, opt ->
                val chosen = answer == i
                val right = i == check.correct
                val fill by animateColorAsState(
                    when {
                        answer == null -> c.glass
                        right -> c.future.copy(alpha = 0.22f)
                        chosen -> c.warning.copy(alpha = 0.14f)
                        else -> c.glass
                    },
                    label = "opt$i",
                )
                val scale by animateFloatAsState(if (chosen) 1.02f else 1f, Motion.snappy(), label = "optScale$i")
                Row(
                    Modifier.fillMaxWidth().padding(bottom = Space.sm).graphicsLayer { scaleX = scale; scaleY = scale }
                        .heightIn(min = 56.dp).clip(RoundedCornerShape(Radius.inner)).background(fill)
                        .border(1.dp, if (answer != null && right) c.future else c.hairline, RoundedCornerShape(Radius.inner))
                        .clickable(enabled = answer == null, role = Role.RadioButton) { onAnswer(i) }
                        .padding(horizontal = Space.md, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(if (answer != null && right) c.future else c.ink.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (answer != null && right) Icon(RuvoIcons.Check, null, Modifier.size(16.dp), tint = c.onAccent)
                        else Text(('A' + i).toString(), style = Ruvo.type.label, color = c.ink)
                    }
                    Spacer(Modifier.width(Space.sm))
                    Text(opt, style = Ruvo.type.bodyStrong, color = c.ink)
                }
            }
            AnimatedVisibility(answer != null, enter = fadeIn() + expandVertically()) {
                val right = answer == check.correct
                Column(Modifier.padding(top = Space.sm).semantics { contentDescription = (if (right) "Correct. " else "Not quite. ") + check.because }) {
                    Text(if (right) "That's it." else "Not quite, and that's how learning works.", style = Ruvo.type.title, color = if (right) c.futureInk else c.ink)
                    Spacer(Modifier.height(Space.xxs))
                    Text(check.because, style = Ruvo.type.body, color = c.inkSoft)
                }
            }
        }
    }
}

@Composable
private fun ApplyPage(label: String, text: String, onApply: () -> Unit, onAsk: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(Modifier.fillMaxSize(), brush = c.heroBrush, border = null, padding = androidx.compose.foundation.layout.PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Icon(RuvoIcons.Spark, null, Modifier.size(40.dp), tint = c.fuel)
            Spacer(Modifier.height(Space.md))
            Eyebrow(label, color = c.onHero.copy(alpha = 0.7f))
            Spacer(Modifier.height(Space.xs))
            Text(text, style = Ruvo.type.headline, color = c.onHero)
            Spacer(Modifier.height(Space.xl))
            PrimaryButton("Do it now", onApply, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.sm))
            OutlineButton("Ask Ruvo about this", onAsk, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.md))
            Text("Skipping is fine. No streaks, no pressure.", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.65f))
        }
    }
}
