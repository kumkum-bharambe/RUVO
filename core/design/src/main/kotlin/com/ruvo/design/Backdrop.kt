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
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The glowing canvas behind every screen: an amber dawn glow near the top left and a lavender dusk glow
 * near the bottom right. They drift on slow, out-of-step loops (23 s and 31 s) so the screen feels alive
 * without anything asking for attention. Reduced motion keeps them still.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val drift = rememberInfiniteTransition(label = "backdrop")
    val a by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(23_000, easing = LinearEasing), RepeatMode.Restart), label = "a")
    val b by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(31_000, easing = LinearEasing), RepeatMode.Restart), label = "b")
    Box(modifier.fillMaxSize().background(c.canvas)) {
        Canvas(Modifier.fillMaxSize()) {
            val ta = if (reduced) 0.1f else a
            val tb = if (reduced) 0.6f else b
            val warm = Offset(
                size.width * (0.05f + 0.10f * cos(2 * PI * ta).toFloat()),
                size.height * (0.10f + 0.05f * sin(2 * PI * ta).toFloat()),
            )
            val cool = Offset(
                size.width * (0.95f + 0.08f * sin(2 * PI * tb).toFloat()),
                size.height * (0.86f + 0.05f * cos(2 * PI * tb).toFloat()),
            )
            val r = size.width * 0.95f
            drawRect(Brush.radialGradient(listOf(c.glowWarm, c.glowWarm.copy(alpha = 0f)), warm, r))
            drawRect(Brush.radialGradient(listOf(c.glowCool, c.glowCool.copy(alpha = 0f)), cool, r * 1.1f))
            val mid = Offset(size.width * (0.7f + 0.1f * cos(2 * PI * (ta + tb)).toFloat()), size.height * 0.42f)
            drawRect(Brush.radialGradient(listOf(c.glowCool.copy(alpha = c.glowCool.alpha * 0.35f), Color.Transparent), mid, r * 0.6f))
        }
        content()
    }
}

/** Fades content out under the status bar so scrolled text never collides with the clock. */
@Composable
fun BoxScope.StatusBarFade() {
    val c = Ruvo.colors
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(c.canvas.copy(alpha = 0.94f)))
        Box(Modifier.fillMaxWidth().height(28.dp).background(Brush.verticalGradient(listOf(c.canvas.copy(alpha = 0.94f), c.canvas.copy(alpha = 0f)))))
    }
}
