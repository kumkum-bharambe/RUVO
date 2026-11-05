package com.ruvo.feature.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.AmbientBackdrop
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.IconCircle
import com.ruvo.design.Motion
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.engine.guide.Glossary
import com.ruvo.engine.guide.Library
import com.ruvo.engine.guide.Stories
import com.ruvo.engine.guide.Tools
import com.ruvo.feature.orb.OnDeviceTranslate
import com.ruvo.feature.orb.RuvoSpeech
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The kinds of page a book is made of, in reading order. */
internal sealed interface BookPage {
    data object Cover : BookPage
    /** One of the book's own five text pages; [idea] is its number (1..5), which saved ideas are keyed by. */
    data class Words(val idea: Int, val label: String, val text: String, val art: Art) : BookPage
    data class Fact(val text: String) : BookPage
    data class Rule(val text: String) : BookPage
    data class Story(val story: Stories.Story) : BookPage
    data class Tool(val tool: Tools.Tool) : BookPage
    data object Quiz : BookPage
    data object Takeaways : BookPage
    data object Apply : BookPage
}

/** Which picture heads a text page. */
internal enum class Art { IDEA, SITUATION, WHY, REFLECT, TRY }

internal fun pagesOf(b: Library.Book): List<BookPage> = buildList {
    add(BookPage.Cover)
    add(BookPage.Words(1, "The idea", b.idea, Art.IDEA))
    add(BookPage.Words(2, "A real student situation", b.situation, Art.SITUATION))
    add(BookPage.Words(3, "Why it matters", b.why, Art.WHY))
    b.fact?.let { add(BookPage.Fact(it)) }
    b.rule?.let { add(BookPage.Rule(it)) }
    add(BookPage.Words(4, "Reflect", b.reflect, Art.REFLECT))
    b.story?.let { Stories.byId(it) }?.let { add(BookPage.Story(it)) }
    b.tool?.let { add(BookPage.Tool(it)) }
    add(BookPage.Words(5, "Try it this week", b.tryIt, Art.TRY))
    add(BookPage.Quiz)
    add(BookPage.Takeaways)
    add(BookPage.Apply)
}

/** Every piece of text a page shows, for translation and for reading aloud (the first entries are read). */
internal fun textsOf(page: BookPage, b: Library.Book): List<String> = when (page) {
    BookPage.Cover -> listOf(b.title, b.shelf.title)
    is BookPage.Words -> listOf(page.text, page.label)
    is BookPage.Fact -> listOf(page.text, "Did you know?")
    is BookPage.Rule -> listOf(page.text, "Rule of thumb")
    is BookPage.Story -> page.story.scenes.flatMap { s -> listOfNotNull(s.text, s.lesson) + s.choices.map { it.label } } + page.story.title
    is BookPage.Tool -> listOf(page.tool.title, page.tool.line)
    BookPage.Quiz -> b.quiz.flatMap { listOf(it.question) + it.options + it.because }
    BookPage.Takeaways -> b.keyPoints + listOfNotNull(b.rule) + "Key takeaways"
    BookPage.Apply -> listOf(b.applyLabel, "Put it to work in Ruvo")
}

/**
 * Reads one book, page by page, like a small illustrated paperback. Pages turn on a soft 3D hinge. Along the way
 * there is a "did you know", a rule of thumb, a choose-your-path story and a calculator where the book has them,
 * then a three-question quiz, the key takeaways and one way to use the idea in Ruvo. Money words are underlined;
 * tapping one explains it. The top bar can read the page aloud and switch the book to Hindi, both on the phone.
 * Finishing marks the book read.
 */
