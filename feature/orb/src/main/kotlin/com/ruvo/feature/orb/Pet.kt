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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ruvo.design.Ruvo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The pet's colours and eyes follow the money: happy, calm, worried, or thinking while Ruvo answers. */
enum class PetMood { HAPPY, CALM, WORRIED, THINKING }

/** Happy when the runway is comfortable, calm when the cushion is thin, worried when money is short. */
fun OrbMood.pet(): PetMood = when {
    energy >= 0.9f -> PetMood.WORRIED
    energy >= 0.5f -> PetMood.CALM
    else -> PetMood.HAPPY
}

/** Four liquid colours that swirl inside the sphere, deepest first. */
private fun swirlFor(m: PetMood): List<Color> = when (m) {
    PetMood.HAPPY -> listOf(Color(0xFFFF7A3D), Color(0xFFFFC23D), Color(0xFFFF6FA8), Color(0xFF8B6CFF))
    PetMood.CALM -> listOf(Color(0xFF5B3FE0), Color(0xFF3FA9FF), Color(0xFFFF6FA8), Color(0xFFB79CFF))
    PetMood.WORRIED -> listOf(Color(0xFFE5402F), Color(0xFFFF8A45), Color(0xFFFF5C8A), Color(0xFFFFC23D))
    PetMood.THINKING -> listOf(Color(0xFF3B2BB8), Color(0xFF8B6CFF), Color(0xFF3FD0FF), Color(0xFFFF8FC8))
}

/** A fixed pseudo-random number in 0..1 for sparkle [i], so the ring looks scattered but never flickers. */
private fun seed(i: Int): Float {
    val x = sin(i * 12.9898f) * 43758.547f
    return x - kotlin.math.floor(x)
}

/**
 * Ruvo's pet: a glossy liquid orb. Colours swirl slowly inside a glassy sphere, it wobbles like jelly as it
 * breathes, two white pill eyes blink and glance about, and a faint ring of sparkles drifts round it.
 * Drawn on a 100 x 100 unit artboard. Every loop is an infinite transition so tests can go idle; reduced
 * motion holds it still.
 */
@Composable
fun RuvoPet(mood: PetMood, modifier: Modifier = Modifier) {
    val reduced = Ruvo.reducedMotion
    val loop = rememberInfiniteTransition(label = "pet")
    val swirl by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "swirl")
    val breath by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "breath")
    val blink by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4300, easing = LinearEasing)), label = "blink")
    val look by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "look")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "drift")
    Canvas(modifier.clearAndSetSemantics { }) {
        val unit = minOf(size.width, size.height) / 100f
        translate((size.width - 100f * unit) / 2f, (size.height - 100f * unit) / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPet(
                    mood,
                    swirl = if (reduced) 0.15f else swirl,
                    breath = if (reduced) 0f else sin(2 * PI * breath).toFloat(),
                    eyeOpen = if (reduced) 1f else blinkOpen(blink),
                    look = if (reduced) 0f else lookAround(look),
                    drift = if (reduced) 0f else drift,
                )
            }
        }
    }
}

