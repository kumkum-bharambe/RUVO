package com.ruvo.feature.guide

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.ruvo.design.Chip
import com.ruvo.design.Covers
import com.ruvo.design.Eyebrow
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.lighten
import com.ruvo.design.reveal
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise
import com.ruvo.engine.guide.Glossary
import com.ruvo.engine.guide.Library
import com.ruvo.engine.guide.Stories
import com.ruvo.engine.guide.Tools
import com.ruvo.engine.rupees
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin

private val Big: TextStyle @Composable get() = Ruvo.type.headline.copy(fontWeight = FontWeight(500), lineHeight = Ruvo.type.headline.lineHeight * 1.15f)

// ---------------------------------------------------------------- Text pages

/** One of the book's own pages, headed by a little picture, with money words underlined. */
@Composable
internal fun WordsPage(
    book: Library.Book, page: BookPage.Words, read: Boolean, glossary: Boolean, tr: (String) -> String,
    onTerm: (Glossary.Term) -> Unit, saved: Boolean, onSave: () -> Unit,
) {
    val c = Ruvo.colors
    RuvoCard(Modifier.fillMaxSize(), padding = PaddingValues(Space.xl)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            PageArt(page.art, book, read)
            Spacer(Modifier.height(Space.md))
            Eyebrow(tr(page.label))
            Spacer(Modifier.height(Space.sm))
            GlossaryText(tr(page.text), Big, c.ink, enabled = glossary, onTerm = onTerm, modifier = Modifier.reveal(1))
            if (page.art == Art.REFLECT) {
                Spacer(Modifier.height(Space.lg))
                Text("Take a moment. There's no right answer here.", style = Ruvo.type.caption, color = c.inkFaint)
            }
            if (glossary && Glossary.inText(page.text).isNotEmpty()) {
                Spacer(Modifier.height(Space.md))
                Text("Tap an underlined word to see what it means.", style = Ruvo.type.caption, color = c.inkFaint)
            }
        }
        Spacer(Modifier.height(Space.md))
        SaveIdeaButton(saved, onSave)
    }
}

/** "Did you know" and "Rule of thumb": the boxed asides of a real book. */
@Composable
internal fun NotePage(fact: Boolean, text: String, glossary: Boolean, tr: (String) -> String, onTerm: (Glossary.Term) -> Unit) {
    val c = Ruvo.colors
    val tint = if (fact) Color(0xFFFF8A45) else c.brand
    RuvoCard(Modifier.fillMaxSize(), padding = PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Spot(if (fact) RuvoIcons.Bulb else RuvoIcons.Check, tint)
            Spacer(Modifier.height(Space.lg))
            Eyebrow(tr(if (fact) "Did you know?" else "Rule of thumb"), color = if (fact) c.futureInk else c.brandInk)
            Spacer(Modifier.height(Space.sm))
            // A rule sits in a boxed panel with a coloured edge, like the tip boxes in a textbook.
            Row(
                Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).clip(RoundedCornerShape(Radius.inner))
                    .background(tint.copy(alpha = if (c.isDark) 0.16f else 0.1f)),
            ) {
                // The coloured edge runs the full height of the box.
                Box(Modifier.width(5.dp).fillMaxHeight().background(tint))
                GlossaryText(tr(text), Big, c.ink, enabled = glossary, onTerm = onTerm, modifier = Modifier.padding(Space.lg))
            }
        }
    }
}

/** Text with each money word underlined once; tapping it opens its meaning. */
@Composable
internal fun GlossaryText(text: String, style: TextStyle, color: Color, enabled: Boolean, onTerm: (Glossary.Term) -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    if (!enabled) { Text(text, modifier, style = style, color = color); return }
    val lower = text.lowercase()
    val spans = Glossary.all.mapNotNull { t ->
        t.forms.map { f -> Glossary.indexOfWord(lower, f.lowercase()) to f.length }.filter { it.first >= 0 }
            .minWithOrNull(compareBy<Pair<Int, Int>> { it.first }.thenByDescending { it.second })?.let { Triple(it.first, it.second, t) }
    }.sortedWith(compareBy<Triple<Int, Int, Glossary.Term>> { it.first }.thenByDescending { it.second })
    val link = TextLinkStyles(SpanStyle(color = c.brandInk, textDecoration = TextDecoration.Underline))
    val annotated = buildAnnotatedString {
        var pos = 0
        for ((start, len, term) in spans) {
            if (start < pos) continue
            append(text.substring(pos, start))
            withLink(LinkAnnotation.Clickable(term.word, link) { onTerm(term) }) { append(text.substring(start, start + len)) }
            pos = start + len
        }
        append(text.substring(pos))
    }
    Text(annotated, modifier, style = style, color = color)
}

