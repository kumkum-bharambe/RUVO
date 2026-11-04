package com.ruvo.feature.orb

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ruvo.design.Ruvo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The pet's face follows the money: happy, calm, worried, or thinking while Ruvo answers. */
enum class PetMood { HAPPY, CALM, WORRIED, THINKING }

/** Happy when the runway is comfortable, calm when the cushion is thin, worried when money is short. */
fun OrbMood.pet(): PetMood = when {
    energy >= 0.9f -> PetMood.WORRIED
    energy >= 0.5f -> PetMood.CALM
    else -> PetMood.HAPPY
}

private class Fur(val light: Color, val mid: Color, val deep: Color)

private fun furFor(m: PetMood) = when (m) {
    PetMood.HAPPY -> Fur(Color(0xFFFFE28A), Color(0xFFFFC23D), Color(0xFFFF9A3D))
    PetMood.CALM -> Fur(Color(0xFFCDBFFF), Color(0xFF9C82FF), Color(0xFF6A4BF0))
    PetMood.WORRIED -> Fur(Color(0xFFFFC08F), Color(0xFFFF8A45), Color(0xFFE5571A))
    PetMood.THINKING -> Fur(Color(0xFFC3B8FF), Color(0xFF8A7BFF), Color(0xFF4F3FD0))
}

private val EyeInk = Color(0xFF141B4D)
private val Mouth = Color(0xFF3A1640)
private val Tongue = Color(0xFFFF7A9C)
private val Blush = Color(0xFFFF8FB0)
private val Glow = Color(0xFFFFD86B)

/** A fixed pseudo-random number in 0..1 for strand [i], so the fur looks shaggy but never flickers. */
private fun strand(i: Int): Float {
    val x = sin(i * 12.9898f) * 43758.547f
    return x - kotlin.math.floor(x)
}

/**
 * Ruvo's pet: a round, fuzzy creature with big eyes and a little antenna tipped with a coin-light. It breathes,
 * its fur ruffles, it blinks and glances about, and the coin-light glows brighter when money is comfortable.
 * Drawn on a 100 x 100 unit artboard. Every loop is an infinite transition so tests can go idle; reduced
 * motion holds it still.
 */
@Composable
fun RuvoPet(mood: PetMood, modifier: Modifier = Modifier) {
    val reduced = Ruvo.reducedMotion
    val loop = rememberInfiniteTransition(label = "pet")
    val breath by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "breath")
    val ruffle by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = LinearEasing)), label = "ruffle")
    val blink by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4300, easing = LinearEasing)), label = "blink")
    val look by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "look")
    Canvas(modifier.clearAndSetSemantics { }) {
        val unit = minOf(size.width, size.height) / 100f
        translate((size.width - 100f * unit) / 2f, (size.height - 100f * unit) / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPet(
                    mood,
                    breath = if (reduced) 0f else sin(2 * PI * breath).toFloat(),
                    ruffle = if (reduced) 0f else ruffle,
                    eyeOpen = if (reduced) 1f else blinkOpen(blink),
                    look = if (reduced) 0f else lookAround(look),
                )
            }
        }
    }
}

/** Eyes stay open most of the loop and close briefly, twice in a row now and then. */
private fun blinkOpen(t: Float): Float {
    fun dip(at: Float) = (1f - kotlin.math.abs(t - at) / 0.014f).coerceIn(0f, 1f)
    return 1f - maxOf(dip(0.62f), dip(0.69f)) * 0.95f
}

/** Glances left, back to centre, right, back: mostly still, moving in short hops. */
private fun lookAround(t: Float): Float = when {
    t < 0.2f -> 0f
    t < 0.3f -> -((t - 0.2f) / 0.1f)
    t < 0.45f -> -1f
    t < 0.55f -> -1f + (t - 0.45f) / 0.1f
    t < 0.7f -> 0f
    t < 0.8f -> (t - 0.7f) / 0.1f
    t < 0.9f -> 1f
    else -> 1f - (t - 0.9f) / 0.1f
}