/** Eyes stay open most of the loop and close briefly, twice in a row now and then. */
private fun blinkOpen(t: Float): Float {
    fun dip(at: Float) = (1f - kotlin.math.abs(t - at) / 0.014f).coerceIn(0f, 1f)
    return 1f - maxOf(dip(0.62f), dip(0.69f)) * 0.92f
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

private fun DrawScope.drawPet(mood: PetMood, swirl: Float, breath: Float, eyeOpen: Float, look: Float, drift: Float) {
    val colours = swirlFor(mood)
    val c = Offset(50f, 50f)
    val r = 36f
    val tau = 2f * PI.toFloat()

    // Sparkle ring: tiny warm specks orbiting slowly, each twinkling on its own beat.
    for (i in 0 until 26) {
        val a = (i / 26f + drift) * tau + seed(i) * 0.5f
        val dist = r + 6f + seed(i + 40) * 8f
        val tw = 0.25f + 0.75f * ((sin((drift * 6f + seed(i + 80)) * tau) + 1f) / 2f)
        drawCircle(Color(0xFFFFE3A3).copy(alpha = 0.55f * tw), 0.5f + seed(i + 120) * 0.9f, c + Offset(cos(a) * dist, sin(a) * dist))
    }

    // Soft glow and a fine dark rim, so the sphere reads on light and dark backgrounds alike.
    drawCircle(Brush.radialGradient(listOf(colours[1].copy(alpha = 0.35f), Color.Transparent), c, r * 1.45f), r * 1.45f, c)

    // Jelly wobble: a little wider as it breathes out, a little taller as it breathes in.
    scale(1f + breath * 0.025f, 1f - breath * 0.025f, pivot = Offset(50f, 86f)) {
        drawCircle(Color(0xFF0B0D24).copy(alpha = 0.55f), r + 1.6f, c)
        val sphere = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(sphere) {
            drawCircle(colours[0], r, c)
            // Liquid: three soft blobs of colour circling at different speeds and depths.
            for (k in 1..3) {
                val speed = when (k) { 1 -> 1f; 2 -> -1.6f; else -> 2.3f }
                val a = (swirl * speed + k * 0.31f) * tau
                val orbit = r * (0.42f - k * 0.05f)
                val centre = c + Offset(cos(a) * orbit, sin(a * 0.8f) * orbit)
                val blob = r * (1.05f - k * 0.12f)
                drawCircle(Brush.radialGradient(listOf(colours[k], colours[k].copy(alpha = 0f)), centre, blob), blob, centre)
            }
            // A light ribbon sweeping through, like a fold in the liquid.
            val ribbon = Path().apply {
                val y = 50f + sin(swirl * tau) * 6f
                moveTo(c.x - r, y + 10f)
                cubicTo(c.x - 10f, y - 14f, c.x + 8f, y + 22f, c.x + r, y - 6f)
            }
            drawPath(ribbon, Color.White.copy(alpha = 0.22f), style = Stroke(9f))
            // Glass: a broad sheen top left, a bright glint, and a warm rim light bottom right.
            drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(36f, 30f), 22f), Offset(16f, 14f), Size(40f, 30f))
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.White.copy(alpha = 0.28f)), c, r), r, c)
        }
        drawCircle(Color.White.copy(alpha = 0.85f), 2.2f, Offset(33f, 27f))
        drawCircle(Color.White.copy(alpha = 0.5f), 1.1f, Offset(39f, 24f))
    }

    // Eyes: two white pills. They glance about, lift while thinking, lean in when worried, and squash to blink.
    val lookX = look * 3f
    val eyeY = when (mood) { PetMood.THINKING -> 44f; else -> 49f } + breath * 0.6f
    val h = (17f * eyeOpen).coerceAtLeast(2.6f) * if (mood == PetMood.HAPPY) 0.92f else 1f
    val w = 8f
    listOf(41.5f, 58.5f).forEachIndexed { i, ex ->
        val lean = if (mood == PetMood.WORRIED) (if (i == 0) 6f else -6f) else 0f
        rotate(lean, Offset(ex + lookX, eyeY)) {
            drawRoundRect(
                Color.White, Offset(ex + lookX - w / 2f, eyeY - h / 2f), Size(w, h), CornerRadius(w / 2f),
            )
        }
    }
    if (mood == PetMood.HAPPY && eyeOpen > 0.5f) {
        // Happy cheeks: two faint warm dots under the eyes.
        drawCircle(Color(0xFFFFE0F0).copy(alpha = 0.45f), 3.2f, Offset(35f, 61f))
        drawCircle(Color(0xFFFFE0F0).copy(alpha = 0.45f), 3.2f, Offset(65f, 61f))
    }
}
