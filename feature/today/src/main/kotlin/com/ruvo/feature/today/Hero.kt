package com.ruvo.feature.today

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ruvo.design.Eyebrow
import com.ruvo.design.lighten
import com.ruvo.design.Haptics
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.RollingNumber
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.design.hardShadow
import com.ruvo.design.softShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.ruvo.engine.DayStrip
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Insights
import com.ruvo.engine.RunwayResult
import kotlinx.coroutines.delay

/** "Next money in 19 days · 4 days spare", or the honest alternative. */
fun runwaySubline(r: RunwayResult): String {
    val next = r.daysToNextIncome
    return when {
        r.unlimited -> "Spending is near zero, so your runway has no end"
        r.incomeOverdue -> "Your next money is late · runway is what counts"
        next == null -> "At your usual pace · income comes when it comes"
        r.makesIt -> "Next money in ${IndianFormat.days(next)} · ${IndianFormat.days(r.spareDays)} spare"
        else -> "Next money in ${IndianFormat.days(next)} · ${IndianFormat.days(-(r.bufferDays ?: 0))} short"
    }
}

fun runwaySpoken(r: RunwayResult): String =
    if (r.unlimited) "Runway: no end at current spending." else "Runway ${IndianFormat.days(r.runwayDays)}. ${runwaySubline(r).replace("·", ".")}"

/**
 * The hero: a big pane of white glass. A warm sun glows in its top corner, a blue light pools at the bottom left
 * and a blush of pink drifts between them, all slowly breathing, with a sheen that sweeps across every few
 * seconds. The runway number sits huge in ink, with the next money, the Fuel behind it and a three-word mood.
 * By day the glass is milky white; at night it is smoky, so the lights show through more.
 */
@Composable
fun RunwayHero(r: RunwayResult, onWhy: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val mood = Insights.mood(r)
    val radius = Radius.card + 6.dp
    val shape = RoundedCornerShape(radius)
    val loop = rememberInfiniteTransition(label = "sun")
    val sheen by loop.animateFloat(-0.8f, 2.2f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "sheen")
    val glow by loop.animateFloat(0.75f, 1f, infiniteRepeatable(tween(3200), RepeatMode.Reverse), label = "glow")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "drift")
    val dark = c.isDark

    Box(
        modifier.fillMaxWidth()
            .then(if (dark) Modifier else Modifier.softShadow(radius, c.shadow, spread = 22.dp, drop = 10.dp))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.05f))
                    else listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.6f)),
                ),
            )
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.3f else 1f), Color.White.copy(alpha = if (dark) 0.05f else 0.4f))), shape)
            .clickable(role = Role.Button, onClickLabel = "Why this many days", onClick = onWhy)
            .semantics { contentDescription = runwaySpoken(r) + ". " + mood.title },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val g = if (reduced) 0.9f else glow
            val a = (if (reduced) 0.2f else drift) * 2f * PI.toFloat()
            fun light(color: Color, at: Offset, r: Float) = drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), at, r), r, at)
            // A warm sun in the top corner, breathing.
            light(c.fuel.copy(alpha = (if (dark) 0.2f else 0.5f) * g), Offset(size.width * (0.96f + 0.04f * cos(a)), size.height * (0.02f + 0.04f * sin(a))), size.width * 0.8f)
            // Blue pooling at the bottom left.
            light(c.brand.lighten(0.3f).copy(alpha = if (dark) 0.34f else 0.32f), Offset(size.width * (0.04f + 0.06f * sin(a)), size.height * 0.98f), size.width * 0.85f)
            // A blush of pink drifting through the middle.
            light(Color(0xFFFF8FB8).copy(alpha = if (dark) 0.12f else 0.22f), Offset(size.width * (0.7f + 0.12f * cos(a)), size.height * (0.62f + 0.1f * sin(a))), size.width * 0.5f)
            if (!reduced) {
                val x = size.width * sheen
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.07f else 0.3f), Color.Transparent), Offset(x - size.width * 0.3f, 0f), Offset(x + size.width * 0.3f, size.height)))
            }
            // A bright line along the top edge, where light catches the glass.
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.3f else 1f), Color.Transparent)), size = Size(size.width, 1.5.dp.toPx()))
        }
        Column(Modifier.fillMaxWidth().padding(Space.xl).clearAndSetSemantics { }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Estimated financial runway", Modifier.weight(1f), color = c.inkSoft)
            }
            Spacer(Modifier.height(Space.xs))
            val heroStyle = if (r.runwayDays >= 100) Ruvo.type.hero.copy(fontSize = Ruvo.type.hero.fontSize * 0.78f) else Ruvo.type.hero
            Row(verticalAlignment = Alignment.Bottom) {
                if (r.unlimited) Text("∞", style = heroStyle, color = c.ink)
                else RollingNumber(r.runwayDays, heroStyle, c.ink)
                Spacer(Modifier.width(Space.xs))
                Text(if (r.runwayDays == 1) "day" else "days", Modifier.padding(bottom = 16.dp), style = Ruvo.type.display, color = c.ink, maxLines = 1, softWrap = false)
            }
            Text(runwaySubline(r), style = Ruvo.type.bodyStrong, color = if (r.makesIt) c.ink else c.warning)
            Spacer(Modifier.height(Space.xxs))
            Text("${IndianFormat.rupees(r.fuel.coerceAtLeast(0))} of Fuel at about ${IndianFormat.rupees(r.dailySpend)} a day", style = Ruvo.type.caption, color = c.inkSoft)
            val toIncome = r.daysToNextIncome
            if (!r.unlimited && toIncome != null && toIncome > 0) {
                Spacer(Modifier.height(Space.md))
                RunwayStrip(r)
            }
            Spacer(Modifier.height(Space.md))
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                // The one strong pill on the card: ink, so it reads as the way in.
                val onInk = if (dark) c.onLight else Color.White
                Row(
                    Modifier.clip(RoundedCornerShape(Radius.chip)).background(c.ink).padding(horizontal = Space.md, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Why ${if (r.unlimited) "no end" else IndianFormat.days(r.runwayDays)}?", style = Ruvo.type.label, color = onInk, maxLines = 1)
                    Icon(RuvoIcons.Forward, null, Modifier.padding(start = 2.dp).size(14.dp), tint = onInk)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(Radius.chip)).background(c.ink.copy(alpha = if (dark) 0.12f else 0.07f)).padding(horizontal = Space.sm, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dot = when (mood.level) {
                        Insights.MoodLevel.COMFORTABLE -> c.future
                        Insights.MoodLevel.WATCH -> c.fuel
                        Insights.MoodLevel.PLAN -> c.warning
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(mood.title, style = Ruvo.type.label, color = c.ink, maxLines = 1)
                }
            }
        }
    }
}
/**
 * The day strip: one rounded pill per day to the next money. Covered days are filled amber, the day money runs out
 * is part-filled, days after it are hatched in coral, and the income day carries a star. A fade at the edge says
 * there is more.
 */
