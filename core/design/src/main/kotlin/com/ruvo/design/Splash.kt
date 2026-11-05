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
 * The opening, a tiny colourful story in about three and a half seconds:
 *  1. The night sky lights up with soft colour glows.
 *  2. A blue rupee coin drops in spinning and bounces twice on an invisible floor, its shadow growing as it falls.
 *  3. On the last landing it squashes, then pops open into the white logo tile, which springs up while the R
 *     builds itself inside.
 *  5. The tile does a happy hop; a golden ring, soft rays and gold and white confetti fly out.
 *  6. "RUVO" jumps in letter by letter in white, a little wave runs through it, and the line fades up.
 * A tap skips it. Always dark, so it reads as a cover. Reduced motion shows the finished picture at once.
 * [mascot] can draw a character beside the tile; the app leaves it empty.
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
    // The rupee coin: height above its resting place in dp, turns of spin, a squash on landing, and pop-and-fade.
    val drop = remember { Animatable(if (reduced) 0f else -440f) }
    val spin = remember { Animatable(if (reduced) 3f else 0f) }
    val squash = remember { Animatable(0f) }
    val coinGone = remember { Animatable(start) }
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
        // The rupee coin falls spinning, bounces twice like a coin dropped on a table, squashes and pops.
        launch {
            drop.animateTo(
                0f,
                keyframes {
                    durationMillis = 880
                    -440f at 0 using FastOutLinearInEasing
                    0f at 430 using LinearOutSlowInEasing
                    -120f at 580 using FastOutLinearInEasing
                    0f at 720 using LinearOutSlowInEasing
                    -36f at 800 using FastOutLinearInEasing
                    0f at 880
                },
            )
        }
        launch { spin.animateTo(3f, tween(880, easing = FastOutSlowInEasing)) }
        launch { delay(860); squash.animateTo(1f, tween(90)); squash.animateTo(0f, tween(110)) }
        launch { delay(960); coinGone.animateTo(1f, tween(220)) }
        launch { delay(980); tile.animateTo(1f, springy) }
        launch { delay(1120); build.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        launch {
            delay(1940)
            hop.animateTo(1f, tween(170, easing = LinearOutSlowInEasing))
            hop.animateTo(0f, jump)
        }
        launch { delay(1960); burst.animateTo(1f, tween(1150, easing = FastOutSlowInEasing)) }
        launch { delay(1960); rays.animateTo(1f, tween(600)) }
        launch { delay(2100); pet.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
        launch { delay(2260); cheer.snapTo(1f); cheer.animateTo(0f, tween(1700)) }
        letters.forEachIndexed { i, l -> launch { delay(2120L + i * 85L); l.animateTo(1f, jump) } }
        whiten.forEachIndexed { i, w -> launch { delay(2420L + i * 85L); w.animateTo(1f, tween(450)) } }
        launch { delay(2520); bar.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        launch { delay(2660); wave.animateTo(1f, tween(650, easing = LinearEasing)) }
        launch { delay(2580); line.animateTo(1f, Motion.gentle()) }
    }
    // Gold, white and a little blue: the coin's colours, nothing more.
    val confetti = listOf(Sun, Paper, Sky)
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
                        val col = if (i % 2 == 0) Sun else Sky
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
            // The coin's shadow on the floor: bigger and darker the closer the coin gets.
            if (coinGone.value < 1f) {
                val h = (-drop.value / 440f).coerceIn(0f, 1f)
                val sw = 46.dp.toPx() * (1f - 0.6f * h)
                drawOval(Color.Black.copy(alpha = 0.4f * (1f - h) * (1f - coinGone.value)), Offset(centre.x - sw / 2, centre.y + 34.dp.toPx()), Size(sw, sw * 0.22f))
            }
            val bu = burst.value
            if (bu > 0f && bu < 1f) {
                // A golden ring and confetti thrown outward, slowing, falling and fading.
                val ringR = 34.dp.toPx() + 120.dp.toPx() * bu
                drawCircle(Sun, ringR, centre, alpha = (1f - bu) * 0.85f, style = Stroke(5.dp.toPx() * (1f - bu) + 0.5f))
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
                // The rupee coin: its width follows the spin, it squashes on the last landing, then pops open and fades
                // as the tile springs up in its place.
                if (coinGone.value < 1f) Box(
                    Modifier.size(64.dp).graphicsLayer {
                        translationY = drop.value * density
                        val turnX = kotlin.math.abs(cos(spin.value * PI.toFloat())).coerceAtLeast(0.12f)
                        val grow = 1f + 0.6f * coinGone.value
                        scaleX = turnX * (1f + 0.22f * squash.value) * grow
                        scaleY = (1f - 0.24f * squash.value) * grow
                        alpha = 1f - coinGone.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        // A golden coin: bright at the top left, deeper gold at the bottom right, with a darker rim.
                        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE795), Color(0xFFF2A516)), Offset.Zero, Offset(size.width, size.height)))
                        drawCircle(Color(0xFFB8740A), size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                        drawCircle(Paper.copy(alpha = 0.35f), size.minDimension / 2 - 4.dp.toPx(), style = Stroke(2.dp.toPx()))
                        drawCircle(Paper.copy(alpha = 0.25f), size.minDimension * 0.12f, Offset(size.width * 0.32f, size.height * 0.3f))
                    }
                    Text("₹", style = Ruvo.type.display.copy(fontWeight = FontWeight(800)), color = Color(0xFF8A5A00))
                }
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
                        color = Paper,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
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

/** One soft blue light drifting behind the opening, and a quieter one low down. Nothing else. */
private fun DrawScope.drawNightGlows(strength: Float, t: Float) {
    if (strength <= 0f) return
    val a = 2f * PI.toFloat() * t
    fun glow(col: Color, x: Float, y: Float, r: Float) {
        val at = Offset(size.width * x, size.height * y)
        val rr = size.width * r
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = col.alpha * strength), Color.Transparent), at, rr), rr, at)
    }
    glow(Color(0x803550D6), 0.9f + 0.06f * cos(a), 0.12f + 0.03f * sin(a), 1.0f)
    glow(Color(0x402338A8), 0.08f, 0.85f + 0.03f * sin(a), 0.8f)
}
