package com.ruvo.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Night = Color(0xFF070B24)
private val Paper = Color(0xFFFFFFFF)
private val Sun = Color(0xFFFFC23D)
private val Tangerine = Color(0xFFFF8A45)
private val Violet = Color(0xFF8B6CFF)
private val Sky = Color(0xFF4F7BFF)
private val Pink = Color(0xFFFF6FA8)

/** The four drops, in the order they fall: Fuel yellow, Future orange, Fixed purple and Ruvo's own blue. */
private val Drops = listOf(Sun, Tangerine, Violet, Sky)

/**
 * The opening, a tiny colourful story in under four seconds:
 *  1. The night sky lights up with soft colour glows.
 *  2. Four glossy drops (yellow, orange, purple, blue: Fuel, Future, Fixed and Ruvo) fall one after another and
 *     bounce on an invisible floor, each with its own shadow.
 *  3. One by one they hop into the middle and merge into a single swirling ball of all four colours.
 *  4. The ball squashes and pops into the white logo tile, which springs up while the R builds itself inside.
 *  5. The tile does a happy hop; a rainbow ring, coloured rays and confetti fly out, and the pet pops up beside it.
 *  6. "RUVO" jumps in letter by letter, each in its drop's colour, settles to white, and a little wave runs
 *     through it over a four-colour bar; the line fades up.
 * A tap skips it. Always dark, so it reads as a cover. Reduced motion shows the finished picture at once.
 * [mascot] draws the pet; it is given a 0..1 cheer that rises as it arrives and fades away.
 */
