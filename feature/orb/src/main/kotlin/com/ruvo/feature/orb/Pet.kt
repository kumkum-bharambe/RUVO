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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ruvo.design.Ruvo
import kotlin.math.PI
import kotlin.math.abs
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
    PetMood.CALM -> listOf(Color(0xFF3550D6), Color(0xFF3FA9FF), Color(0xFFFF6FA8), Color(0xFFB79CFF))
    PetMood.WORRIED -> listOf(Color(0xFFE5402F), Color(0xFFFF8A45), Color(0xFFFF5C8A), Color(0xFFFFC23D))
    PetMood.THINKING -> listOf(Color(0xFF2338A8), Color(0xFF8B6CFF), Color(0xFF3FD0FF), Color(0xFFFF8FC8))
}

/** A fixed pseudo-random number in 0..1 for sparkle [i], so the ring looks scattered but never flickers. */
private fun seed(i: Int): Float {
    val x = sin(i * 12.9898f) * 43758.547f
    return x - kotlin.math.floor(x)
}

/** The little tricks the pet does on its own, now and then. */
private enum class Trick { HOP, WINK, WIGGLE, COIN, SPIN }

/** Where each trick sits on the 28 s idle loop, as start and end fractions. Long quiet gaps between them. */
private val TRICKS = listOf(
    Trick.HOP to (0.02f to 0.055f),
    Trick.WINK to (0.21f to 0.26f),
    Trick.WIGGLE to (0.40f to 0.48f),
    Trick.COIN to (0.60f to 0.68f),
    Trick.SPIN to (0.81f to 0.86f),
)

/** The trick playing at loop time [t] and how far through it is (0..1), or null between tricks. */
private fun trickAt(t: Float): Pair<Trick, Float>? {
    for ((trick, w) in TRICKS) if (t >= w.first && t < w.second) return trick to (t - w.first) / (w.second - w.first)
    return null
}

/** Everything that shapes one frame of the pet. */
private class Pose(
    val mood: PetMood,
    val swirl: Float,
    val breath: Float,
    val eyeOpen: Float,
    val look: Float,
    val drift: Float,
    val joy: Float,
    val spin: Float,
    val pull: Float,
    val lean: Float,
    val trick: Trick?,
    val p: Float,
)

/**
 * Ruvo's pet: a glossy liquid orb with a face. Colours swirl inside a glassy sphere, it wobbles like jelly as it
 * breathes, its white pill eyes blink and glance about, and a faint ring of sparkles drifts round it.
 *
 * Every half minute or so it does a little trick on its own: a hop, a wink, a wiggle, tossing a tiny rupee coin, a
 * spin. It also reacts: [joy] (after a tap or a pat) gives happy eyes, a big smile and floating hearts; [spin]
 * plays a full turn; [pull] (being dragged down) makes it surprised; [lean] (the phone's tilt) moves its eyes.
 * Drawn on a 100 x 100 unit artboard. Every loop is an infinite transition, so tests can go idle; reduced motion
 * holds it still and skips the tricks.
 */
@Composable
fun RuvoPet(
    mood: PetMood,
    modifier: Modifier = Modifier,
    joy: Float = 0f,
    spin: Float = 0f,
    pull: Float = 0f,
    lean: Float = 0f,
    tricks: Boolean = true,
) {
    val reduced = Ruvo.reducedMotion
    val loop = rememberInfiniteTransition(label = "pet")
    val swirl by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "swirl")
    val breath by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "breath")
    val blink by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4300, easing = LinearEasing)), label = "blink")
    val look by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "look")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "drift")
    val cycle by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(28_000, easing = LinearEasing)), label = "tricks")
    Canvas(modifier.clearAndSetSemantics { }) {
        val unit = minOf(size.width, size.height) / 100f
        val playing = if (reduced || !tricks || joy > 0.05f || spin > 0f || pull > 0.05f) null else trickAt(cycle)
        translate((size.width - 100f * unit) / 2f, (size.height - 100f * unit) / 2f) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPet(
                    Pose(
                        mood,
                        swirl = if (reduced) 0.15f else swirl,
                        breath = if (reduced) 0f else sin(2 * PI * breath).toFloat(),
                        eyeOpen = if (reduced) 1f else blinkOpen(blink),
                        look = if (reduced) 0f else lookAround(look),
                        drift = if (reduced) 0f else drift,
                        joy = joy, spin = spin, pull = pull, lean = if (reduced) 0f else lean,
                        trick = playing?.first, p = playing?.second ?: 0f,
                    ),
                )
            }
        }
    }
}

