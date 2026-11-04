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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import kotlin.random.Random
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The page behind every screen: warm paper (or ink-blue night) with a fine printed grain and a faint warmth in the
 * top corner, like light falling on a desk. It is still on purpose: the content moves, the page does not.
 * The grain is drawn once per size and cached.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val c = Ruvo.colors
    Box(
        modifier.fillMaxSize().background(c.canvas).drawWithCache {
            // One 256 px tile of grain, drawn once and repeated as a pattern.
            val tile = ImageBitmap(256, 256)
            val pen = Canvas(tile)
            val paint = Paint()
            val random = Random(42)
            val grain = if (c.isDark) Color.White else Color(0xFF5A4A30)
            repeat(260) {
                val a = random.nextFloat()
                paint.color = grain.copy(alpha = 0.04f + a * 0.06f)
                pen.drawCircle(Offset(random.nextFloat() * 256f, random.nextFloat() * 256f), 0.7f + a * 0.9f, paint)
            }
            val paperGrain = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
            onDrawBehind {
                drawRect(Brush.radialGradient(listOf(c.glowWarm, Color.Transparent), Offset(size.width * 0.1f, 0f), size.width * 1.1f))
                drawRect(Brush.radialGradient(listOf(c.glowCool, Color.Transparent), Offset(size.width, size.height), size.width))
                drawRect(paperGrain)
            }
        },
    ) {
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