@Composable
fun RuvoSplash(
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    mascot: (@Composable (modifier: Modifier, cheer: Float) -> Unit)? = null,
) {
    val reduced = Ruvo.reducedMotion
    val start = if (reduced) 1f else 0f
    val glowIn = remember { Animatable(start) }
    // The drops: height above the floor in dp, a squash on landing, and how far each has travelled into the ball.
    val fall = remember { List(4) { Animatable(if (reduced) 0f else -560f) } }
    val landed = remember { List(4) { Animatable(0f) } }
    val merge = remember { List(4) { Animatable(start) } }
    // The merged ball: how big it has grown, its squash before popping, and the pop itself.
    val ball = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val pop = remember { Animatable(start) }
    // The tile, the mark inside it, and the celebration.
    val tile = remember { Animatable(start) }
    val build = remember { Animatable(start) }
    val hop = remember { Animatable(0f) }
    val burst = remember { Animatable(start) }
    val rays = remember { Animatable(start) }
    val pet = remember { Animatable(start) }
    val cheer = remember { Animatable(0f) }
    val letters = remember { List(4) { Animatable(start) } }
    val whiten = remember { List(4) { Animatable(start) } }
    val wave = remember { Animatable(if (reduced) 1f else 0f) }
    val bar = remember { Animatable(start) }
    val line = remember { Animatable(start) }

    val springy = spring<Float>(dampingRatio = 0.42f, stiffness = 320f)
    val jump = spring<Float>(dampingRatio = 0.4f, stiffness = 420f)
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { glowIn.animateTo(1f, tween(900)) }
        Drops.indices.forEach { i ->
            val at = 80L + i * 110L
            launch {
                delay(at)
                // Falls, bounces, bounces smaller, settles: like a ball of jelly dropped on a table.
                fall[i].animateTo(
                    0f,
                    keyframes {
                        durationMillis = 640
                        -560f at 0 using FastOutLinearInEasing
                        0f at 380 using LinearOutSlowInEasing
                        -64f at 490 using FastOutLinearInEasing
                        0f at 590 using LinearOutSlowInEasing
                        -12f at 615
                        0f at 640
                    },
                )
            }
            launch { delay(at + 380); landed[i].animateTo(1f, tween(70)); landed[i].animateTo(0f, tween(150)) }
            launch {
                delay(1000L + i * 80L)
                merge[i].animateTo(1f, tween(330, easing = FastOutSlowInEasing))
                ball.animateTo((i + 1) / 4f, spring(dampingRatio = 0.5f, stiffness = 600f))
            }
        }
        launch { delay(1540); squash.animateTo(1f, tween(110)); squash.animateTo(0f, tween(90)); }
        launch { delay(1650); pop.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
        launch { delay(1660); tile.animateTo(1f, springy) }
        launch { delay(1780); build.animateTo(1f, tween(760, easing = FastOutSlowInEasing)) }
        launch {
            delay(2540)
            hop.animateTo(1f, tween(170, easing = LinearOutSlowInEasing))
            hop.animateTo(0f, jump)
        }
        launch { delay(2560); burst.animateTo(1f, tween(1150, easing = FastOutSlowInEasing)) }
        launch { delay(2560); rays.animateTo(1f, tween(600)) }
        launch { delay(2700); pet.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
        launch { delay(2860); cheer.snapTo(1f); cheer.animateTo(0f, tween(1700)) }
        letters.forEachIndexed { i, l -> launch { delay(2740L + i * 85L); l.animateTo(1f, jump) } }
        whiten.forEachIndexed { i, w -> launch { delay(3020L + i * 85L); w.animateTo(1f, tween(450)) } }
        launch { delay(3120); bar.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        launch { delay(3260); wave.animateTo(1f, tween(650, easing = LinearEasing)) }
        launch { delay(3180); line.animateTo(1f, Motion.gentle()) }
    }
    val confetti = listOf(Sun, Tangerine, Violet, Sky, Pink, Paper)
    val loop = rememberInfiniteTransition(label = "splash")
    val turn by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "turn")
    val swirl by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "swirl")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "drift")
    var tileCentre by remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier.fillMaxSize().background(Night)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Skip", onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = if (tileCentre.isSpecified) tileCentre else Offset(size.width / 2, size.height * 0.42f)
            drawNightGlows(glowIn.value, if (reduced) 0.2f else drift)
            // Coloured light rays behind the logo once it is complete, turning slowly.
            if (rays.value > 0f) {
                rotate(if (reduced) 0f else turn, centre) {
                    val len = size.maxDimension
                    for (i in 0 until 12) {
                        val a = i / 12f * 2f * PI.toFloat()
                        val w = 0.08f
                        val col = listOf(Sky, Sun, Pink, Violet)[i % 4]
                        drawPath(
                            Path().apply {
                                moveTo(centre.x, centre.y)
                                lineTo(centre.x + cos(a - w) * len, centre.y + sin(a - w) * len)
                                lineTo(centre.x + cos(a + w) * len, centre.y + sin(a + w) * len)
                                close()
                            },
                            Brush.radialGradient(listOf(col.copy(alpha = 0.16f * rays.value), Color.Transparent), centre, len * 0.5f),
                        )
                    }
                }
            }
            // The drops, falling, bouncing, then hopping into the middle.
            val r = 15.dp.toPx()
            val floor = centre.y + 26.dp.toPx()
            Drops.forEachIndexed { i, col ->
                val e = merge[i].value
                if (e >= 1f) return@forEachIndexed
                val colX = centre.x + (i - 1.5f) * 56.dp.toPx()
                val restY = floor - r
                val dropY = restY + fall[i].value * density
                val x = colX + (centre.x - colX) * e
                val y = dropY + (centre.y - dropY) * e - 54.dp.toPx() * sin(PI.toFloat() * e)
                val k = 1f - 0.45f * e
                // Its shadow on the floor: bigger and darker the lower it is.
                val height = ((restY - y) / (560.dp.toPx())).coerceIn(0f, 1f)
                val sw = r * 2.2f * (1f - 0.6f * height) * k
                drawOval(Color.Black.copy(alpha = 0.4f * (1f - height) * (1f - e)), Offset(x - sw / 2, floor + 3.dp.toPx()), Size(sw, sw * 0.24f))
                val sq = landed[i].value
                drawDrop(col, Offset(x, y + sq * r * 0.2f), r * k * (1f + 0.22f * sq), r * k * (1f - 0.24f * sq))
            }
            // The merged ball: all four colours swirling, growing as each drop joins, then squashing and popping.
            val b = ball.value
            if (b > 0f && pop.value < 1f) {
                val br = 34.dp.toPx() * b * (1f + 0.7f * pop.value)
                val alpha = 1f - pop.value
                val sx = 1f + 0.26f * squash.value
                val sy = 1f - 0.26f * squash.value
                rotate(if (reduced) 0f else swirl, centre) {
                    drawOval(
                        Brush.sweepGradient(Drops + Drops.first(), centre),
                        Offset(centre.x - br * sx, centre.y - br * sy), Size(br * 2 * sx, br * 2 * sy), alpha = alpha,
                    )
                }
                drawOval(
                    Brush.radialGradient(listOf(Paper.copy(alpha = 0.55f * alpha), Color.Transparent), Offset(centre.x - br * 0.35f, centre.y - br * 0.4f), br * 0.7f),
                    Offset(centre.x - br * sx, centre.y - br * sy), Size(br * 2 * sx, br * 2 * sy),
                )
            }
            val bu = burst.value
            if (bu > 0f && bu < 1f) {
                // A rainbow ring and confetti thrown outward, slowing, falling and fading.
                val ringR = 34.dp.toPx() + 120.dp.toPx() * bu
                drawCircle(Brush.sweepGradient(Drops + Drops.first(), centre), ringR, centre, alpha = (1f - bu) * 0.9f, style = Stroke(5.dp.toPx() * (1f - bu) + 0.5f))
                for (i in 0 until 22) {
                    val ang = i / 22f * 2f * PI.toFloat() + 0.2f
                    val dist = 44.dp.toPx() + 140.dp.toPx() * bu * (0.62f + (i % 3) * 0.18f)
                    val p = centre + Offset(cos(ang) * dist, sin(ang) * dist + 80.dp.toPx() * bu * bu)
                    val col = confetti[i % confetti.size].copy(alpha = 1f - bu)
                    when (i % 3) {
                        0 -> drawCircle(col, 4.5.dp.toPx(), p)
                        1 -> drawLine(col, p, p + Offset(cos(ang + 1f) * 10.dp.toPx(), sin(ang + 1f) * 10.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                        else -> rotate(bu * 540f + i * 20f, p) { drawRect(col, p - Offset(3.dp.toPx(), 3.dp.toPx()), Size(6.dp.toPx(), 6.dp.toPx())) }
                    }
                }
            }
        }
        Column(
            Modifier.padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(112.dp).onGloballyPositioned {
                    val p = it.positionInRoot()
                    tileCentre = Offset(p.x + it.size.width / 2f, p.y + it.size.height / 2f)
                },
                contentAlignment = Alignment.Center,
            ) {
                RuvoLogo(
                    Modifier.graphicsLayer {
                        val t = tile.value
                        val sc = (0.2f + 0.8f * t) * (1f + 0.08f * hop.value)
                        scaleX = sc; scaleY = sc * (1f - 0.05f * hop.value)
                        alpha = t.coerceIn(0f, 1f)
                        rotationZ = (1f - t) * 14f
                        translationY = -20.dp.toPx() * hop.value
                    },
                    size = 112.dp, build = build.value,
                )
                // The pet pops up at the tile's corner and cheers.
                if (mascot != null) {
                    mascot(
                        Modifier.align(Alignment.BottomEnd).offset(x = 34.dp, y = 26.dp).size(58.dp).graphicsLayer {
                            val p = pet.value
                            scaleX = p; scaleY = p
                            alpha = p.coerceIn(0f, 1f)
                            rotationZ = (1f - p) * -40f
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                        cheer.value,
                    )
                }
            }
            Spacer(Modifier.height(Space.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                "RUVO".forEachIndexed { i, ch ->
                    val p = letters[i].value
                    // After they land, a wave runs left to right: each letter rises and settles in turn.
                    val w = ((wave.value * 1.6f) - i * 0.2f).coerceIn(0f, 1f)
                    val bump = if (wave.value > 0.001f && wave.value < 0.999f) sin(w * PI.toFloat()) else 0f
                    Text(
                        ch.toString(),
                        Modifier.graphicsLayer {
                            alpha = p.coerceIn(0f, 1f)
                            translationY = (1f - p) * 34.dp.toPx() - bump * 9.dp.toPx()
                            val sc = (0.5f + 0.5f * p) * (1f + 0.08f * bump)
                            scaleX = sc; scaleY = sc
                            rotationZ = (1f - p) * (if (i % 2 == 0) -18f else 18f)
                        },
                        style = Ruvo.type.display.copy(fontSize = Ruvo.type.display.fontSize * 1.2f, fontWeight = FontWeight(800)),
                        color = lerp(Drops[i], Paper, whiten[i].value),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // Four little bars, one per drop: the colours Ruvo is made of.
            Row(
                Modifier.graphicsLayer { scaleX = bar.value; alpha = bar.value.coerceIn(0f, 1f) },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Drops.forEach { col -> Box(Modifier.width(22.dp).height(5.dp).clip(RoundedCornerShape(Radius.chip)).background(col)) }
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                "Your money, without the mental load.",
                Modifier.graphicsLayer { alpha = line.value.coerceIn(0f, 1f); translationY = (1f - line.value) * 12.dp.toPx() },
                style = Ruvo.type.label, color = Paper.copy(alpha = 0.7f),
            )
        }
        Text(
            "Tap to skip",
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Space.xl),
            style = Ruvo.type.caption, color = Paper.copy(alpha = 0.4f),
        )
    }
}

/** Soft blue, pink, purple and yellow lights drifting in the night sky behind the opening. */
private fun DrawScope.drawNightGlows(strength: Float, t: Float) {
    if (strength <= 0f) return
    val a = 2f * PI.toFloat() * t
    fun glow(col: Color, x: Float, y: Float, r: Float) {
        val at = Offset(size.width * x, size.height * y)
        val rr = size.width * r
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = col.alpha * strength), Color.Transparent), at, rr), rr, at)
    }
    glow(Color(0x803550D6), 0.9f + 0.06f * cos(a), 0.12f + 0.03f * sin(a), 1.0f)
    glow(Color(0x40FF6FA8), 0.05f + 0.05f * sin(a), 0.72f + 0.04f * cos(a), 0.85f)
    glow(Color(0x4D8B6CFF), 0.95f, 0.88f + 0.03f * sin(a), 0.8f)
    glow(Color(0x26FFC23D), 0.1f + 0.04f * cos(a), 0.3f, 0.6f)
}

/** One glossy drop: its colour with a lighter top, a soft highlight and a bright glint. */
private fun DrawScope.drawDrop(col: Color, at: Offset, rx: Float, ry: Float) {
    drawOval(
        Brush.radialGradient(listOf(col.lighten(0.35f), col, col.copy(red = col.red * 0.85f, green = col.green * 0.85f, blue = col.blue * 0.85f)), Offset(at.x - rx * 0.3f, at.y - ry * 0.4f), maxOf(rx, ry) * 1.4f),
        Offset(at.x - rx, at.y - ry), Size(rx * 2, ry * 2),
    )
    drawOval(Paper.copy(alpha = 0.55f), Offset(at.x - rx * 0.55f, at.y - ry * 0.62f), Size(rx * 0.5f, ry * 0.32f))
    drawCircle(Paper.copy(alpha = 0.9f), rx * 0.1f, Offset(at.x + rx * 0.35f, at.y - ry * 0.3f))
}
