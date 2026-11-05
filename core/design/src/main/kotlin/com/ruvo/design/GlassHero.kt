package com.ruvo.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The big pane of white glass used for the most important number on a screen: the runway on Home and the balance
 * on Money. A warm sun glows in its top corner, a blue light pools at the bottom left and a blush of pink drifts
 * between them, all slowly breathing, with a sheen that sweeps across every few seconds. Milky by day, smoky at night.
 * Text inside uses ink colours. Reduced motion holds the lights still.
 */
@Composable
fun GlassHero(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val radius = Radius.card + 6.dp
    val shape = RoundedCornerShape(radius)
    val loop = rememberInfiniteTransition(label = "glassHero")
    val sheen by loop.animateFloat(-0.8f, 2.2f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "sheen")
    val glow by loop.animateFloat(0.75f, 1f, infiniteRepeatable(tween(3200), RepeatMode.Reverse), label = "glow")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "drift")
    val dark = c.isDark
    var m = modifier.fillMaxWidth()
        .then(if (dark) Modifier else Modifier.softShadow(radius, c.shadow, spread = 22.dp, drop = 10.dp))
        .clip(shape)
        .background(
            Brush.verticalGradient(
                if (dark) listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.05f))
                else listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.6f)),
            ),
        )
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.3f else 1f), Color.White.copy(alpha = if (dark) 0.05f else 0.4f))), shape)
    if (onClick != null) m = m.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
    Box(m) {
        Canvas(Modifier.matchParentSize()) {
            val g = if (reduced) 0.9f else glow
            val a = (if (reduced) 0.2f else drift) * 2f * PI.toFloat()
            fun light(color: Color, at: Offset, r: Float) = drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), at, r), r, at)
            light(c.fuel.copy(alpha = (if (dark) 0.2f else 0.5f) * g), Offset(size.width * (0.96f + 0.04f * cos(a)), size.height * (0.02f + 0.04f * sin(a))), size.width * 0.8f)
            light(c.brand.lighten(0.3f).copy(alpha = if (dark) 0.34f else 0.32f), Offset(size.width * (0.04f + 0.06f * sin(a)), size.height * 0.98f), size.width * 0.85f)
            light(Color(0xFFFF8FB8).copy(alpha = if (dark) 0.12f else 0.22f), Offset(size.width * (0.7f + 0.12f * cos(a)), size.height * (0.62f + 0.1f * sin(a))), size.width * 0.5f)
            if (!reduced) {
                val x = size.width * sheen
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.07f else 0.3f), Color.Transparent), Offset(x - size.width * 0.3f, 0f), Offset(x + size.width * 0.3f, size.height)))
            }
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.3f else 1f), Color.Transparent)), size = Size(size.width, 1.5.dp.toPx()))
        }
        content()
    }
}
