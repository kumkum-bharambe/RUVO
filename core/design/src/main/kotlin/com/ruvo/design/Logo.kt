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

/** The R, Ruvo's deep royal blue; and the rupee strokes inside it, a bright sky blue. */
private val LogoR = Color(0xFF16216E)
private val LogoRupee = Color(0xFF4F7BFF)

/**
 * The Ruvo mark: a bold R whose left side is a rupee sign. The R's top bar, bowl and leg are deep royal blue; the
 * rupee's two bars and its diagonal tail are bright sky blue, running parallel to the R's leg. On a white tile
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
        else drawRoundRect(Brush.linearGradient(listOf(LogoPaper, Color(0xFFE6ECFF)), Offset(0f, 0f), Offset(s, s)), cornerRadius = CornerRadius(s * 0.24f))
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

