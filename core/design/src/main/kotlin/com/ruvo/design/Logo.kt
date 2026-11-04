package com.ruvo.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LogoInk = Color(0xFF141B4D)
private val LogoViolet = Color(0xFF2E2680)
private val LogoPaper = Color(0xFFFFFFFF)
private val LogoHaldi = Color(0xFFFFC23D)
private val LogoOrange = Color(0xFFFF8A45)

private val LogoRunway = Color(0xFF8B6CFF)
private val LogoRunwayDeep = Color(0xFF5B3FE0)

/**
 * The Ruvo mark: a runway leading to a sunrise. A violet runway narrows to the horizon with white centre
 * dashes, and a yellow half-sun rises behind it: the day the next money arrives. On a night-blue tile that warms
 * to violet. [runway] builds the strip from the near end to the horizon, [sun] raises the sun (0 hidden, 1 risen,
 * a little more overshoots), and [dashShift] moves the dashes toward you; the opening animates all three.
 */
@Composable
fun RuvoLogo(modifier: Modifier = Modifier, size: Dp = 56.dp, tile: Color? = null, runway: Float = 1f, sun: Float = 1f, dashShift: Float = 0f) {
    Canvas(modifier.size(size).semantics { contentDescription = "Ruvo" }) {
        val s = this.size.width
        if (tile != null) drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.24f))
        else drawRoundRect(Brush.linearGradient(listOf(LogoViolet, LogoInk), Offset(0f, 0f), Offset(s, s)), cornerRadius = CornerRadius(s * 0.24f))
        val horizon = s * 0.55f
        // The sun, clipped at the horizon so it rises from behind it.
        if (sun > 0f) {
            val r = s * 0.2f
            val cy = horizon + r * (1f - sun)
            clipRect(bottom = horizon) {
                drawCircle(LogoHaldi.copy(alpha = 0.25f), r * 1.45f, Offset(s / 2, cy))
                drawCircle(Brush.verticalGradient(listOf(LogoHaldi, LogoOrange), cy - r, cy + r), r, Offset(s / 2, cy))
            }
        }
        // The horizon glow line.
        drawLine(LogoOrange.copy(alpha = 0.9f), Offset(s * 0.2f, horizon), Offset(s * 0.8f, horizon), s * 0.022f, StrokeCap.Round)
        // The runway: a trapezoid from near (wide, bottom) to far (narrow, at the horizon), built near end first.
        val t = runway.coerceIn(0f, 1f)
        if (t > 0f) {
            val nearY = s * 0.82f
            val farY = horizon + s * 0.025f
            val topY = nearY - (nearY - farY) * t
            fun halfWidth(y: Float) = s * (0.04f + 0.24f * (y - farY) / (nearY - farY))
            val strip = Path().apply {
                moveTo(s / 2 - halfWidth(nearY), nearY); lineTo(s / 2 + halfWidth(nearY), nearY)
                lineTo(s / 2 + halfWidth(topY), topY); lineTo(s / 2 - halfWidth(topY), topY); close()
            }
            drawPath(strip, Brush.verticalGradient(listOf(LogoRunwayDeep, LogoRunway), farY, nearY))
            // Centre dashes, longer and wider as they come closer, sliding toward you with [dashShift].
            for (i in 0 until 3) {
                val p = ((i + dashShift) / 3f) % 1f
                val y0 = farY + (nearY - farY) * (0.12f + 0.8f * p * p)
                val len = s * (0.025f + 0.05f * p)
                if (y0 + len > topY && y0 < nearY - s * 0.01f) {
                    drawLine(LogoPaper, Offset(s / 2, maxOf(y0, topY)), Offset(s / 2, minOf(y0 + len, nearY - s * 0.01f)), s * (0.012f + 0.018f * p), StrokeCap.Round)
                }
            }
        }
    }
}


/**
 * The opening: the tile spins in, the runway draws itself out to the horizon with its dashes rushing toward you,
 * then the sun rises behind it with a little bounce, and as it clears the horizon a ring and a burst of
 * confetti in the brand colours fly out. Then each letter of "ruvo." hops up on a spring, the final dot dropping in last, and
 * the line appears. About 2.5 s; a tap skips it. Always dark, so it reads as a cover. Reduced motion shows the
 * finished mark at once.
 */
