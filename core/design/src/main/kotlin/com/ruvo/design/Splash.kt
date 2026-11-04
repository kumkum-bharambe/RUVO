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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Night = Color(0xFF070B24)
private val NightGlow = Color(0xFF1A2A7A)
private val Sky = Color(0xFF4F7BFF)
private val SkyPale = Color(0xFF9DB4FF)
private val Royal = Color(0xFF2E45C9)
private val Sun = Color(0xFFFFC23D)
private val Paper = Color(0xFFFFFFFF)

/**
 * The opening, a tiny story in about three seconds:
 *  1. A blue rupee coin drops in from above, spinning, and bounces twice on an invisible floor; its shadow grows
 *     as it falls.
 *  2. On the last landing it squashes, then pops open into the white logo tile, which springs up with a wiggle.
 *  3. The R builds itself inside: the bowl drops, the leg strikes, the rupee bars slide in and the tail follows.
 *  4. The tile does a happy hop; a ring, slow light rays and a burst of confetti fly out.
 *  5. "RUVO" jumps in letter by letter, a little wave runs through it, and the line fades up.
 * A tap skips it. Always dark, so it reads as a cover. Reduced motion shows the finished mark at once.
 */
@Composable
fun RuvoSplash(modifier: Modifier = Modifier, onSkip: () -> Unit = {}) {
    val reduced = Ruvo.reducedMotion
    val start = if (reduced) 1f else 0f
    // The coin: height above its resting place in dp, turns of spin, pop-and-fade, and squash on landing.
    val drop = remember { Animatable(if (reduced) 0f else -440f) }
    val spin = remember { Animatable(if (reduced) 3f else 0f) }
    val coinGone = remember { Animatable(start) }
    val squash = remember { Animatable(0f) }
    // The tile, the mark inside it, and the celebration.
    val tile = remember { Animatable(start) }
    val build = remember { Animatable(start) }
    val hop = remember { Animatable(0f) }
    val burst = remember { Animatable(start) }
    val rays = remember { Animatable(start) }
    val letters = remember { List(4) { Animatable(start) } }
    val wave = remember { Animatable(if (reduced) 1f else 0f) }
    val line = remember { Animatable(start) }

    // Falls, bounces, bounces smaller, settles: like a coin dropped on a table, never sinking below it.
    val bounce = keyframes<Float> {
        durationMillis = 880
        -440f at 0 using FastOutLinearInEasing
        0f at 430 using LinearOutSlowInEasing
        -120f at 580 using FastOutLinearInEasing
        0f at 720 using LinearOutSlowInEasing
        -36f at 800 using FastOutLinearInEasing
        0f at 880
    }
    val springy = spring<Float>(dampingRatio = 0.42f, stiffness = 320f)
    val jump = spring<Float>(dampingRatio = 0.4f, stiffness = 420f)
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { drop.animateTo(0f, bounce) }
        launch { spin.animateTo(3f, tween(880, easing = FastOutSlowInEasing)) }
        launch {
            delay(860)
            squash.animateTo(1f, tween(90))
            squash.animateTo(0f, tween(110))
        }
        launch { delay(960); coinGone.animateTo(1f, tween(220)) }
        launch { delay(980); tile.animateTo(1f, springy) }
        launch { delay(1120); build.animateTo(1f, tween(820, easing = FastOutSlowInEasing)) }
        launch {
            delay(1940)
            hop.animateTo(1f, tween(170, easing = LinearOutSlowInEasing))
            hop.animateTo(0f, jump)
        }
        launch { delay(1960); burst.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        launch { delay(1960); rays.animateTo(1f, tween(600)) }
        letters.forEachIndexed { i, l -> launch { delay(2080L + i * 90L); l.animateTo(1f, jump) } }
        launch { delay(2560); wave.animateTo(1f, tween(650, easing = LinearEasing)) }
        launch { delay(2500); line.animateTo(1f, Motion.gentle()) }
    }
    val confetti = listOf(Sky, Sun, SkyPale, Paper, Royal)
    val loop = rememberInfiniteTransition(label = "rays")
    val turn by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "turn")

    Box(
        modifier.fillMaxSize().background(Night)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Skip", onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height * 0.42f)
            drawRect(Brush.radialGradient(listOf(NightGlow, Night), centre, size.height * 0.6f))
            // Slow light rays behind the logo once it is complete.
            if (rays.value > 0f) {
                rotate(if (reduced) 0f else turn, centre) {
                    val len = size.maxDimension
                    for (i in 0 until 12) {
                        val a = i / 12f * 2f * PI.toFloat()
                        val w = 0.09f
                        drawPath(
                            Path().apply {
                                moveTo(centre.x, centre.y)
                                lineTo(centre.x + cos(a - w) * len, centre.y + sin(a - w) * len)
                                lineTo(centre.x + cos(a + w) * len, centre.y + sin(a + w) * len)
                                close()
                            },
                            Brush.radialGradient(listOf(Sky.copy(alpha = 0.16f * rays.value), Color.Transparent), centre, len * 0.55f),
                        )
                    }
                }
            }
            // The coin's shadow on the floor: bigger and darker the closer the coin gets.
            if (coinGone.value < 1f) {
                val h = (-drop.value / 440f).coerceIn(0f, 1f)
                val sw = 46.dp.toPx() * (1f - 0.6f * h)
                drawOval(Color.Black.copy(alpha = 0.35f * (1f - h) * (1f - coinGone.value)), Offset(centre.x - sw / 2, centre.y + 34.dp.toPx()), Size(sw, sw * 0.22f))
            }
            val b = burst.value
            if (b > 0f && b < 1f) {
                val ringR = 30.dp.toPx() + 110.dp.toPx() * b
                drawCircle(Sky.copy(alpha = (1f - b) * 0.8f), ringR, centre, style = Stroke(4.dp.toPx() * (1f - b) + 0.5f))
                // Confetti: little dots, dashes and squares thrown outward, slowing, falling and fading.
                for (i in 0 until 18) {
                    val ang = i / 18f * 2f * PI.toFloat() + 0.2f
                    val dist = 40.dp.toPx() + 130.dp.toPx() * b * (0.65f + (i % 3) * 0.17f)
                    val p = centre + Offset(cos(ang) * dist, sin(ang) * dist + 70.dp.toPx() * b * b)
                    val col = confetti[i % confetti.size].copy(alpha = 1f - b)
                    when (i % 3) {
                        0 -> drawCircle(col, 4.5.dp.toPx(), p)
                        1 -> drawLine(col, p, p + Offset(cos(ang + 1f) * 9.dp.toPx(), sin(ang + 1f) * 9.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                        else -> drawRect(col, p, Size(6.dp.toPx(), 6.dp.toPx()))
                    }
                }
            }
        }
        Column(
            Modifier.padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
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
                // The coin: its width follows the spin, it squashes on the last landing, then pops open and fades.
                if (coinGone.value < 1f) Box(
                    Modifier.size(64.dp).graphicsLayer {
                        translationY = drop.value * density
                        val turnX = abs(cos(spin.value * PI.toFloat())).coerceAtLeast(0.12f)
                        val pop = 1f + 0.6f * coinGone.value
                        scaleX = turnX * (1f + 0.22f * squash.value) * pop
                        scaleY = (1f - 0.24f * squash.value) * pop
                        alpha = 1f - coinGone.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(Brush.linearGradient(listOf(Color(0xFF6E8CFF), Color(0xFF1F35B8)), Offset.Zero, Offset(size.width, size.height)))
                        drawCircle(Paper.copy(alpha = 0.35f), size.minDimension / 2 - 4.dp.toPx(), style = Stroke(2.dp.toPx()))
                        drawCircle(Paper.copy(alpha = 0.25f), size.minDimension * 0.12f, Offset(size.width * 0.32f, size.height * 0.3f))
                    }
                    Text("₹", style = Ruvo.type.display.copy(fontWeight = FontWeight(800)), color = Paper)
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
                            translationY = (1f - p) * 30.dp.toPx() - bump * 9.dp.toPx()
                            val sc = (0.5f + 0.5f * p) * (1f + 0.08f * bump)
                            scaleX = sc; scaleY = sc
                            rotationZ = (1f - p) * (if (i % 2 == 0) -16f else 16f)
                        },
                        style = Ruvo.type.display.copy(fontSize = Ruvo.type.display.fontSize * 1.2f, fontWeight = FontWeight(800)),
                        color = Paper,
                    )
                }
            }
            Spacer(Modifier.height(Space.xxs))
            Text(
                "Your money, without the mental load.",
                Modifier.graphicsLayer { alpha = line.value.coerceIn(0f, 1f); translationY = (1f - line.value) * 12.dp.toPx() },
                style = Ruvo.type.label, color = Paper.copy(alpha = 0.65f),
            )
        }
        Text(
            "Tap to skip",
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Space.xl),
            style = Ruvo.type.caption, color = Paper.copy(alpha = 0.4f),
        )
    }
}