@Composable
fun BookReader(bookId: String, onClose: () -> Unit, onApply: (Library.Apply) -> Unit, onAsk: (String) -> Unit) {
    val book = Library.byId(bookId) ?: run { onClose(); return }
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = remember(bookId) { pagesOf(book) }
    val pager = rememberPagerState { pages.size }
    var term by remember { mutableStateOf<Glossary.Term?>(null) }
    BackHandler(onBack = onClose)

    // Read aloud, with the phone's own voice.
    val speech = remember { RuvoSpeech(context) }
    DisposableEffect(Unit) { onDispose { speech.shutdown() } }
    LaunchedEffect(pager.currentPage) { speech.stop() }

    // Hindi, translated on the phone and cached for this book. English stays underneath if anything fails.
    var hindi by rememberSaveable { mutableStateOf(false) }
    var hindiNote by remember { mutableStateOf<String?>(null) }
    val translated = remember { mutableStateMapOf<String, String>() }
    val tr: (String) -> String = { s -> if (hindi) translated[s] ?: s else s }
    LaunchedEffect(hindi, pager.currentPage) {
        if (!hindi) return@LaunchedEffect
        if (!OnDeviceTranslate.prepare("hi-IN")) {
            hindiNote = "Hindi isn't on this phone yet. Connect to the internet once to download it."
            hindi = false
            return@LaunchedEffect
        }
        hindiNote = null
        // This page first, then the next one, so turning a page is instant.
        listOfNotNull(pages.getOrNull(pager.currentPage), pages.getOrNull(pager.currentPage + 1)).forEach { p ->
            textsOf(p, book).forEach { s ->
                if (s !in translated) OnDeviceTranslate.fromEnglish(s, "hi-IN")?.let { translated[s] = it }
            }
        }
    }

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
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(pages.size) { i ->
                        val on by animateColorAsState(if (i <= pager.currentPage) c.brand else c.ink.copy(alpha = 0.12f), label = "seg$i")
                        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(Radius.chip)).background(on))
                    }
                }
                Spacer(Modifier.width(Space.xs))
                // Hindi on and off: the button shows the language it switches to.
                Box(
                    Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (hindi) c.brand else c.glass)
                        .border(1.dp, c.glassEdge, androidx.compose.foundation.shape.CircleShape)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = if (hindi) "Read in English" else "Read in Hindi") { hindi = !hindi; Haptics.tick(view) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (hindi) "EN" else "हिं", style = Ruvo.type.bodyStrong, color = if (hindi) c.onBrand else c.ink)
                }
                Spacer(Modifier.width(Space.xs))
                // Read this page aloud; tap again to stop.
                val pageId = "page${pager.currentPage}"
                val speaking = speech.speakingId == pageId
                IconCircle(
                    RuvoIcons.Speaker, if (speaking) "Stop reading" else "Read this page aloud",
                    {
                        val p = pages[pager.currentPage]
                        val words = textsOf(p, book).let { if (p is BookPage.Quiz || p is BookPage.Takeaways || p is BookPage.Story) it else it.take(1) }
                        val ok = speech.toggle(words.joinToString(". ") { tr(it) }, if (hindi) "hi-IN" else "en-IN", pageId)
                        if (!ok) hindiNote = "This phone has no voice for reading aloud. You can add one in Settings › Text-to-speech."
                    },
                    tint = if (speaking) c.onBrand else c.ink, background = if (speaking) c.brand else c.glass,
                )
            }
            hindiNote?.let {
                Text(it, Modifier.fillMaxWidth().padding(horizontal = Space.gutter), style = Ruvo.type.caption, color = c.warning, textAlign = TextAlign.Center)
            }

            HorizontalPager(
                pager,
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.gutter),
                pageSpacing = Space.md,
            ) { index ->
                val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
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
                    when (val p = pages[index]) {
                        BookPage.Cover -> CoverPage(book, book.id in state.settings.readBooks, pages.size - 1, tr)
                        is BookPage.Words -> {
                            val key = "${book.id}:${p.idea}"
                            WordsPage(book, p, read = book.id in state.settings.readBooks, glossary = !hindi, tr = tr, onTerm = { term = it }, saved = key in state.settings.savedIdeas) {
                                scope.launch { RuvoGraph.repository.toggleIdea(key); Haptics.tick(view) }
                            }
                        }
                        is BookPage.Fact -> NotePage(fact = true, text = p.text, glossary = !hindi, tr = tr, onTerm = { term = it })
                        is BookPage.Rule -> NotePage(fact = false, text = p.text, glossary = !hindi, tr = tr, onTerm = { term = it })
                        is BookPage.Story -> StoryPage(p.story, tr) { Haptics.tick(view) }
                        is BookPage.Tool -> ToolPage(p.tool, state.runway.dailySpend)
                        BookPage.Quiz -> QuizPage(book, tr) { Haptics.tick(view) }
                        BookPage.Takeaways -> TakeawaysPage(book, tr, onTerm = { term = it })
                        BookPage.Apply -> ApplyPage(tr(book.applyLabel), onApply = { finish(book.apply) }, onAsk = { onClose(); onAsk("Explain \"${book.title}\" in simple words") })
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (pager.currentPage > 0) {
                    GhostButton("Back", { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, Modifier.weight(1f))
                }
                val last = pager.currentPage == pages.size - 1
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
    term?.let { t -> TermSheet(t) { term = null } }
}

@Composable
private fun CoverPage(book: Library.Book, read: Boolean, pageCount: Int, tr: (String) -> String) {
    val c = Ruvo.colors
    val lift = remember { Animatable(0f) }
    LaunchedEffect(Unit) { lift.animateTo(1f, Motion.gentle()) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BookCover(
            book, read, width = 200.dp, showLabel = false,
            modifier = Modifier.graphicsLayer {
                rotationZ = -4f * lift.value
                translationY = (1f - lift.value) * 40f
            },
        )
        Spacer(Modifier.height(Space.xl))
        Eyebrow(tr(book.shelf.title))
        Spacer(Modifier.height(Space.xs))
        Text(tr(book.title), Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        val extras = listOfNotNull(if (book.story != null) "a story" else null, if (book.tool != null) "a calculator" else null, "a quiz")
        Text("${book.minutes} min read · $pageCount pages · ${extras.joinToString(", ")}", style = Ruvo.type.caption, color = c.inkSoft, textAlign = TextAlign.Center)
        if (read) {
            Spacer(Modifier.height(Space.sm))
            com.ruvo.design.StatusTag("You've read this", c.future)
        }
    }
}

@Composable
private fun ApplyPage(text: String, onApply: () -> Unit, onAsk: () -> Unit) {
    val c = Ruvo.colors
    com.ruvo.design.RuvoCard(Modifier.fillMaxSize(), brush = c.heroBrush, border = null, padding = androidx.compose.foundation.layout.PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            androidx.compose.material3.Icon(RuvoIcons.Bulb, null, Modifier.padding(bottom = Space.md).height(40.dp).width(40.dp), tint = c.fuel)
            Eyebrow("Put it to work in Ruvo", color = c.onHero.copy(alpha = 0.7f))
            Spacer(Modifier.height(Space.xs))
            Text(text, style = Ruvo.type.headline, color = c.onHero)
            Spacer(Modifier.height(Space.xl))
            PrimaryButton("Do it now", onApply, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.sm))
            com.ruvo.design.OutlineButton("Ask Ruvo about this", onAsk, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.md))
            Text("Skipping is fine. No streaks, no pressure.", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.65f))
        }
    }
}
