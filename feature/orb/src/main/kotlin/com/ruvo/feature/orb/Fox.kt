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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ruvo.design.Ruvo
import kotlin.math.PI
import kotlin.math.sin

/** The fox's face follows the money: happy, calm, worried, or thinking while Ruvo answers. */
enum class FoxMood { HAPPY, CALM, WORRIED, THINKING }

/** Happy when the runway is comfortable, calm when the cushion is thin, worried when money is short. */
fun OrbMood.fox(): FoxMood = when {
    energy >= 0.9f -> FoxMood.WORRIED
    energy >= 0.5f -> FoxMood.CALM
    else -> FoxMood.HAPPY
}

private val Ink = Color(0xFF16161A)
private val Cream = Color(0xFFFFF4DC)
private val InnerEar = Color(0xFF3B2A08)
private val Mouth = Color(0xFF5A1F2B)
private val Blush = Color(0xFFFF8FA3)

private fun gradientFor(m: FoxMood) = when (m) {
    FoxMood.WORRIED -> listOf(Color(0xFFFFB070), Color(0xFFF26B1D), Color(0xFFC94F0A))
    FoxMood.CALM, FoxMood.THINKING -> listOf(Color(0xFFBFDFFB), Color(0xFF4A9BEA), Color(0xFF2F7FD0))
    FoxMood.HAPPY -> listOf(Color(0xFFFFD768), Color(0xFFFDB913), Color(0xFFE8960A))
}

/**
 * Ruvo's fox, drawn natively from the Ruvo 7 design (a 116 x 112 unit artboard). It bobs, wags its tail, twitches
 * an ear, blinks, and sparkles when things are good. Every loop is an infinite transition, so tests can go idle,
 * and reduced motion holds it still.
 */
@Composable
fun FoxPet(mood: FoxMood, modifier: Modifier = Modifier) {
    val reduced = Ruvo.reducedMotion
    val loop = rememberInfiniteTransition(label = "fox")
    val bob by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "bob")
    val tail by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "tail")
    val ear by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "ear")
    val blink by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "blink")
    val spark by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "spark")
    Canvas(modifier.clearAndSetSemantics { }) {
        // Map the artboard (x -8..108, y -4..108) onto the canvas, keeping its proportions.
        val unit = minOf(size.width / 116f, size.height / 112f)
        translate((size.width - 116f * unit) / 2f + 8f * unit, (size.height - 112f * unit) / 2f + 4f * unit) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawFox(
                    mood,
                    bob = if (reduced) 0f else -4f * (0.5f - 0.5f * kotlin.math.cos(2 * PI * bob).toFloat()),
                    tail = if (reduced) 2f else 2f + 12f * sin(2 * PI * tail).toFloat(),
                    ear = if (reduced) 0f else earTwitch(ear),
                    eyeOpen = if (reduced) 1f else blinkOpen(blink),
                    spark = if (reduced) 0.8f else 0.6f + 0.5f * (0.5f - 0.5f * kotlin.math.cos(2 * PI * spark).toFloat()),
                )
            }
        }
    }
}

/** Ear twitch: still for most of the loop, then a quick flick back and forth. */
private fun earTwitch(t: Float): Float = when {
    t < 0.88f -> 0f
    t < 0.92f -> -12f * (t - 0.88f) / 0.04f
    t < 0.96f -> -12f + 18f * (t - 0.92f) / 0.04f
    else -> 6f * (1f - (t - 0.96f) / 0.04f)
}

/** Blink: eyes open, then closed for a moment near the end of each loop. */
private fun blinkOpen(t: Float): Float = when {
    t < 0.9f -> 1f
    t < 0.94f -> 1f - 0.92f * (t - 0.9f) / 0.04f
    t < 0.98f -> 0.08f + 0.92f * (t - 0.94f) / 0.04f
    else -> 1f
}