/** Eyes stay open most of the loop and close briefly, twice in a row now and then. */
private fun blinkOpen(t: Float): Float {
    fun dip(at: Float) = (1f - abs(t - at) / 0.014f).coerceIn(0f, 1f)
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

/** Rises and falls once over 0..1, with a soft start and end. */
private fun arc(p: Float) = sin(PI.toFloat() * p.coerceIn(0f, 1f))

private fun DrawScope.drawPet(o: Pose) {
    val colours = swirlFor(o.mood)
    val c = Offset(50f, 50f)
    val r = 36f
    val tau = 2f * PI.toFloat()
    val p = o.p

    // Whole-body motion for the tricks and reactions.
    var lift = 0f
    var sx = 1f
    var sy = 1f
    var tilt = 0f
    when (o.trick) {
        Trick.HOP -> {
            lift = -18f * arc(p)
            // Squash on take-off and landing, stretch in the air.
            val squash = when { p < 0.14f -> arc(p / 0.14f); p > 0.86f -> arc((1f - p) / 0.14f); else -> 0f }
            sx = 1f + 0.13f * squash - 0.05f * arc(p)
            sy = 1f - 0.13f * squash + 0.07f * arc(p)
        }
        Trick.WIGGLE -> tilt = sin(p * 6f * PI.toFloat()) * 13f * (1f - p * 0.6f)
        Trick.SPIN -> { sx = 1f - 0.08f * arc(p); lift = -5f * arc(p) }
        Trick.COIN -> lift = 2f * arc(p * 2f)
        else -> {}
    }
    if (o.spin > 0f) { sx *= 1f - 0.1f * arc(o.spin); lift -= 10f * arc(o.spin) }
    if (o.joy > 0f) { val j = arc(1f - o.joy) * o.joy; sx *= 1f + 0.08f * j; sy *= 1f + 0.08f * j }

    // Sparkle ring: tiny warm specks orbiting slowly, each twinkling on its own beat.
    for (i in 0 until 26) {
        val a = (i / 26f + o.drift) * tau + seed(i) * 0.5f
        val dist = r + 6f + seed(i + 40) * 8f
        val tw = 0.25f + 0.75f * ((sin((o.drift * 6f + seed(i + 80)) * tau) + 1f) / 2f)
        drawCircle(Color(0xFFFFE3A3).copy(alpha = 0.55f * tw), 0.5f + seed(i + 120) * 0.9f, c + Offset(cos(a) * dist, sin(a) * dist))
    }
    // A soft shadow on the ground that shrinks as it rises.
    val rise = (-lift / 18f).coerceIn(0f, 1f)
    drawOval(Color.Black.copy(alpha = 0.16f * (1f - rise * 0.6f)), Offset(50f - 20f * (1f - rise * 0.4f), 88f), Size(40f * (1f - rise * 0.4f), 6f))

    translate(top = lift) {
        rotate(tilt, Offset(50f, 86f)) {
            // Soft glow, so the sphere reads on light and dark backgrounds alike.
            drawCircle(Brush.radialGradient(listOf(colours[1].copy(alpha = 0.35f), Color.Transparent), c, r * 1.45f), r * 1.45f, c)

            // Jelly wobble: a little wider as it breathes out, a little taller as it breathes in.
            scale(sx * (1f + o.breath * 0.025f), sy * (1f - o.breath * 0.025f), pivot = Offset(50f, 86f)) {
                drawCircle(Color(0xFF0B0D24).copy(alpha = 0.55f), r + 1.6f, c)
                val sphere = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
                clipPath(sphere) {
                    drawCircle(colours[0], r, c)
                    // Liquid: three soft blobs of colour circling at different speeds and depths.
                    for (k in 1..3) {
                        val speed = when (k) { 1 -> 1f; 2 -> -1.6f; else -> 2.3f }
                        val a = (o.swirl * speed + k * 0.31f) * tau
                        val orbit = r * (0.42f - k * 0.05f)
                        val centre = c + Offset(cos(a) * orbit, sin(a * 0.8f) * orbit)
                        val blob = r * (1.05f - k * 0.12f)
                        drawCircle(Brush.radialGradient(listOf(colours[k], colours[k].copy(alpha = 0f)), centre, blob), blob, centre)
                    }
                    // A light ribbon sweeping through, like a fold in the liquid.
                    val ribbon = Path().apply {
                        val y = 50f + sin(o.swirl * tau) * 6f
                        moveTo(c.x - r, y + 10f)
                        cubicTo(c.x - 10f, y - 14f, c.x + 8f, y + 22f, c.x + r, y - 6f)
                    }
                    drawPath(ribbon, Color.White.copy(alpha = 0.22f), style = Stroke(9f))
                    // Glass: a broad sheen top left and a soft rim light.
                    drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(36f, 30f), 22f), Offset(16f, 14f), Size(40f, 30f))
                    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.White.copy(alpha = 0.28f)), c, r), r, c)
                }
                drawCircle(Color.White.copy(alpha = 0.85f), 2.2f, Offset(33f, 27f))
                drawCircle(Color.White.copy(alpha = 0.5f), 1.1f, Offset(39f, 24f))
                // The face lives on the sphere, so on a spin it slides round the edge and out of sight.
                clipPath(sphere) { drawFace(o) }
            }
        }
    }
    drawExtras(o, lift)
}