@Composable
fun RuvoSplash(modifier: Modifier = Modifier, onSkip: () -> Unit = {}) {
    val reduced = Ruvo.reducedMotion
    val start = if (reduced) 1f else 0f
    val tile = remember { Animatable(start) }
    val runway = remember { Animatable(start) }
    val sun = remember { Animatable(start) }
    val dashes = remember { Animatable(0f) }
    val burst = remember { Animatable(start) }
    val letters = remember { List(5) { Animatable(start) } }
    val line = remember { Animatable(start) }
    // The sun overshoots a touch as it rises, then settles, like it popped up over the horizon.
    val rise = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.45f, stiffness = 180f)
    val hop = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.42f, stiffness = 420f)
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { tile.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 300f)) }
        launch { delay(260); runway.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { delay(260); dashes.animateTo(3f, tween(1400, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) }
        launch { delay(820); sun.animateTo(1f, rise) }
        launch { delay(1050); burst.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        letters.forEachIndexed { i, l -> launch { delay(1150L + i * 80L + if (i == 4) 160L else 0L); l.animateTo(1f, hop) } }
        launch { delay(1800); line.animateTo(1f, Motion.gentle()) }
    }
    val brand = listOf(LogoHaldi, LogoOrange, Color(0xFF8B6CFF), Color(0xFFFF8FB0), LogoPaper)
    Box(
        modifier.fillMaxSize().background(Color(0xFF0A0D26))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Skip", onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height * 0.42f)
            drawRect(Brush.radialGradient(listOf(Color(0xFF2A2272), Color(0xFF0A0D26)), centre, size.height * 0.6f))
            val b = burst.value
            if (b > 0f && b < 1f) {
                // The sun's centre, just above the middle of the 112 dp tile.
                val land = centre + Offset(0f, -6.dp.toPx())
                val ringR = 14.dp.toPx() + 90.dp.toPx() * b
                drawCircle(LogoHaldi.copy(alpha = (1f - b) * 0.7f), ringR, land, style = Stroke(3.dp.toPx() * (1f - b) + 0.5f))
                // Confetti: little dots and dashes thrown outward, slowing and fading.
                for (i in 0 until 14) {
                    val ang = i / 14f * 2f * kotlin.math.PI.toFloat() + 0.3f
                    val dist = 30.dp.toPx() + 110.dp.toPx() * b * (0.7f + (i % 3) * 0.15f)
                    val p = land + Offset(kotlin.math.cos(ang) * dist, kotlin.math.sin(ang) * dist + 40.dp.toPx() * b * b)
                    val col = brand[i % brand.size].copy(alpha = (1f - b))
                    if (i % 2 == 0) drawCircle(col, 4.dp.toPx(), p)
                    else drawLine(col, p, p + Offset(kotlin.math.cos(ang + 1f) * 8.dp.toPx(), kotlin.math.sin(ang + 1f) * 8.dp.toPx()), 3.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
        }
        Column(
            Modifier.padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            RuvoLogo(
                Modifier.graphicsLayer {
                    val t = tile.value
                    val sc = 0.3f + 0.7f * t
                    scaleX = sc; scaleY = sc; alpha = t.coerceIn(0f, 1f)
                    rotationZ = (1f - t) * -40f
                },
                size = 112.dp, runway = runway.value, sun = sun.value, dashShift = dashes.value,
            )
            Spacer(Modifier.height(Space.lg))
            Row {
                "ruvo.".forEachIndexed { i, ch ->
                    val p = letters[i].value
                    Text(
                        ch.toString(),
                        Modifier.graphicsLayer {
                            alpha = p.coerceIn(0f, 1f)
                            // The final dot falls in from above; the letters hop up from below.
                            translationY = (1f - p) * (if (ch == '.') -46.dp.toPx() else 26.dp.toPx())
                            val sc = 0.6f + 0.4f * p
                            scaleX = sc; scaleY = sc
                        },
                        style = Ruvo.type.display.copy(fontSize = Ruvo.type.display.fontSize * 1.25f),
                        color = if (ch == '.') LogoHaldi else LogoPaper,
                    )
                }
            }
            Spacer(Modifier.height(Space.xxs))
            Text(
                "Your money, without the mental load.",
                Modifier.graphicsLayer { alpha = line.value.coerceIn(0f, 1f); translationY = (1f - line.value) * 12.dp.toPx() },
                style = Ruvo.type.label, color = LogoPaper.copy(alpha = 0.65f),
            )
        }
        Text(
            "Tap to skip",
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Space.xl),
            style = Ruvo.type.caption, color = LogoPaper.copy(alpha = 0.4f),
        )
    }
}