@Composable
fun DayStripRow(r: RunwayResult, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val days = DayStrip.build(r)
    val runsOut = DayStrip.runsOutOn(r)
    val caption = when {
        r.unlimited -> "Every day is covered"
        r.daysToNextIncome == null -> runsOut?.let { "At this pace your Fuel lasts until ${IndianFormat.date(it, r.today)}" } ?: ""
        r.makesIt -> "Every day to your next money is covered"
        else -> runsOut?.let { "Fuel runs out around ${IndianFormat.date(it, r.today)} · swipe to see the gap" } ?: ""
    }
    Column(modifier.semantics { contentDescription = caption }) {
        Box {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.gutter)) {
                itemsIndexed(days, key = { _, d -> d.date.toEpochDay() }) { i, d ->
                    DayPill(d, DayStrip.fill(r, i))
                }
            }
            Box(
                Modifier.align(Alignment.CenterEnd).width(40.dp).height(88.dp)
                    .background(Brush.horizontalGradient(listOf(c.canvas.copy(alpha = 0f), c.canvas))),
            )
        }
        Text(caption, Modifier.padding(horizontal = Space.gutter, vertical = Space.xs), style = Ruvo.type.caption, color = c.inkSoft)
    }
}

@Composable
private fun DayPill(d: DayStrip.Day, fill: Float) {
    val c = Ruvo.colors
    val target = if (d.state == DayStrip.State.INCOME) 1f else fill
    val animated by animateFloatAsState(target, Motion.soft(), label = "fill")
    val accent = when (d.state) {
        DayStrip.State.INCOME -> c.future
        DayStrip.State.SHORT -> c.warning
        else -> c.fuel
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
        Text(d.date.dayOfWeek.name.take(1), style = Ruvo.type.caption, color = if (d.isToday) c.ink else c.inkFaint)
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.width(26.dp).height(64.dp)) {
            val corner = size.width / 2
            val box = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner, corner)) }
            drawPath(box, c.ink.copy(alpha = if (c.isDark) 0.1f else 0.07f))
            val h = size.height * animated
            if (h > 0f) {
                clipPath(box) {
                    drawRect(accent, topLeft = Offset(0f, size.height - h))
                }
            }
            if (d.state == DayStrip.State.SHORT) {
                // Hatching: the days Fuel does not cover.
                clipPath(box) {
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(accent.copy(alpha = 0.55f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.5.dp.toPx())
                        x += 6.dp.toPx()
                    }
                }
                drawPath(box, accent.copy(alpha = 0.7f), style = Stroke(width = 1.5.dp.toPx()))
            }
            if (d.isToday) drawPath(box, c.ink, style = Stroke(width = 2.dp.toPx()))
            if (d.state == DayStrip.State.INCOME) {
                // A four-point star on the income day.
                val cx = size.width / 2; val cy = size.height * 0.38f; val s = size.width * 0.28f
                val star = Path().apply {
                    moveTo(cx, cy - s); quadraticTo(cx, cy, cx + s, cy); quadraticTo(cx, cy, cx, cy + s)
                    quadraticTo(cx, cy, cx - s, cy); quadraticTo(cx, cy, cx, cy - s); close()
                }
                drawPath(star, Color.White.copy(alpha = 0.9f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            d.date.dayOfMonth.toString(), style = Ruvo.type.caption, textAlign = TextAlign.Center,
            color = if (d.isToday || d.state == DayStrip.State.INCOME) c.ink else c.inkFaint,
        )
    }
}