/** A money word's meaning and an everyday example. */
@Composable
internal fun TermSheet(term: Glossary.Term, onDismiss: () -> Unit) {
    val c = Ruvo.colors
    RuvoSheet(term.word, onDismiss, eyebrow = "Money word") {
        Text(term.meaning, style = Ruvo.type.title, color = c.ink)
        Spacer(Modifier.height(Space.md))
        RuvoCard(Modifier.fillMaxWidth()) {
            Eyebrow("For example")
            Spacer(Modifier.height(Space.xxs))
            Text(term.example, style = Ruvo.type.body, color = c.ink)
        }
        if (term.also.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            Text("Also called: ${term.also.joinToString(", ")}", style = Ruvo.type.caption, color = c.inkSoft)
        }
        Spacer(Modifier.height(Space.lg))
        com.ruvo.design.GhostButton("Got it", onDismiss, Modifier.fillMaxWidth())
    }
}

/** Keeps the idea on this page in Saved ideas. The ribbon fills when saved. */
@Composable
private fun SaveIdeaButton(saved: Boolean, onSave: () -> Unit) {
    val c = Ruvo.colors
    val fill by animateColorAsState(if (saved) c.brand else c.ink.copy(alpha = 0.06f), label = "saveFill")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(saved) { if (saved) { pop.snapTo(0.92f); pop.animateTo(1f, Motion.bouncy()) } }
    Row(
        Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .heightIn(min = 48.dp).clip(RoundedCornerShape(Radius.chip)).background(fill)
            .border(1.dp, if (saved) c.brand else c.hairline, RoundedCornerShape(Radius.chip))
            .clickable(role = Role.Button, onClickLabel = if (saved) "Remove from saved ideas" else "Save this idea", onClick = onSave)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (saved) RuvoIcons.BookmarkFilled else RuvoIcons.Bookmark, null, Modifier.size(18.dp), tint = if (saved) c.onBrand else c.ink)
        Spacer(Modifier.width(Space.xs))
        Text(if (saved) "Saved to your ideas" else "Save this idea", style = Ruvo.type.label, color = if (saved) c.onBrand else c.ink)
    }
}

// ---------------------------------------------------------------- Pictures

/** The picture at the top of a text page: the book's own cover art for the story page, a bright spot otherwise. */
@Composable
private fun PageArt(art: Art, book: Library.Book, read: Boolean) {
    when (art) {
        Art.SITUATION -> Row(verticalAlignment = Alignment.Bottom) {
            BookCover(book, read, width = 84.dp, showLabel = false, modifier = Modifier.graphicsLayer { rotationZ = -6f })
            Spacer(Modifier.width(Space.sm))
            Spot(RuvoIcons.User, Covers.brights[4].first, small = true)
        }
        Art.IDEA -> Spot(RuvoIcons.Bulb, Covers.brights[0].first)
        Art.WHY -> Spot(RuvoIcons.Target, Covers.brights[2].first)
        Art.REFLECT -> Spot(RuvoIcons.Message, Covers.brights[5].first)
        Art.TRY -> Spot(RuvoIcons.Calendar, Covers.brights[1].first)
    }
}

