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
import androidx.compose.ui.graphics.drawscope.translate
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

/** The R, a deep purple; and the rupee strokes inside it, a bright lavender. */
private val LogoR = Color(0xFF2E1A87)
private val LogoRupee = Color(0xFF9D7BFF)

/**
 * The Ruvo mark: a bold R whose left side is a rupee sign. The R's top bar, bowl and leg are deep purple; the
 * rupee's two bars and its diagonal tail are bright lavender, running parallel to the R's leg. On a white tile
 * with a faint lavender fade, so it stands out on dark and light screens alike.
 *
 * [build] puts it together for the opening: the top and bowl drop in, the leg strikes down, then the rupee bars
 * slide in from the left and its tail follows. At 1 the mark is still.
 */
@Composable
fun RuvoLogo(modifier: Modifier = Modifier, size: Dp = 56.dp, tile: Color? = null, build: Float = 1f) {
    Canvas(modifier.size(size).semantics { contentDescription = "Ruvo" }) {
        val s = this.size.width
        if (tile != null) drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.24f))
        else drawRoundRect(Brush.linearGradient(listOf(LogoPaper, Color(0xFFECE6FF)), Offset(0f, 0f), Offset(s, s)), cornerRadius = CornerRadius(s * 0.24f))
        // Each part appears over its own slice of [build], easing in from an offset.
        fun part(from: Float, to: Float) = ((build - from) / (to - from)).coerceIn(0f, 1f)
        val u = s / 100f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        // Top bar and bowl, dropping in from above.
        val a = part(0f, 0.4f)
        if (a > 0f) translate(top = (1f - a) * -12f * u) {
            val bowl = Path().apply {
                moveTo(16 * u, 14 * u); lineTo(58 * u, 14 * u)
                cubicTo(72 * u, 14 * u, 82 * u, 24 * u, 82 * u, 36 * u)
                cubicTo(82 * u, 48 * u, 72 * u, 58 * u, 58 * u, 58 * u)
                lineTo(40 * u, 58 * u); lineTo(40 * u, 46 * u); lineTo(58 * u, 46 * u)
                cubicTo(64 * u, 46 * u, 69 * u, 42 * u, 69 * u, 36 * u)
                cubicTo(69 * u, 30 * u, 64 * u, 26 * u, 58 * u, 26 * u)
                lineTo(16 * u, 26 * u); close()
            }
            drawPath(bowl, LogoR.copy(alpha = a))
        }
        // The leg, striking down to the right.
        val b = part(0.25f, 0.6f)
        if (b > 0f) {
            val top = p(46f, 54f)
            val end = 54f + (88f - 54f) * b
            val dx = (end - 54f) * (26f / 34f)
            drawPath(Path().apply {
                moveTo(top.x, top.y); lineTo(60 * u, 54 * u); lineTo((60f + dx) * u, end * u); lineTo((46f + dx) * u, end * u); close()
            }, LogoR)
        }
        // The rupee's two bars, sliding in from the left.
        val c = part(0.5f, 0.8f)
        if (c > 0f) translate(left = (1f - c) * -14f * u) {
            drawRect(LogoRupee.copy(alpha = c), p(16f, 31f), Size(20 * u, 7 * u))
            drawRect(LogoRupee.copy(alpha = c), p(16f, 43f), Size(20 * u, 7 * u))
        }
        // And its tail, parallel to the R's leg.
        val d = part(0.7f, 1f)
        if (d > 0f) {
            val end = 54f + (88f - 54f) * d
            val dx = (end - 54f) * (24f / 34f)
            drawPath(Path().apply {
                moveTo(20 * u, 54 * u); lineTo(33 * u, 54 * u); lineTo((33f + dx) * u, end * u); lineTo((20f + dx) * u, end * u); close()
            }, LogoRupee)
        }
    }
}


/**
 * The opening: the white tile spins in, the R builds itself (top and bowl drop in, the leg strikes down, the rupee
 * bars slide in and the tail follows), and as it locks together a ring and a burst of confetti in the brand
 * colours fly out. Then each letter of "RUVO" hops up on a spring, and the line appears. About 2.5 s; a tap skips it. Always dark, so it reads as a cover. Reduced motion shows the
 * finished mark at once.
 */
@Composable
fun RuvoSplash(modifier: Modifier = Modifier, onSkip: () -> Unit = {}) {
    val reduced = Ruvo.reducedMotion
    val start = if (reduced) 1f else 0f
    val tile = remember { Animatable(start) }
    val build = remember { Animatable(start) }
    val burst = remember { Animatable(start) }
    val letters = remember { List(4) { Animatable(start) } }
    val line = remember { Animatable(start) }
    val hop = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.42f, stiffness = 420f)
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { tile.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 300f)) }
        launch { delay(280); build.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { delay(1050); burst.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        letters.forEachIndexed { i, l -> launch { delay(1150L + i * 80L); l.animateTo(1f, hop) } }
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
                // The middle of the 112 dp tile, where the R locks together.
                val land = centre
                val ringR = 14.dp.toPx() + 90.dp.toPx() * b
                drawCircle(LogoRupee.copy(alpha = (1f - b) * 0.8f), ringR, land, style = Stroke(3.dp.toPx() * (1f - b) + 0.5f))
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
                size = 112.dp, build = build.value,
            )
            Spacer(Modifier.height(Space.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                "RUVO".forEachIndexed { i, ch ->
                    val p = letters[i].value
                    Text(
                        ch.toString(),
                        Modifier.graphicsLayer {
                            alpha = p.coerceIn(0f, 1f)
                            // Each letter hops up from below.
                            translationY = (1f - p) * 26.dp.toPx()
                            val sc = 0.6f + 0.4f * p
                            scaleX = sc; scaleY = sc
                        },
                        style = Ruvo.type.display.copy(fontSize = Ruvo.type.display.fontSize * 1.2f, fontWeight = androidx.compose.ui.text.font.FontWeight(800)),
                        color = LogoPaper,
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
