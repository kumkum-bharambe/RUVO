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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruvo.design.Eyebrow
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.RollingNumber
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.engine.DayStrip
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.RunwayResult
import kotlin.math.PI
import kotlin.math.sin

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
 * The hero: the number big and wide, sitting in a card whose bottom is a slow liquid surface.
 * The liquid level follows how comfortable the buffer is; it ripples faster when money is tight.
 */
@Composable
fun RunwayHero(r: RunwayResult, onWhy: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val buffer = r.bufferDays
    val next = r.daysToNextIncome
    val tight = !r.makesIt || (buffer != null && buffer < 3)
    val health = when {
        r.unlimited -> 0.9f
        next == null -> (r.runwayDays / 30f).coerceIn(0.15f, 0.9f)
        next == 0 -> 0.8f
        else -> (r.runwayDays.toFloat() / next).coerceIn(0.12f, 1.2f) * 0.65f
    }
    val level by animateFloatAsState(health, Motion.soft(), label = "level")
    val wave = rememberInfiniteTransition(label = "wave")
    val phase by wave.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(if (tight) 2600 else 5200, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    val liquid = if (tight) c.warning else c.fuel
    val shape = RoundedCornerShape(Radius.card + 6.dp)

    Box(
        modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.hairline, shape)
            .clickable(role = Role.Button, onClickLabel = "Why this many days", onClick = onWhy)
            .semantics { contentDescription = runwaySpoken(r) },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val top = size.height * (1f - level.coerceIn(0f, 1f) * 0.62f)
            val amp = if (reduced) 0f else size.height * (if (tight) 0.035f else 0.02f)
            fun surface(offset: Float, alpha: Float) {
                val p = Path().apply {
                    moveTo(0f, size.height)
                    var x = 0f
                    while (x <= size.width) {
                        val y = top + amp * sin((x / size.width) * 2f * PI.toFloat() * 1.3f + phase + offset)
                        lineTo(x, y); x += 6f
                    }
                    lineTo(size.width, size.height); close()
                }
                drawPath(p, Brush.verticalGradient(listOf(liquid.copy(alpha = alpha), liquid.copy(alpha = alpha * 0.35f)), startY = top, endY = size.height))
            }
            surface(1.4f, if (c.isDark) 0.22f else 0.16f)
            surface(0f, if (c.isDark) 0.34f else 0.26f)
        }
        Column(Modifier.fillMaxWidth().padding(Space.xl).clearAndSetSemantics { }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Runway", color = c.inkSoft)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(Radius.chip)).background(c.canvas.copy(alpha = 0.7f))
                        .padding(horizontal = Space.sm, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Why?", style = Ruvo.type.label, color = c.ink)
                    Icon(RuvoIcons.Forward, null, Modifier.padding(start = 2.dp).width(14.dp), tint = c.ink)
                }
            }
            Spacer(Modifier.height(Space.xs))
            Row(verticalAlignment = Alignment.Bottom) {
                if (r.unlimited) {
                    Text("∞", style = Ruvo.type.hero, color = c.ink)
                } else {
                    RollingNumber(r.runwayDays, Ruvo.type.hero, c.ink)
                }
                Spacer(Modifier.width(Space.xs))
                Text(if (r.runwayDays == 1) "day" else "days", Modifier.padding(bottom = 18.dp), style = Ruvo.type.display, color = c.ink)
            }
            Text(runwaySubline(r), style = Ruvo.type.bodyStrong, color = if (r.makesIt) c.ink else c.warning)
            Spacer(Modifier.height(Space.xxs))
            Text("${IndianFormat.rupees(r.fuel.coerceAtLeast(0))} of Fuel at about ${IndianFormat.rupees(r.dailySpend)} a day", style = Ruvo.type.caption, color = c.inkSoft)
        }
    }
}

/**
 * The day strip: one liquid-filled pill per day to the next money. Covered days are full, the day money runs
 * out is part-filled, days after it are empty and outlined, and the income day glows.
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
        else -> runsOut?.let { "Fuel runs out around ${IndianFormat.date(it, r.today)}" } ?: ""
    }
    Column(modifier.semantics { contentDescription = caption }) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.gutter)) {
            itemsIndexed(days, key = { _, d -> d.date.toEpochDay() }) { i, d ->
                DayPill(d, DayStrip.fill(r, i))
            }
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
        DayStrip.State.INCOME -> c.sun
        DayStrip.State.SHORT -> c.warning
        else -> c.fuel
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
        Text(d.date.dayOfWeek.name.take(1), style = Ruvo.type.caption, color = if (d.isToday) c.ink else c.inkFaint)
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.width(30.dp).height(72.dp)) {
            val r = size.width / 2
            val shapePath = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, r, r))
            }
            drawPath(shapePath, c.hairline.copy(alpha = if (c.isDark) 0.6f else 0.9f))
            val h = size.height * animated
            if (h > 0f) {
                clipPath(shapePath) {
                    drawRect(
                        Brush.verticalGradient(listOf(accent.copy(alpha = 0.75f), accent), startY = size.height - h, endY = size.height),
                        topLeft = Offset(0f, size.height - h),
                    )
                }
            }
            if (d.state == DayStrip.State.SHORT) {
                drawPath(shapePath, accent.copy(alpha = 0.7f), style = Stroke(width = 2.dp.toPx()))
            }
            if (d.isToday) {
                drawPath(shapePath, c.ink, style = Stroke(width = 2.dp.toPx()))
            }
            if (d.state == DayStrip.State.INCOME) {
                drawCircle(Color.White.copy(alpha = 0.85f), radius = r * 0.32f, center = Offset(size.width / 2, size.height * 0.3f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            d.date.dayOfMonth.toString(), style = Ruvo.type.caption, textAlign = TextAlign.Center,
            color = when {
                d.state == DayStrip.State.INCOME -> c.ink
                d.isToday -> c.ink
                else -> c.inkFaint
            },
        )
    }
}