/** A glossy coloured blob with an icon and three little sparkles that bob around it. */
@Composable
private fun Spot(icon: ImageVector, tint: Color, small: Boolean = false) {
    val reduced = Ruvo.reducedMotion
    val loop = rememberInfiniteTransition(label = "spot")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val size = if (small) 44.dp else 76.dp
    Box(Modifier.size(size + 20.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val p = if (reduced) 0.2f else t
            val a = p * 2f * PI.toFloat()
            listOf(0f, 2.1f, 4.2f).forEachIndexed { i, off ->
                val r = size.toPx() * 0.62f
                val at = center + Offset(kotlin.math.cos(a + off) * r, sin(a * 1.3f + off) * r * 0.8f)
                drawCircle(tint.lighten(0.3f).copy(alpha = 0.7f), (3 + i).dp.toPx(), at)
            }
        }
        Box(
            Modifier.size(size).clip(CircleShape)
                .background(Brush.linearGradient(listOf(tint.lighten(0.35f), tint)))
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(if (small) 22.dp else 36.dp), tint = Color.White)
        }
    }
}

// ---------------------------------------------------------------- Story

/** A choose-your-path story: read the moment, pick what happens, see where it leads, then try another path. */
@Composable
internal fun StoryPage(story: Stories.Story, tr: (String) -> String, onChoose: () -> Unit) {
    val c = Ruvo.colors
    var at by rememberSaveable(story.id) { mutableStateOf(story.start.id) }
    var steps by rememberSaveable(story.id) { mutableIntStateOf(0) }
    val scene = story.scene(at) ?: story.start
    RuvoCard(Modifier.fillMaxSize(), padding = PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spot(RuvoIcons.Split, Covers.brights[3].first, small = true)
                Spacer(Modifier.width(Space.sm))
                Column {
                    Eyebrow(tr("Choose your path"))
                    Text(tr(story.title), style = Ruvo.type.title, color = c.ink)
                }
            }
            Spacer(Modifier.height(Space.md))
            AnimatedContent(scene, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "scene") { s ->
                Column {
                    Text(tr(s.text), style = Big, color = c.ink)
                    Spacer(Modifier.height(Space.lg))
                    if (s.isEnding) {
                        RuvoCard(Modifier.fillMaxWidth(), brush = c.irisBrush, border = null) {
                            Eyebrow(tr("What happened"), color = c.onLight.copy(alpha = 0.7f))
                            Spacer(Modifier.height(Space.xxs))
                            Text(tr(s.lesson.orEmpty()), style = Ruvo.type.bodyStrong, color = c.onLight)
                        }
                        Spacer(Modifier.height(Space.md))
                        com.ruvo.design.GhostButton("Try another path", { at = story.start.id; steps = 0; onChoose() }, Modifier.fillMaxWidth())
                    } else {
                        s.choices.forEach { ch ->
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = Space.sm).heightIn(min = 56.dp)
                                    .clip(RoundedCornerShape(Radius.inner)).background(c.glassTop)
                                    .border(1.dp, if (c.isDark) c.glassEdge else c.hairline, RoundedCornerShape(Radius.inner))
                                    .clickable(role = Role.Button) { at = ch.next; steps++; onChoose() }
                                    .padding(horizontal = Space.md, vertical = Space.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(tr(ch.label), Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink)
                                Icon(RuvoIcons.Forward, null, Modifier.size(18.dp), tint = c.inkSoft)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Calculators

/** A slider in rupees or plain numbers, with its label and current value. */
@Composable
private fun ValueSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit) {
    val c = Ruvo.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = Ruvo.type.label, color = c.inkSoft)
        Text(shown, style = Ruvo.type.bodyStrong, color = c.ink)
    }
    Slider(
        value, onChange, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = c.brand, activeTrackColor = c.brand, inactiveTrackColor = c.ink.copy(alpha = 0.12f)),
        modifier = Modifier.semantics { contentDescription = "$label: $shown" },
    )
}

/** A row of example rates to pick from. */
@Composable
private fun RateChips(label: String, rates: List<Double>, chosen: Double, onPick: (Double) -> Unit) {
    val c = Ruvo.colors
    Text(label, style = Ruvo.type.label, color = c.inkSoft)
    Spacer(Modifier.height(Space.xxs))
    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        rates.forEach { r -> Chip("${if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()}%", chosen == r, { onPick(r) }) }
    }
}