/** Eyes, mouth and cheeks. */
private fun DrawScope.drawFace(o: Pose) {
    val p = o.p
    val happy = o.joy > 0.15f || (o.trick == Trick.HOP && p in 0.2f..0.8f)
    val surprised = o.pull > 0.25f
    val winking = o.trick == Trick.WINK && p in 0.12f..0.88f
    val coin = o.trick == Trick.COIN
    // Where the eyes look: glances, the phone's tilt, and up at the coin.
    val lookX = (o.look * 3f + o.lean * 4f).coerceIn(-5f, 5f)
    val lookY = if (coin) -5f * arc(p * 1.25f) else 0f
    val eyeY = when (o.mood) { PetMood.THINKING -> 44f; else -> 48f } + o.breath * 0.6f + lookY
    // A spin carries the face round the back of the sphere and out the other side.
    val turn = when {
        o.spin > 0f -> o.spin
        o.trick == Trick.SPIN -> p
        else -> 0f
    }
    val shift = sin(turn * 2f * PI.toFloat()) * 26f
    val facing = cos(turn * 2f * PI.toFloat())
    if (facing < 0.05f && turn > 0f) return
    val squeeze = if (turn > 0f) facing.coerceIn(0f, 1f) else 1f

    scale(squeeze, 1f, pivot = Offset(50f + shift, 50f)) {
        translate(left = shift) {
            val w = 8f
            val eyes = listOf(41.5f, 58.5f)
            eyes.forEachIndexed { i, base ->
                val ex = base + lookX
                when {
                    happy || (winking && i == 0) -> {
                        // A happy arc, like a smile on its side: ^
                        val arcPath = Path().apply { moveTo(ex - 5f, eyeY + 2.5f); quadraticTo(ex, eyeY - 6f, ex + 5f, eyeY + 2.5f) }
                        drawPath(arcPath, Color.White, style = Stroke(3.4f, cap = StrokeCap.Round))
                    }
                    surprised -> drawCircle(Color.White, 5.4f, Offset(ex, eyeY))
                    else -> {
                        val h = (17f * o.eyeOpen).coerceAtLeast(2.6f) * if (o.mood == PetMood.HAPPY) 0.92f else 1f
                        val lean = if (o.mood == PetMood.WORRIED) (if (i == 0) 6f else -6f) else 0f
                        rotate(lean, Offset(ex, eyeY)) {
                            drawRoundRect(Color.White, Offset(ex - w / 2f, eyeY - h / 2f), Size(w, h), CornerRadius(w / 2f))
                        }
                    }
                }
            }
            // The mouth: a big open smile when delighted, an "o" when surprised, otherwise it follows the mood.
            val mx = 50f + lookX * 0.6f
            val my = eyeY + 12f
            val line = Stroke(2.6f, cap = StrokeCap.Round)
            when {
                happy -> drawPath(Path().apply { moveTo(mx - 6.5f, my - 1f); quadraticTo(mx, my + 9f, mx + 6.5f, my - 1f); close() }, Color.White)
                surprised -> drawCircle(Color.White, 2.8f, Offset(mx, my + 1.5f), style = Stroke(2.4f))
                winking -> drawPath(Path().apply { moveTo(mx - 5f, my); quadraticTo(mx + 1f, my + 4.5f, mx + 6f, my - 2f) }, Color.White, style = line)
                o.mood == PetMood.WORRIED -> drawPath(Path().apply { moveTo(mx - 4.5f, my + 2.5f); quadraticTo(mx, my - 1f, mx + 4.5f, my + 2.5f) }, Color.White, style = line)
                o.mood == PetMood.THINKING -> drawCircle(Color.White, 1.8f, Offset(mx + 3f, my + 1f), style = Stroke(2f))
                o.mood == PetMood.CALM -> drawPath(Path().apply { moveTo(mx - 3.5f, my); quadraticTo(mx, my + 3f, mx + 3.5f, my) }, Color.White, style = line)
                else -> drawPath(Path().apply { moveTo(mx - 5.5f, my - 0.5f); quadraticTo(mx, my + 5f, mx + 5.5f, my - 0.5f) }, Color.White, style = line)
            }
            if (o.mood == PetMood.HAPPY || happy) {
                // Warm cheeks under the eyes.
                drawCircle(Color(0xFFFFD6EA).copy(alpha = 0.5f), 3.4f, Offset(34f + lookX, eyeY + 9f))
                drawCircle(Color(0xFFFFD6EA).copy(alpha = 0.5f), 3.4f, Offset(66f + lookX, eyeY + 9f))
            }
        }
    }
}

