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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
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

/**
 * The Ruvo mark: a lowercase r whose shoulder lifts off like a runway, followed by a sun-yellow dot, the "."
 * of "ruvo." and the day the next money arrives. White stem, orange-to-yellow arc, on a night-blue tile that
 * warms to violet. [draw] builds it (stem first, then the arc), and [dot] pops the dot, which is how the
 * opening animates it; both are 1 when the mark is still.
 */
@Composable
fun RuvoLogo(modifier: Modifier = Modifier, size: Dp = 56.dp, tile: Color? = null, draw: Float = 1f, dot: Float = 1f) {
    Canvas(modifier.size(size).semantics { contentDescription = "Ruvo" }) {
        val s = this.size.width
        if (tile != null) drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.24f))
        else drawRoundRect(Brush.linearGradient(listOf(LogoViolet, LogoInk), Offset(0f, 0f), Offset(s, s)), cornerRadius = CornerRadius(s * 0.24f))
        val w = s * 0.11f
        val stemT = (draw / 0.4f).coerceIn(0f, 1f)
        val arcT = ((draw - 0.3f) / 0.7f).coerceIn(0f, 1f)
        if (stemT > 0f) {
            val bottom = s * 0.72f
            drawLine(LogoPaper, Offset(s * 0.36f, bottom), Offset(s * 0.36f, bottom - (bottom - s * 0.54f) * stemT), w, StrokeCap.Round)
        }
        if (arcT > 0f) {
            val arc = Path().apply {
                moveTo(s * 0.36f, s * 0.54f)
                cubicTo(s * 0.36f, s * 0.38f, s * 0.47f, s * 0.31f, s * 0.61f, s * 0.315f)
            }
            val measure = PathMeasure().apply { setPath(arc, false) }
            val part = Path().also { measure.getSegment(0f, measure.length * arcT, it, true) }
            drawPath(
                part, Brush.linearGradient(listOf(LogoOrange, LogoHaldi), Offset(s * 0.36f, s * 0.54f), Offset(s * 0.61f, s * 0.31f)),
                style = Stroke(w, cap = StrokeCap.Round),
            )
        }
        if (dot > 0f) drawCircle(LogoHaldi, s * 0.072f * dot, Offset(s * 0.745f, s * 0.31f))
    }
}


/**
 * The opening, after Ruvo 7: the tile pops in, the r draws itself (stem up, then the runway arc lifting off), the
 * sun dot pops with an amber ring pulsing out behind it, "ruvo." rises letter by letter (the dot is amber), then the line. About 2.4 s; a tap skips it.
 * Always dark, whatever the mood, so it reads as a cover. Reduced motion shows the finished mark at once.
 */
@Composable
fun RuvoSplash(modifier: Modifier = Modifier, onSkip: () -> Unit = {}) {
    val reduced = Ruvo.reducedMotion
    val start = if (reduced) 1f else 0f
    val tile = remember { Animatable(start) }
    val draw = remember { Animatable(start) }
    val dot = remember { Animatable(start) }
    val ring = remember { Animatable(start) }
    val letters = remember { List(5) { Animatable(start) } }
    val line = remember { Animatable(start) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { tile.animateTo(1f, Motion.bouncy()) }
        launch { delay(250); draw.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { delay(900); dot.animateTo(1f, Motion.bouncy()) }
        launch { delay(900); ring.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
        letters.forEachIndexed { i, l -> launch { delay(1000L + i * 60L); l.animateTo(1f, Motion.gentle()) } }
        launch { delay(1700); line.animateTo(1f, Motion.gentle()) }
    }
    Box(
        modifier.fillMaxSize().background(Color(0xFF0A0D26))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Skip", onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height * 0.42f)
            drawRect(Brush.radialGradient(listOf(Color(0xFF2A2272), Color(0xFF0A0D26)), centre, size.height * 0.6f))
            // The ring: a soft amber disc that swells and fades as the dot lands.
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
                size = 112.dp, draw = draw.value, dot = dot.value,
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