/** The answer of a calculator: a big number and what it means. */
@Composable
private fun Result(label: String, value: String, line: String, tint: Color) {
    val c = Ruvo.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).background(tint.copy(alpha = if (c.isDark) 0.18f else 0.12f)).padding(Space.md),
    ) {
        Text(label, style = Ruvo.type.caption, color = c.inkSoft)
        Text(value, style = Ruvo.type.display, color = c.ink)
        Text(line, style = Ruvo.type.caption, color = c.inkSoft)
    }
}

private fun rs(p: Paise) = IndianFormat.rupees(p)

private fun monthsWords(m: Int): String {
    val y = m / 12
    val r = m % 12
    return listOfNotNull(if (y > 0) "$y year${if (y > 1) "s" else ""}" else null, if (r > 0) "$r month${if (r > 1) "s" else ""}" else null).joinToString(" ")
}

/** A calculator that turns the book's idea into the reader's own numbers. Always an estimate for learning. */
@Composable
internal fun ToolPage(tool: Tools.Tool, dailySpend: Paise) {
    val c = Ruvo.colors
    RuvoCard(Modifier.fillMaxSize(), padding = PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spot(RuvoIcons.Chart, Covers.brights[3].first, small = true)
                Spacer(Modifier.width(Space.sm))
                Column {
                    Eyebrow("Try it with numbers")
                    Text(tool.title, style = Ruvo.type.title, color = c.ink)
                }
            }
            Spacer(Modifier.height(Space.xs))
            Text(tool.line, style = Ruvo.type.body, color = c.inkSoft)
            Spacer(Modifier.height(Space.md))
            when (tool) {
                Tools.Tool.SMALL_SPENDS -> {
                    var week by rememberSaveable { mutableFloatStateOf(300f) }
                    val perWeek = rupees((week / 50f).roundToLong() * 50)
                    ValueSlider("A habit, each week", week, 50f..2_000f, rs(perWeek)) { week = it }
                    val h = Tools.smallSpends(perWeek, dailySpend)
                    Spacer(Modifier.height(Space.sm))
                    Result("In a year", rs(h.perYear), if (h.days > 0) "That's about ${IndianFormat.days(h.days)} of your usual spending." else "Add a few spends to see it in days.", c.fuel)
                }
                Tools.Tool.GROWTH -> {
                    var monthly by rememberSaveable { mutableFloatStateOf(500f) }
                    var years by rememberSaveable { mutableFloatStateOf(5f) }
                    var rate by rememberSaveable { mutableStateOf(7.0) }
                    val m = rupees((monthly / 100f).roundToLong() * 100)
                    ValueSlider("Each month", monthly, 100f..5_000f, rs(m)) { monthly = it }
                    ValueSlider("For", years, 1f..15f, "${years.toInt()} years") { years = it }
                    RateChips("Example yearly growth", listOf(4.0, 7.0, 10.0), rate) { rate = it }
                    val g = Tools.growth(m, years.toInt(), rate)
                    Spacer(Modifier.height(Space.sm))
                    Result("Could become about", rs(g.endsAt), "You put in ${rs(g.putIn)}; about ${rs(g.earned)} is growth on top.", c.future)
                    GrowthBars(g.putIn, g.endsAt)
                }
                Tools.Tool.INFLATION -> {
                    var amount by rememberSaveable { mutableFloatStateOf(10_000f) }
                    var years by rememberSaveable { mutableFloatStateOf(10f) }
                    var rate by rememberSaveable { mutableStateOf(6.0) }
                    val a = rupees((amount / 500f).roundToLong() * 500)
                    ValueSlider("Money kept still", amount, 1_000f..50_000f, rs(a)) { amount = it }
                    ValueSlider("After", years, 1f..20f, "${years.toInt()} years") { years = it }
                    RateChips("Prices rise each year by", listOf(4.0, 6.0, 8.0), rate) { rate = it }
                    val later = Tools.inflation(a, years.toInt(), rate)
                    Spacer(Modifier.height(Space.sm))
                    Result("It will buy what this buys today", rs(later), "The note still says ${rs(a)}, but prices have moved.", c.warning)
                }
                Tools.Tool.EMI -> {
                    var price by rememberSaveable { mutableFloatStateOf(12_000f) }
                    var months by rememberSaveable { mutableFloatStateOf(6f) }
                    var rate by rememberSaveable { mutableStateOf(16.0) }
                    val p = rupees((price / 500f).roundToLong() * 500)
                    ValueSlider("Price", price, 1_000f..1_00_000f, rs(p)) { price = it }
                    ValueSlider("Spread over", months, 3f..24f, "${months.toInt()} months") { months = it }
                    RateChips("Yearly interest", listOf(0.0, 16.0, 24.0), rate) { rate = it }
                    val loan = Tools.emi(p, rate, months.toInt())
                    Spacer(Modifier.height(Space.sm))
                    Result("Each month", rs(loan.emi), "You pay ${rs(loan.totalPaid)} in all: ${rs(loan.extra(p))} more than the price, before any fees.", c.fixed)
                }
                Tools.Tool.CARD_MINIMUM -> {
                    var bill by rememberSaveable { mutableFloatStateOf(12_000f) }
                    val b = rupees((bill / 500f).roundToLong() * 500)
                    ValueSlider("Card bill", bill, 1_000f..50_000f, rs(b)) { bill = it }
                    val pay = Tools.cardMinimum(b)
                    Spacer(Modifier.height(Space.sm))
                    Result("Pay the total due", "₹0", "No interest at all, if paid by the due date.", c.future)
                    Spacer(Modifier.height(Space.sm))
                    Result(
                        "Pay only the minimum",
                        if (pay.cleared) monthsWords(pay.months) else "Never clears",
                        "About ${rs(pay.interest)} in interest, at 3.5% a month with no new spending.",
                        c.warning,
                    )
                }
            }
            Spacer(Modifier.height(Space.md))
            Text("An example for learning, not advice. Real rates and returns vary and are never promised.", style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
}

/** Two bars: what you put in, and what it became. */
@Composable
private fun GrowthBars(putIn: Paise, endsAt: Paise) {
    val c = Ruvo.colors
    val f by animateFloatAsState((putIn.toFloat() / endsAt.coerceAtLeast(1)).coerceIn(0.05f, 1f), Motion.soft(), label = "grow")
    Spacer(Modifier.height(Space.sm))
    Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(Radius.chip)).background(c.future)) {
        Box(Modifier.fillMaxWidth(f).height(14.dp).clip(RoundedCornerShape(Radius.chip)).background(c.brand))
    }
    Spacer(Modifier.height(Space.xxs))
    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        com.ruvo.design.LegendDot(c.brand, "What you put in")
        com.ruvo.design.LegendDot(c.future, "Growth")
    }
}

