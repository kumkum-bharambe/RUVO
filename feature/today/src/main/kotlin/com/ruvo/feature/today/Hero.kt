package com.ruvo.feature.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import com.ruvo.design.Haptics
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.RollingNumber
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.design.hardShadow
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
 * A ticket: small cut corners and two half-circle notches where the stub tears off, [stubHeight] from the bottom.
 */
class TicketShape(private val stubHeight: Dp, private val notch: Dp = 11.dp, private val corner: Dp = Radius.card) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { notch.toPx() }
        val cr = with(density) { corner.toPx() }
        val y = size.height - with(density) { stubHeight.toPx() }
        val body = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, cr, cr)) }
        val holes = Path().apply {
            addOval(Rect(Offset(0f, y), r))
            addOval(Rect(Offset(size.width, y), r))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, holes))
    }
}

private val STUB = 64.dp

/**
 * The hero is a runway ticket. Printed in ink (paper in Night swim), it carries the runway number huge in the
 * serif, a serial number, and a stub with the next money. A rubber stamp, "MAKES IT +4" or "3 DAYS SHORT", lands
 * on it a moment after it appears, with a firm haptic.
 */
@Composable
fun RunwayHero(r: RunwayResult, onWhy: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val view = LocalView.current
    val shape = TicketShape(STUB)
    val mood = Insights.mood(r)
    val short = !r.makesIt
    val stampInk = when {
        short -> c.warning
        (r.bufferDays ?: 99) < 3 -> c.fuelDeep
        else -> c.futureDeep
    }
    val stampText = when {
        r.unlimited -> "NO END\nIN SIGHT"
        r.daysToNextIncome == null -> "${r.runwayDays} DAYS\nAT YOUR PACE"
        short -> "${-(r.bufferDays ?: 0)} DAYS\nSHORT"
        else -> "MAKES IT\n+${r.spareDays} DAYS"
    }

    // The stamp lands: big and faint, then firmly down at a slight angle.
    val land = remember(stampText) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(stampText) {
        if (reduced) return@LaunchedEffect
        delay(520)
        land.animateTo(1f, Motion.stamp())
        Haptics.medium(view)
    }

    Box(
        modifier.fillMaxWidth()
            .hardShadow(shape, 6.dp, c.fuel)
            .clip(shape).background(c.heroBrush)
            .clickable(role = Role.Button, onClickLabel = "Why this many days", onClick = onWhy)
            .semantics { contentDescription = runwaySpoken(r) + ". " + mood.title },
    ) {
        Column(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            Column(Modifier.fillMaxWidth().padding(start = Space.xl, end = Space.xl, top = Space.lg, bottom = Space.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Ruvo · Runway pass", color = c.onHero.copy(alpha = 0.6f))
                    Spacer(Modifier.weight(1f))
                    Text("No. ${r.runwayDays.toString().padStart(4, '0')}", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.55f))
                }
                Spacer(Modifier.height(Space.xs))
                Box(Modifier.fillMaxWidth()) {
                    val heroStyle = if (r.runwayDays >= 100) Ruvo.type.hero.copy(fontSize = Ruvo.type.hero.fontSize * 0.76f) else Ruvo.type.hero
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (r.unlimited) Text("∞", style = heroStyle, color = c.onHero)
                        else RollingNumber(r.runwayDays, heroStyle, c.onHero)
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            if (r.runwayDays == 1) "day" else "days", Modifier.padding(bottom = 18.dp),
                            style = Ruvo.type.display.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = c.onHero,
                            maxLines = 1, softWrap = false,
                        )
                    }
                    Stamp(
                        stampText, stampInk,
                        Modifier.align(Alignment.TopEnd).padding(top = 6.dp).graphicsLayer {
                            val p = land.value
                            alpha = p.coerceIn(0f, 1f)
                            val s = 1f + (1f - p) * 1.2f
                            scaleX = s; scaleY = s
                            rotationZ = -11f + (1f - p) * -10f
                        },
                    )
                }
                Text(
                    "${IndianFormat.rupees(r.fuel.coerceAtLeast(0))} of Fuel at about ${IndianFormat.rupees(r.dailySpend)} a day",
                    style = Ruvo.type.label, color = c.onHero.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(Space.sm))
                Row(
                    Modifier.clip(RoundedCornerShape(Radius.chip)).border(1.dp, c.onHero.copy(alpha = 0.35f), RoundedCornerShape(Radius.chip))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).background(stampInk))
                    Spacer(Modifier.width(6.dp))
                    Text(mood.title, style = Ruvo.type.caption, color = c.onHero, maxLines = 1)
                    Icon(RuvoIcons.Forward, null, Modifier.padding(start = 2.dp).size(12.dp), tint = c.onHero)
                }
            }
            // Perforation where the stub tears off.
            Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp)) {
                drawLine(
                    c.onHero.copy(alpha = 0.35f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f)),
                )
            }
            Row(
                Modifier.fillMaxWidth().height(STUB).padding(horizontal = Space.xl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    val next = r.daysToNextIncome
                    Eyebrow(
                        when {
                            next == null -> "Income"
                            else -> "Next money" + (r.nextIncomeDate?.let { " · ${IndianFormat.date(it, r.today)}" } ?: "")
                        },
                        color = c.onHero.copy(alpha = 0.55f),
                        modifier = Modifier.semantics { },
                    )
                    Text(
                        when {
                            r.unlimited -> "No end at this pace"
                            r.incomeOverdue -> "Late"
                            next == null -> "No fixed date"
                            else -> "In ${IndianFormat.days(next)}"
                        },
                        style = Ruvo.type.bodyStrong, color = c.onHero, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Text("Why?", style = Ruvo.type.label, color = c.onHero.copy(alpha = 0.7f))
                Icon(RuvoIcons.Forward, null, Modifier.padding(start = 2.dp).size(14.dp), tint = c.onHero.copy(alpha = 0.7f))
            }
        }
    }
}

/** A rubber stamp: double-ruled box, two lines of spaced capitals, ink slightly uneven. */
@Composable
private fun Stamp(text: String, ink: Color, modifier: Modifier) {
    Box(
        modifier.border(2.dp, ink.copy(alpha = 0.9f), RoundedCornerShape(3.dp)).padding(3.dp)
            .border(1.dp, ink.copy(alpha = 0.6f), RoundedCornerShape(2.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ruvo.type.eyebrow.copy(fontSize = Ruvo.type.eyebrow.fontSize * 1.15f, lineHeight = Ruvo.type.eyebrow.lineHeight * 1.2f), color = ink, textAlign = TextAlign.Center)
    }
}

/**
 * The day strip: one punched square per day to the next money. Covered days are printed in haldi, the day money runs
 * out is part-filled, days after it are hatched in sindoor, and the income day carries a star. A fade at the edge
 * says there is more.
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
        Canvas(Modifier.width(28.dp).height(52.dp)) {
            val corner = 3.dp.toPx()
            val box = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner, corner)) }
            drawPath(box, c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f))
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
