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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LogoInk = Color(0xFF0F1B3D)
private val LogoPaper = Color(0xFFFFFFFF)
private val LogoHaldi = Color(0xFFFDB913)
private val LogoNeela = Color(0xFF4A9BEA)

/**
 * The Ruvo mark: three rising bars, white, amber and blue, on a navy tile. No coin. [bars] grows each bar from
 * 0 to its full height, which is how the splash builds the mark.
 */
@Composable
fun RuvoLogo(modifier: Modifier = Modifier, size: Dp = 56.dp, tile: Color = LogoInk, bars: List<Float> = FULL) {
    Canvas(modifier.size(size).semantics { contentDescription = "Ruvo" }) {
        val s = this.size.width
        drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.2f))
        val w = s * 0.1f
        val gap = s * 0.04f
        val base = s * 0.68f
        val x0 = (s - (3 * w + 2 * gap)) / 2
        listOf(0.16f to LogoPaper, 0.26f to LogoHaldi, 0.36f to LogoNeela).forEachIndexed { i, (h, col) ->
            val height = s * h * bars.getOrElse(i) { 1f }.coerceIn(0f, 1.2f)
            if (height > 0.5f) drawRoundRect(col, Offset(x0 + i * (w + gap), base - height), Size(w, height), CornerRadius(w / 2))
        }
    }
}

private val FULL = listOf(1f, 1f, 1f)

/**
 * The opening, after Ruvo 7: the tile pops in, its three bars rise one after another, an amber ring pulses out
 * behind it, "ruvo." rises letter by letter (the dot is amber), then the line. About 2.4 s; a tap skips it.
 * Always dark, whatever the mood, so it reads as a cover. Reduced motion shows the finished mark at once.
 */
@Composable
fun RuvoSplash(modifier: Modifier = Modifier, onSkip: () -> Unit = {}) {
    val reduced = Ruvo.reducedMotion
    val start = if (reduced) 1f else 0f
    val tile = remember { Animatable(start) }
    val bars = remember { List(3) { Animatable(start) } }
    val ring = remember { Animatable(start) }
    val letters = remember { List(5) { Animatable(start) } }
    val line = remember { Animatable(start) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { tile.animateTo(1f, Motion.bouncy()) }
        bars.forEachIndexed { i, b -> launch { delay(300L + i * 200L); b.animateTo(1f, Motion.bouncy()) } }
        launch { delay(500); ring.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
        letters.forEachIndexed { i, l -> launch { delay(1000L + i * 60L); l.animateTo(1f, Motion.gentle()) } }
        launch { delay(1700); line.animateTo(1f, Motion.gentle()) }
    }
    Box(
        modifier.fillMaxSize().background(Color(0xFF070B1C))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Skip", onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height * 0.42f)
            drawRect(Brush.radialGradient(listOf(Color(0xFF17245A), Color(0xFF070B1C)), centre, size.height * 0.6f))
            // The ring: a soft amber disc that swells and fades as the bars finish rising.
            val r = 70.dp.toPx() * (0.6f + 1.6f * ring.value)
            val a = (1f - ring.value) * 0.55f
            if (a > 0.01f) {
                drawCircle(LogoHaldi.copy(alpha = a * 0.5f), r, centre)
                drawCircle(LogoHaldi.copy(alpha = a), r, centre, style = Stroke(3.dp.toPx()))
            }
        }
        Column(
            Modifier.padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            RuvoLogo(
                Modifier.graphicsLayer {
                    val sc = 0.55f + 0.45f * tile.value
                    scaleX = sc; scaleY = sc; alpha = tile.value.coerceIn(0f, 1f)
                },
                size = 112.dp, tile = Color(0xFF101010), bars = bars.map { it.value },
            )
            Spacer(Modifier.height(Space.lg))
            Row {
                "ruvo.".forEachIndexed { i, ch ->
                    val p = letters[i].value
                    Text(
                        ch.toString(),
                        Modifier.graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = (1f - p) * 18.dp.toPx() },
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