// ---------------------------------------------------------------- Quiz and takeaways

/** Three questions, one after another on the same page, each explaining itself, with a kind score at the end. */
@Composable
internal fun QuizPage(book: Library.Book, tr: (String) -> String, onAnswer: () -> Unit) {
    val c = Ruvo.colors
    val answers = remember(book.id) { mutableStateListOf<Int?>().apply { repeat(book.quiz.size) { add(null) } } }
    RuvoCard(Modifier.fillMaxSize(), padding = PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spot(RuvoIcons.Target, Covers.brights[2].first, small = true)
                Spacer(Modifier.width(Space.sm))
                Column {
                    Eyebrow(tr("Check your understanding"))
                    Text("${book.quiz.size} quick questions", style = Ruvo.type.title, color = c.ink)
                }
            }
            book.quiz.forEachIndexed { qi, q ->
                Spacer(Modifier.height(Space.lg))
                Text("${qi + 1}. ${tr(q.question)}", style = Ruvo.type.title, color = c.ink)
                Spacer(Modifier.height(Space.sm))
                val answer = answers[qi]
                q.options.forEachIndexed { i, opt ->
                    val chosen = answer == i
                    val right = i == q.correct
                    val fill by animateColorAsState(
                        when {
                            answer == null -> c.glassTop
                            right -> c.future.copy(alpha = 0.22f)
                            chosen -> c.warning.copy(alpha = 0.14f)
                            else -> c.glassTop
                        },
                        label = "opt$qi$i",
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = Space.xs)
                            .heightIn(min = 52.dp).clip(RoundedCornerShape(Radius.inner)).background(fill)
                            .border(1.dp, if (answer != null && right) c.future else if (c.isDark) c.glassEdge else c.hairline, RoundedCornerShape(Radius.inner))
                            .clickable(enabled = answer == null, role = Role.RadioButton) { answers[qi] = i; onAnswer() }
                            .padding(horizontal = Space.md, vertical = Space.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(if (answer != null && right) c.future else c.ink.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (answer != null && right) Icon(RuvoIcons.Check, null, Modifier.size(16.dp), tint = c.onLight)
                            else Text(('A' + i).toString(), style = Ruvo.type.label, color = c.ink)
                        }
                        Spacer(Modifier.width(Space.sm))
                        Text(tr(opt), style = Ruvo.type.bodyStrong, color = c.ink)
                    }
                }
                AnimatedVisibility(answer != null, enter = fadeIn() + expandVertically()) {
                    val right = answer == q.correct
                    Column(Modifier.padding(top = Space.xxs).semantics { contentDescription = (if (right) "Correct. " else "Not quite. ") + q.because }) {
                        Text(if (right) "That's it." else "Not quite, and that's how learning works.", style = Ruvo.type.bodyStrong, color = if (right) c.futureInk else c.ink)
                        Text(tr(q.because), style = Ruvo.type.body, color = c.inkSoft)
                    }
                }
            }
            if (answers.all { it != null }) {
                val score = answers.indices.count { answers[it] == book.quiz[it].correct }
                Spacer(Modifier.height(Space.lg))
                RuvoCard(Modifier.fillMaxWidth(), brush = c.irisBrush, border = null) {
                    Text("$score of ${book.quiz.size}", style = Ruvo.type.display, color = c.onLight)
                    Text(
                        when (score) {
                            book.quiz.size -> "All right. You've got this idea."
                            0 -> "A fresh start. The takeaways on the next page sum it up."
                            else -> "Nicely done. The takeaways on the next page cover the rest."
                        },
                        style = Ruvo.type.body, color = c.onLight,
                    )
                }
            }
        }
    }
}