/**
 * The runway, drawn as one: a dashed airstrip from today to the next money, filled in amber for the days the
 * Fuel lasts. A paper plane glides out to where the Fuel ends and then floats there. Short of next money, the
 * plane stops before the flag and the gap is tinted rose; with days spare it sails past the flag.
 */
@Composable
private fun RunwayStrip(r: RunwayResult) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val toIncome = r.daysToNextIncome ?: return
    // The strip shows a little past next money, so spare days have somewhere to go.
    val span = maxOf(toIncome, r.runwayDays).toFloat() * 1.12f
    val fuelEnd = (r.runwayDays / span).coerceIn(0.04f, 1f)
    val flagAt = (toIncome / span).coerceIn(0f, 1f)
    val glide = remember { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(r.runwayDays) { if (!reduced) { glide.snapTo(0f); glide.animateTo(1f, androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) } }
    val loop = rememberInfiniteTransition(label = "plane")
    val bob by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")
    Column {
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val y = size.height * 0.62f
            val w = size.width
            val track = 8.dp.toPx()
            drawRoundRect(c.ink.copy(alpha = if (c.isDark) 0.14f else 0.08f), Offset(0f, y - track / 2), Size(w, track), androidx.compose.ui.geometry.CornerRadius(track / 2))
            val p = glide.value
            val fuelX = w * fuelEnd * p
            if (!r.makesIt) {
                drawRect(c.warning.copy(alpha = 0.28f), Offset(w * fuelEnd, y - track / 2), Size((w * (flagAt - fuelEnd)).coerceAtLeast(0f), track))
            }
            drawRoundRect(Brush.horizontalGradient(listOf(c.sun, c.fuel)), Offset(0f, y - track / 2), Size(fuelX.coerceAtLeast(track), track), androidx.compose.ui.geometry.CornerRadius(track / 2))
            // Dashed centre line, like runway markings.
            drawLine(Color.White.copy(alpha = 0.9f), Offset(track, y), Offset((fuelX - track).coerceAtLeast(track), y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            // The flag at next money, in brand blue: money coming in.
            val fx = w * flagAt
            drawLine(c.ink, Offset(fx, y + track / 2), Offset(fx, 2.dp.toPx()), 1.5.dp.toPx())
            drawPath(Path().apply { moveTo(fx, 2.dp.toPx()); lineTo(fx + 11.dp.toPx(), 6.dp.toPx()); lineTo(fx, 10.dp.toPx()); close() }, c.brandInk)
            // The paper plane, tilted up as if taking off, floating gently.
            val px = fuelX
            val py = y - 11.dp.toPx() + (if (reduced) 0f else bob) * 1.5.dp.toPx()
            val s = 9.dp.toPx()
            val plane = Path().apply {
                moveTo(px + s, py - s * 0.35f); lineTo(px - s, py - s * 0.15f); lineTo(px - s * 0.3f, py + s * 0.15f); close()
                moveTo(px + s, py - s * 0.35f); lineTo(px - s * 0.3f, py + s * 0.15f); lineTo(px - s * 0.2f, py + s * 0.65f); close()
            }
            drawPath(plane, c.ink)
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Today", style = Ruvo.type.caption, color = c.inkSoft, modifier = Modifier.weight(1f))
            Text(
                r.nextIncomeDate?.let { "Next money · ${IndianFormat.date(it, r.today)}" } ?: "Next money",
                style = Ruvo.type.caption, color = c.inkSoft,
            )
        }
    }
}
