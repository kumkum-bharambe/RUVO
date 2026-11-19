package com.ruvo.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Where each part of the screen sits, so the tour can light it up. Parts register with [tourTarget]. */
object TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

/** Marks a part of the screen the first-run tour can point at. */
fun Modifier.tourTarget(id: String): Modifier = onGloballyPositioned { TourTargets.bounds[id] = it.boundsInWindow() }

/** One stop on the tour: the part it lights up (null for a card in the middle), a title and one or two lines. */
data class TourStep(val target: String?, val title: String, val body: String)

/**
 * The first-run tour, shown once after setup, like a design app's welcome walkthrough: the screen dims, one part
 * at a time is lit up in a rounded window, and a small card beside it says what it is. Next moves on, Skip ends it.
 * The last step can carry its own button (for example turning on notifications). Taps on the dimmed screen do
 * nothing, so nothing opens by accident. With reduced motion the window jumps instead of gliding.
 */
@Composable
fun TourOverlay(
    steps: List<TourStep>,
    onFinish: (completed: Boolean) -> Unit,
    finalAction: Pair<String, () -> Unit>? = null,
) {
    val c = Ruvo.colors
    val density = LocalDensity.current
    val reduced = Ruvo.reducedMotion
    var index by rememberSaveable { mutableStateOf(0) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val pad = with(density) { 8.dp.toPx() }
    val target = step.target?.let { TourTargets.bounds[it] }?.translate(-origin.x, -origin.y)?.inflate(pad)
    // The lit window glides from one part to the next (or jumps, with reduced motion).
    val spec = if (reduced) androidx.compose.animation.core.snap() else Motion.gentle<Float>()
    val l by animateFloatAsState(target?.left ?: 0f, spec, label = "l")
    val t by animateFloatAsState(target?.top ?: 0f, spec, label = "t")
    val r by animateFloatAsState(target?.right ?: 0f, spec, label = "r")
    val b by animateFloatAsState(target?.bottom ?: 0f, spec, label = "b")
    val last = index == steps.lastIndex

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            // Swallow taps on the dimmed screen.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = 0.66f))
            if (target != null) {
                // The lit window, punched out of the dim layer, with a soft white ring around it.
                val corner = CornerRadius(24.dp.toPx())
                drawRoundRect(Color.Transparent, Offset(l, t), Size(r - l, b - t), corner, blendMode = BlendMode.Clear)
                drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(l, t), Size(r - l, b - t), corner, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
        }
        // The card sits below the lit part when there is room, otherwise above it; a middle card when no target.
        val screenH = with(density) { maxHeight.toPx() }
        val cardGap = with(density) { 14.dp.toPx() }
        val estimate = with(density) { 210.dp.toPx() }
        val y = when {
            target == null -> (screenH - estimate) / 2f
            target.bottom + cardGap + estimate < screenH -> target.bottom + cardGap
            else -> (target.top - cardGap - estimate).coerceAtLeast(with(density) { 40.dp.toPx() })
        }
        Column(
            Modifier.offset { IntOffset(0, y.toInt()) }.padding(horizontal = Space.gutter).fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            RuvoCard(Modifier.fillMaxWidth(), elevated = true) {
                Text("${index + 1} of ${steps.size}", style = Ruvo.type.caption, color = c.inkSoft)
                Spacer(Modifier.height(Space.xxs))
                Text(step.title, style = Ruvo.type.title, color = c.ink)
                Spacer(Modifier.height(Space.xxs))
                Text(step.body, style = Ruvo.type.body, color = c.inkSoft)
                Spacer(Modifier.height(Space.sm))
                // Progress dots: where you are on the tour.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { i ->
                        Box(Modifier.size(if (i == index) 18.dp else 6.dp, 6.dp).clip(CircleShape).background(if (i == index) c.brand else c.ink.copy(alpha = 0.2f)))
                    }
                }
                Spacer(Modifier.height(Space.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!last) {
                        GhostButton("Skip", { onFinish(false) }, Modifier.weight(1f))
                        Spacer(Modifier.width(Space.xs))
                        PrimaryButton("Next", { index++ }, Modifier.weight(1f))
                    } else if (finalAction != null) {
                        GhostButton("Not now", { onFinish(true) }, Modifier.weight(1f))
                        Spacer(Modifier.width(Space.xs))
                        PrimaryButton(finalAction.first, { finalAction.second(); onFinish(true) }, Modifier.weight(1.3f))
                    } else {
                        PrimaryButton("Done", { onFinish(true) }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