/** The summary at the back of the book: what to remember, its rule of thumb, and the money words it used. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TakeawaysPage(book: Library.Book, tr: (String) -> String, onTerm: (Glossary.Term) -> Unit) {
    val c = Ruvo.colors
    val words = Glossary.inText(book.pages.joinToString(" ") { it.second } + " " + (book.rule ?: "") + " " + (book.fact ?: ""))
    RuvoCard(Modifier.fillMaxSize(), padding = PaddingValues(Space.xl)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spot(RuvoIcons.BookmarkFilled, Covers.brights[0].first, small = true)
                Spacer(Modifier.width(Space.sm))
                Column {
                    Eyebrow(tr("Key takeaways"))
                    Text(book.title, style = Ruvo.type.title, color = c.ink)
                }
            }
            Spacer(Modifier.height(Space.md))
            book.keyPoints.forEachIndexed { i, point ->
                Row(Modifier.padding(bottom = Space.sm).reveal(i), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 2.dp).size(24.dp).clip(CircleShape).background(c.future), contentAlignment = Alignment.Center) {
                        Icon(RuvoIcons.Check, null, Modifier.size(14.dp), tint = c.onLight)
                    }
                    Spacer(Modifier.width(Space.sm))
                    Text(tr(point), style = Ruvo.type.bodyStrong, color = c.ink)
                }
            }
            book.rule?.let { rule ->
                Spacer(Modifier.height(Space.sm))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).background(c.brandTint).padding(Space.md)) {
                    Text(tr("Rule of thumb"), style = Ruvo.type.label, color = c.brandInk)
                    Text(tr(rule), style = Ruvo.type.body, color = c.ink)
                }
            }
            if (words.isNotEmpty()) {
                Spacer(Modifier.height(Space.lg))
                Text("Money words in this book", style = Ruvo.type.label, color = c.inkSoft)
                Spacer(Modifier.height(Space.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    words.forEach { t -> Chip(t.word, false, { onTerm(t) }) }
                }
            }
        }
    }
}