private fun DrawScope.drawPet(mood: PetMood, breath: Float, ruffle: Float, eyeOpen: Float, look: Float) {
    val fur = furFor(mood)
    val cx = 50f
    val cy = 56f + breath * 0.8f
    val r = 31f
    val squash = 1f + breath * 0.025f

    // Feet first, so the body sits on them.
    drawOval(fur.deep, Offset(33f, 84f), Size(14f, 8f))
    drawOval(fur.deep, Offset(53f, 84f), Size(14f, 8f))

    // Antenna with its coin-light: brighter and with a halo when things are comfortable.
    val sway = breath * 2.2f + look * 1.5f
    val tip = Offset(cx + 6f + sway, cy - r - 17f)
    val stalk = Path().apply { moveTo(cx, cy - r + 2f); quadraticTo(cx + 1f, cy - r - 8f, tip.x, tip.y) }
    drawPath(stalk, fur.deep, style = Stroke(2.6f, cap = StrokeCap.Round))
    val halo = when (mood) { PetMood.HAPPY -> 1f; PetMood.CALM, PetMood.THINKING -> 0.6f; PetMood.WORRIED -> 0.35f }
    drawCircle(Brush.radialGradient(listOf(Glow.copy(alpha = 0.55f * halo), Color.Transparent), tip, 11f), 11f, tip)
    drawCircle(Glow, 4.6f, tip)
    drawCircle(Color.White.copy(alpha = 0.7f), 1.5f, tip + Offset(-1.4f, -1.4f))

    // Fur: short strands all round the edge, each with its own length, ruffling in a slow wave.
    scale(1f / squash, squash, pivot = Offset(cx, 90f)) {
        // A soft halo fills the gaps between strands so the edge reads as fluff, not spikes.
        drawCircle(fur.mid.copy(alpha = 0.85f), r + 2.5f, Offset(cx, cy))
        val strands = 170
        for (i in 0 until strands) {
            val a = (i.toFloat() / strands) * 2f * PI.toFloat()
            val len = 2.5f + strand(i) * 3f - if (sin(a) > 0.6f) 1.5f else 0f
            val wiggle = 0.06f * sin(ruffle * 2f * PI.toFloat() + i * 0.45f)
            val base = Offset(cx + cos(a) * (r - 3f), cy + sin(a) * (r - 3f))
            val tipA = a + wiggle
            val end = Offset(cx + cos(tipA) * (r + len), cy + sin(tipA) * (r + len))
            drawLine(if (i % 3 == 0) fur.light else fur.mid, base, end, 4.6f, StrokeCap.Round)
        }
        // The body itself, lit from the top left.
        drawCircle(Brush.radialGradient(listOf(fur.light, fur.mid, fur.deep), Offset(cx - 12f, cy - 14f), r * 1.7f), r, Offset(cx, cy))
        // A few strands drawn over the body edge so it reads as fluff, not a circle.
        for (i in 0 until 40) {
            val a = (i.toFloat() / 40) * 2f * PI.toFloat() + 0.07f
            val base = Offset(cx + cos(a) * (r - 7f), cy + sin(a) * (r - 7f))
            val end = Offset(cx + cos(a) * (r - 1f), cy + sin(a) * (r - 1f))
            drawLine(fur.light.copy(alpha = 0.5f), base, end, 2f, StrokeCap.Round)
        }
    }

    // Cheeks.
    drawOval(Blush.copy(alpha = 0.45f), Offset(cx - 25f, cy + 2f), Size(10f, 6f))
    drawOval(Blush.copy(alpha = 0.45f), Offset(cx + 15f, cy + 2f), Size(10f, 6f))

    // Eyes: big whites, dark pupils that glance about (and up while thinking), a bright catchlight.
    val eyeY = cy - 7f
    val lookX = look * 2.4f
    val lookY = if (mood == PetMood.THINKING) -3f else 0.5f
    listOf(cx - 11f, cx + 11f).forEach { ex ->
        if (eyeOpen < 0.35f) {
            // Mid-blink: a soft closed-eye curve, not a slit.
            val lid = Path().apply { moveTo(ex - 6.5f, eyeY); quadraticTo(ex, eyeY + 4.5f, ex + 6.5f, eyeY) }
            drawPath(lid, EyeInk, style = Stroke(2.4f, cap = StrokeCap.Round))
            return@forEach
        }
        val h = 19f * eyeOpen
        drawOval(Color.White, Offset(ex - 8f, eyeY - h / 2f), Size(16f, h))
        run {
            val p = Offset(ex + lookX, eyeY + lookY + 1f)
            drawCircle(EyeInk, 5.6f, p)
            drawCircle(Color.White, 1.9f, p + Offset(-1.8f, -2.2f))
        }
    }
    if (mood == PetMood.WORRIED) {
        drawLine(EyeInk, Offset(cx - 17f, eyeY - 13f), Offset(cx - 7f, eyeY - 15.5f), 2.2f, StrokeCap.Round)
        drawLine(EyeInk, Offset(cx + 17f, eyeY - 13f), Offset(cx + 7f, eyeY - 15.5f), 2.2f, StrokeCap.Round)
    }

    // Mouth: open smile, gentle smile, a wobble, or a small "o" while thinking.
    val my = cy + 8f
    when (mood) {
        PetMood.HAPPY -> {
            val m = Path().apply { moveTo(cx - 7f, my); quadraticTo(cx, my + 10f, cx + 7f, my); close() }
            drawPath(m, Mouth)
            drawOval(Tongue, Offset(cx - 3.5f, my + 3.2f), Size(7f, 3.6f))
        }
        PetMood.CALM -> {
            val m = Path().apply { moveTo(cx - 5f, my + 1f); quadraticTo(cx, my + 5.5f, cx + 5f, my + 1f) }
            drawPath(m, Mouth, style = Stroke(2.3f, cap = StrokeCap.Round))
        }
        PetMood.WORRIED -> {
            val m = Path().apply {
                moveTo(cx - 6f, my + 3f); quadraticTo(cx - 3f, my + 0.5f, cx, my + 3f); quadraticTo(cx + 3f, my + 5.5f, cx + 6f, my + 3f)
            }
            drawPath(m, Mouth, style = Stroke(2.2f, cap = StrokeCap.Round))
        }
        PetMood.THINKING -> drawOval(Mouth, Offset(cx + 1f, my + 1f), Size(5f, 5.5f))
    }
}
