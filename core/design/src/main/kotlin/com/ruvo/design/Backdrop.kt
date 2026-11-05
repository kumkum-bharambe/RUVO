package com.ruvo.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The sky behind every screen. A gentle top-to-bottom wash (deeper periwinkle at the top by day, a deep night blue
 * fading to black at night) with four soft lights drifting on slow, out-of-step loops: a big blue one behind the
 * header, a warm sun top left, a blush pink at the side and a second blue low down. White glass cards float on it
 * and catch the colour through their frost. Reduced motion keeps the lights still.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val drift = rememberInfiniteTransition(label = "backdrop")
    val a by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(23_000, easing = LinearEasing), RepeatMode.Restart), label = "a")
    val b by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(31_000, easing = LinearEasing), RepeatMode.Restart), label = "b")
    Box(modifier.fillMaxSize().background(c.canvas)) {
        Canvas(Modifier.fillMaxSize()) { drawSky(c, if (reduced) 0.1f else a, if (reduced) 0.6f else b) }
        content()
    }
}

/** Paints Ruvo's sky into any area, e.g. behind a sheet. [ta] and [tb] are the two drift loops, 0..1. */
fun DrawScope.drawSky(c: RuvoColors, ta: Float, tb: Float) {
    val w = size.width
    val h = size.height
    // The wash: by day the whole height, at night only the top half, so the rest stays true black.
    drawRect(Brush.verticalGradient(listOf(c.canvasTop, c.canvas), startY = 0f, endY = if (c.isDark) h * 0.55f else h))
    fun glow(color: Color, x: Float, y: Float, r: Float) {
        val at = Offset(x, y)
        drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), at, r), r, at)
    }
    val pa = 2 * PI * ta
    val pb = 2 * PI * tb
    // Blue behind the header, swinging slowly across the top right.
    glow(c.glowCool, w * (0.92f + 0.08f * sin(pa).toFloat()), h * (0.04f + 0.04f * cos(pa).toFloat()), w * 0.95f)
    // A warm sun in the top left.
    glow(c.glowWarm, w * (0.04f + 0.10f * cos(pb).toFloat()), h * (0.12f + 0.04f * sin(pb).toFloat()), w * 0.8f)
    // Blush pink drifting up and down the left side, like the light inside a soap bubble.
    glow(c.glowPink, w * (0.02f + 0.06f * sin(pb).toFloat()), h * (0.48f + 0.08f * cos(pa + pb).toFloat()), w * 0.7f)
    // A second, quieter blue low on the right.
    glow(c.glowCool.copy(alpha = c.glowCool.alpha * 0.6f), w * (0.96f + 0.06f * cos(pb).toFloat()), h * (0.84f + 0.05f * sin(pa).toFloat()), w * 0.85f)
}

/** Fades content out under the status bar so scrolled text never collides with the clock. */
@Composable
fun BoxScope.StatusBarFade() {
    val c = Ruvo.colors
    // The top of the sky colour, a touch see-through, so the glow behind the header still shows.
    val top = c.canvasTop.copy(alpha = if (c.isDark) 0.94f else 0.86f)
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(top))
        Box(Modifier.fillMaxWidth().height(24.dp).background(Brush.verticalGradient(listOf(top, top.copy(alpha = 0f)))))
    }
}