/** Things outside the sphere: hearts when it is happy, a sparkle with the wink, the coin it tosses. */
private fun DrawScope.drawExtras(o: Pose, lift: Float) {
    val p = o.p
    if (o.joy > 0.02f) {
        // Three little hearts float up and fade.
        val q = 1f - o.joy
        listOf(Triple(70f, 26f, 6f), Triple(28f, 22f, 4.6f), Triple(58f, 12f, 3.8f)).forEachIndexed { k, (x, y, s) ->
            val up = q * (26f + k * 6f)
            heart(Offset(x + sin((q + k) * 5f) * 3f, y - up + lift), s * (0.6f + 0.6f * q), Color(0xFFFF6FA8).copy(alpha = o.joy.coerceIn(0f, 1f)))
        }
    }
    if (o.trick == Trick.WINK) {
        // A four-point sparkle pops beside the open eye.
        val s = 6f * arc(p)
        sparkle(Offset(76f, 30f + lift), s, Color(0xFFFFE08A))
    }
    if (o.trick == Trick.COIN) {
        // A tiny golden rupee coin, flipped up and caught again, spinning as it goes.
        val h = arc(p)
        val y = 6f - 30f * h + lift
        val turn = abs(cos(p * 7f * PI.toFloat())).coerceAtLeast(0.15f)
        val cw = 9f * turn
        drawOval(Brush.linearGradient(listOf(Color(0xFFFFE795), Color(0xFFF2A516)), Offset(50f - cw, y - 9f), Offset(50f + cw, y + 9f)), Offset(50f - cw, y - 9f), Size(cw * 2f, 18f))
        drawOval(Color(0xFFB8740A).copy(alpha = 0.7f), Offset(50f - cw, y - 9f), Size(cw * 2f, 18f), style = Stroke(1.2f))
        if (turn > 0.5f) {
            // The rupee mark on the face of the coin.
            val k = turn
            drawLine(Color(0xFFB8740A), Offset(50f - 3.5f * k, y - 4f), Offset(50f + 3.5f * k, y - 4f), 1.4f, StrokeCap.Round)
            drawLine(Color(0xFFB8740A), Offset(50f - 3.5f * k, y - 1.2f), Offset(50f + 3.5f * k, y - 1.2f), 1.4f, StrokeCap.Round)
            drawLine(Color(0xFFB8740A), Offset(50f - 1.5f * k, y + 1.2f), Offset(50f + 2.5f * k, y + 5f), 1.4f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.heart(at: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(at.x, at.y + s * 0.9f)
        cubicTo(at.x - s * 1.4f, at.y, at.x - s * 0.7f, at.y - s * 1.1f, at.x, at.y - s * 0.35f)
        cubicTo(at.x + s * 0.7f, at.y - s * 1.1f, at.x + s * 1.4f, at.y, at.x, at.y + s * 0.9f)
        close()
    }
    drawPath(path, color)
    drawCircle(Color.White.copy(alpha = color.alpha * 0.7f), s * 0.18f, Offset(at.x - s * 0.45f, at.y - s * 0.35f))
}

private fun DrawScope.sparkle(at: Offset, s: Float, color: Color) {
    if (s <= 0.2f) return
    val path = Path().apply {
        moveTo(at.x, at.y - s)
        quadraticTo(at.x, at.y, at.x + s, at.y)
        quadraticTo(at.x, at.y, at.x, at.y + s)
        quadraticTo(at.x, at.y, at.x - s, at.y)
        quadraticTo(at.x, at.y, at.x, at.y - s)
        close()
    }
    drawPath(path, color)
}