private fun DrawScope.drawFox(mood: FoxMood, bob: Float, tail: Float, ear: Float, eyeOpen: Float, spark: Float) {
    val g = gradientFor(mood)
    val fur = Brush.radialGradient(listOf(g[0], g[1], g[2]), center = Offset(8f + 0.35f * 100f, 0f + 0.28f * 100f), radius = 95f)

    // Ground shadow stays put while the body bobs.
    drawOval(Color.Black.copy(alpha = 0.2f), Offset(20f, 98f), Size(60f, 8f))

    translate(0f, bob) {
        // Tail, wagging from its root.
        rotate(tail, Offset(78f, 90f)) {
            drawPath(Path().apply { moveTo(70f, 92f); quadraticTo(104f, 94f, 98f, 58f); quadraticTo(92f, 76f, 74f, 80f); close() }, fur)
            drawPath(Path().apply { moveTo(98f, 58f); quadraticTo(101f, 70f, 96f, 78f); quadraticTo(92f, 68f, 98f, 58f); close() }, Color.White)
        }
        // Body and belly.
        drawPath(Path().apply { moveTo(30f, 78f); quadraticTo(50f, 70f, 70f, 78f); lineTo(74f, 98f); quadraticTo(50f, 106f, 26f, 98f); close() }, fur)
        drawPath(Path().apply { moveTo(42f, 84f); quadraticTo(50f, 80f, 58f, 84f); lineTo(58f, 100f); quadraticTo(50f, 104f, 42f, 100f); close() }, Cream)
        // Left ear.
        drawPath(Path().apply { moveTo(12f, 48f); lineTo(16f, 8f); lineTo(42f, 28f); close() }, fur)
        drawPath(Path().apply { moveTo(19f, 38f); lineTo(21f, 19f); lineTo(34f, 29f); close() }, InnerEar)
        // Right ear, which twitches.
        rotate(ear, Offset(72f, 30f)) {
            drawPath(Path().apply { moveTo(88f, 48f); lineTo(84f, 8f); lineTo(58f, 28f); close() }, fur)
            drawPath(Path().apply { moveTo(81f, 38f); lineTo(79f, 19f); lineTo(66f, 29f); close() }, InnerEar)
        }
        // Head.
        drawPath(Path().apply { moveTo(8f, 60f); quadraticTo(8f, 30f, 50f, 30f); quadraticTo(92f, 30f, 92f, 60f); quadraticTo(92f, 88f, 50f, 90f); quadraticTo(8f, 88f, 8f, 60f); close() }, fur)
        // White cheeks.
        drawPath(Path().apply { moveTo(8f, 62f); quadraticTo(22f, 66f, 34f, 76f); quadraticTo(44f, 86f, 50f, 90f); quadraticTo(30f, 90f, 8f, 62f); close() }, Cream)
        drawPath(Path().apply { moveTo(92f, 62f); quadraticTo(78f, 66f, 66f, 76f); quadraticTo(56f, 86f, 50f, 90f); quadraticTo(70f, 90f, 92f, 62f); close() }, Cream)
        // Shine on the forehead.
        drawPath(Path().apply { moveTo(22f, 42f); quadraticTo(29f, 34f, 42f, 34f) }, Color.White.copy(alpha = 0.5f), style = Stroke(3.5f, cap = StrokeCap.Round))

        // Eyes: look up when thinking, smaller pupils when worried.
        val px = if (mood == FoxMood.THINKING) 2.4f else 0f
        val py = when (mood) { FoxMood.THINKING -> -2.4f; FoxMood.WORRIED -> 1.2f; else -> 0.4f }
        val pupil = if (mood == FoxMood.WORRIED) 3.7f else 4.8f
        listOf(34f, 66f).forEach { x ->
            scale(1f, eyeOpen.coerceIn(0.08f, 1f), pivot = Offset(x, 56f)) {
                drawOval(Color.White, Offset(x - 7.6f, 56f - 8.8f), Size(15.2f, 17.6f))
                drawCircle(Ink, pupil, Offset(x + px, 56f + py))
                drawCircle(Color.White, 1.6f, Offset(x + px + 1.6f, 54f + py))
            }
        }
        // Brows for worry and thought.
        val brow = Stroke(3f, cap = StrokeCap.Round)
        if (mood == FoxMood.WORRIED) {
            drawLine(Ink, Offset(25f, 43f), Offset(38f, 48f), 3f, StrokeCap.Round)
            drawLine(Ink, Offset(75f, 43f), Offset(62f, 48f), 3f, StrokeCap.Round)
        } else if (mood == FoxMood.THINKING) {
            drawLine(Ink, Offset(57f, 44f), Offset(71f, 40f), 3f, StrokeCap.Round)
        }
        // Nose.
        drawOval(Ink, Offset(45f, 64.4f), Size(10f, 7.2f))
        drawLine(Ink, Offset(50f, 71.5f), Offset(50f, 74.5f), 2.4f)
        // Mouth.
        when (mood) {
            FoxMood.HAPPY -> drawPath(Path().apply { moveTo(42f, 76f); quadraticTo(50f, 85f, 58f, 76f); close() }, Mouth)
            FoxMood.CALM -> drawPath(Path().apply { moveTo(43f, 76f); quadraticTo(50f, 81f, 57f, 76f) }, Ink, style = brow)
            FoxMood.WORRIED -> drawPath(Path().apply { moveTo(42f, 79f); quadraticTo(46f, 74f, 50f, 79f); quadraticTo(54f, 84f, 58f, 79f) }, Ink, style = brow)
            FoxMood.THINKING -> drawLine(Ink, Offset(44f, 77f), Offset(56f, 77f), 3f, StrokeCap.Round)
        }
        // Blush.
        drawCircle(Blush.copy(alpha = 0.45f), 5f, Offset(20f, 70f))
        drawCircle(Blush.copy(alpha = 0.45f), 5f, Offset(80f, 70f))
    }

    // Extras outside the bob: a sparkle when happy, a thought bubble, a sweat drop.
    when (mood) {
        FoxMood.HAPPY -> scale(spark, spark, pivot = Offset(94f, 26f)) {
            drawPath(
                Path().apply {
                    moveTo(94f, 19f); lineTo(96f, 24f); lineTo(101f, 26f); lineTo(96f, 28f); lineTo(94f, 33f)
                    lineTo(92f, 28f); lineTo(87f, 26f); lineTo(92f, 24f); close()
                },
                Color(0xFFFFF3B0).copy(alpha = spark.coerceIn(0f, 1f)),
            )
        }
        FoxMood.THINKING -> {
            drawCircle(Color(0xFFEDEDF0), 9f, Offset(92f, 22f))
            drawCircle(Ink, 9f, Offset(92f, 22f), style = Stroke(1.6f))
            drawLine(Ink, Offset(92f, 25f), Offset(92f, 25.5f), 2.6f, StrokeCap.Round)
            drawPath(Path().apply { moveTo(88.5f, 18.5f); quadraticTo(92f, 14f, 95.5f, 18.5f); quadraticTo(95.5f, 21f, 92f, 22f) }, Ink, style = Stroke(2f, cap = StrokeCap.Round))
        }
        FoxMood.WORRIED -> drawPath(Path().apply { moveTo(88f, 42f); quadraticTo(93f, 50f, 88f, 53f); quadraticTo(83f, 50f, 88f, 42f); close() }, Color(0xFFECECF0))
        FoxMood.CALM -> {}
    }
}
