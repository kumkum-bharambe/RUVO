package com.ruvo.feature.orb

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruvo.design.Haptics
import com.ruvo.design.Motion
import com.ruvo.design.Ruvo
import com.ruvo.engine.RunwayResult
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** What the Orb shows. Derived from the runway so its mood is always honest. */
data class OrbMood(
    /** Liquid level, 0..1: how full the Fuel is relative to what it needs to cover. */
    val level: Float,
    /** 0 calm, 1 tight. Drives how fast the surface ripples. */
    val energy: Float,
    /** A goal just completed: the Orb glows. */
    val glow: Boolean,
) {
    companion object {
        fun from(r: RunwayResult, anyGoalComplete: Boolean): OrbMood {
            val next = r.daysToNextIncome
            val buffer = r.bufferDays
            val level = when {
                r.unlimited -> 0.85f
                r.fuel <= 0 -> 0.06f
                next == null -> (r.runwayDays / 30f).coerceIn(0.1f, 0.9f)
                next == 0 -> 0.8f
                else -> (r.runwayDays.toFloat() / next * 0.7f).coerceIn(0.08f, 0.92f)
            }
            val energy = when {
                !r.makesIt -> 1f
                buffer != null && buffer < 3 -> 0.55f
                else -> 0.1f
            }
            return OrbMood(level, energy, anyGoalComplete)
        }
    }
}

/**
 * The Ruvo Orb. One object replaces the mascot, the AI button and the capture button.
 * Tap to talk. Pull down to add a spend. Poke it and it ripples; drag it and it stretches like a droplet;
 * tilt the phone and the liquid leans. No face: motion and light carry the emotion.
 */
@Composable
fun RuvoOrb(
    mood: OrbMood,
    tilt: Float,
    onTalk: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reduced = Ruvo.reducedMotion
    val density = LocalDensity.current
    val pullThreshold = with(density) { 64.dp.toPx() }
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val poke = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    var pokeAt by remember { mutableStateOf(Offset.Zero) }
    var armed by remember { mutableStateOf(false) }

    val stretchY = 1f + (drag.value.y / 700f).coerceIn(-0.15f, 0.35f)
    val stretchX = 1f / stretchY.coerceAtLeast(0.6f)
    val glowPulse = rememberInfiniteTransition(label = "glow")
    val halo by glowPulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "halo")

    Box(
        modifier
            .graphicsLayer {
                translationX = drag.value.x
                translationY = drag.value.y
                scaleX = stretchX
                scaleY = stretchY
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
            .size(size)
            .semantics {
                role = Role.Button
                contentDescription = "Ruvo. Tap to ask a question. Pull down to add a spend."
                onClick(label = "Talk to Ruvo") { onTalk(); true }
                customActions = listOf(CustomAccessibilityAction("Add a spend") { onQuickAdd(); true })
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { at ->
                        pokeAt = at
                        scope.launch { poke.snapTo(1f); poke.animateTo(0f, tween(1100)) }
                    },
                    onTap = {
                        Haptics.tick(view)
                        if (!reduced) scope.launch { hop.snapTo(0f); hop.animateTo(1f, tween(520)); hop.snapTo(0f) }
                        onTalk()
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val pulled = drag.value.y > pullThreshold
                        scope.launch { drag.animateTo(Offset.Zero, Motion.bouncy()) }
                        if (pulled) { Haptics.confirm(view); onQuickAdd() }
                        armed = false
                    },
                    onDragCancel = { scope.launch { drag.animateTo(Offset.Zero, Motion.bouncy()) }; armed = false },
                ) { change, amount ->
                    change.consume()
                    // Resistance grows with distance, like pulling a droplet.
                    val next = drag.value + amount * (1f / (1f + drag.value.getDistance() / 400f))
                    scope.launch { drag.snapTo(next) }
                    val nowArmed = next.y > pullThreshold
                    if (nowArmed && !armed) Haptics.tick(view)
                    armed = nowArmed
                }
            },
    ) {
        // Ruvo 7's fox is the companion now. A tap makes it hop before the chat opens; a goal reached makes it glow.
        val hopY = with(density) { -22.dp.toPx() }
        if (mood.glow) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }) {
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD54F).copy(alpha = 0.55f * halo), Color.Transparent)))
            }
        }
        FoxPet(
            mood.fox(),
            Modifier.fillMaxSize().graphicsLayer {
                val p = hop.value
                translationY = hopY * sin(PI.toFloat() * p)
                val squash = if (p < 0.15f || p > 0.85f) 1f - 0.08f * sin(PI.toFloat() * (if (p < 0.15f) p / 0.15f else (1f - p) / 0.15f)) else 1f
                scaleX = 2f - squash; scaleY = squash
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
        )
    }
}

@Composable
private fun rememberTime(): Float {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        // The infinite variant lets UI tests treat the endless ripple as idle.
        while (true) withInfiniteAnimationFrameNanos { now -> t = (now - start) / 1_000_000_000f }
    }
    return t
}

@RequiresApi(33)
@Composable
private fun ShaderOrb(mood: OrbMood, tilt: Float, poke: Float, pokeAt: Offset, halo: Float) {
    val c = Ruvo.colors
    val shader = remember { runCatching { RuntimeShader(ORB_AGSL) }.getOrNull() }
    if (shader == null) {
        CanvasOrb(mood, tilt, poke, halo); return
    }
    val time = rememberTime()
    val level by animateFloatAsState(mood.level, Motion.soft(), label = "level")
    val lean by animateFloatAsState(tilt, Motion.bouncy(), label = "lean")
    val energy by animateFloatAsState(mood.energy, Motion.soft(), label = "energy")
    Canvas(Modifier.fillMaxSize()) {
        if (mood.glow) drawGlow(c.sun, halo)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("level", level)
        shader.setFloatUniform("tilt", lean)
        shader.setFloatUniform("energy", energy)
        shader.setFloatUniform("poke", poke)
        shader.setFloatUniform("pokeAt", pokeAt.x, pokeAt.y)
        shader.setColorUniform("deep", c.orbDeep.toArgb())
        shader.setColorUniform("glow", c.orbGlow.toArgb())
        shader.setColorUniform("core", c.orbCore.toArgb())
        shader.setColorUniform("glass", (if (c.isDark) c.raised else c.ink).toArgb())
        drawRect(ShaderBrush(shader))
    }
}

/** Fallback for Android 8–12 and reduced motion: the same idea drawn with paths. */
@Composable
private fun CanvasOrb(mood: OrbMood, tilt: Float, poke: Float, halo: Float) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val level by animateFloatAsState(mood.level, Motion.soft(), label = "level")
    val lean by animateFloatAsState(tilt, Motion.bouncy(), label = "lean")
    val wave = rememberInfiniteTransition(label = "orbwave")
    val phase by wave.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(if (mood.energy > 0.5f) 1600 else 3800, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        if (mood.glow) drawGlow(c.sun, halo)
        val r = size.minDimension / 2
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)) }
        clipPath(circle) {
            drawRect((if (c.isDark) c.raised else c.ink).copy(alpha = 0.9f))
            val top = size.height * (1f - level)
            val amp = if (reduced) 0f else (2.5f + mood.energy * 3f + poke * 5f) * density
            val p = Path().apply {
                moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width) {
                    val t = x / size.width
                    lineTo(x, top + (t - 0.5f) * lean * size.height * 0.35f + amp * sin(t * 2f * PI.toFloat() * 1.4f + phase))
                    x += 3f
                }
                lineTo(size.width, size.height); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(c.orbCore, c.orbGlow, c.orbDeep), startY = top - 4f, endY = size.height))
        }
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = Offset(r * 0.62f, r * 0.58f), radius = r * 0.42f), radius = r * 0.42f, center = Offset(r * 0.62f, r * 0.58f))
    }
}

private fun DrawScope.drawGlow(color: Color, halo: Float) {
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f * halo), Color.Transparent), center = center, radius = r * 1.45f), radius = r * 1.45f)
}

/**
 * AGSL for the liquid sphere. Everything is in float4 and converted once at the end, which keeps it
 * compiling on every Android 13+ GPU driver.
 */
private const val ORB_AGSL = """
uniform float2 size;
uniform float time;
uniform float level;
uniform float tilt;
uniform float energy;
uniform float poke;
uniform float2 pokeAt;
layout(color) uniform float4 deep;
layout(color) uniform float4 glow;
layout(color) uniform float4 core;
layout(color) uniform float4 glass;

half4 main(float2 p) {
    float2 uv = p / size;
    float2 c = uv - float2(0.5, 0.5);
    float r = length(c);
    if (r > 0.5) { return half4(0.0, 0.0, 0.0, 0.0); }

    float speed = mix(0.7, 2.6, energy);
    float surface = 1.0 - level + (uv.x - 0.5) * tilt * 0.38
        + (0.018 + energy * 0.02) * sin(uv.x * 8.0 + time * speed)
        + 0.012 * sin(uv.x * 17.0 - time * speed * 1.7);
    float d = length(p - pokeAt) / size.x;
    surface += poke * 0.05 * sin(d * 38.0 - time * 11.0) * exp(-d * 5.0);

    float4 col = float4(glass.rgb * 0.92, 1.0);
    if (uv.y > surface) {
        float depth = clamp((uv.y - surface) * 1.8, 0.0, 1.0);
        col = mix(glow, deep, depth);
        float lip = 1.0 - smoothstep(0.0, 0.03, abs(uv.y - surface));
        col = mix(col, core, lip * 0.85);
        float caustic = 0.06 * sin(uv.x * 23.0 + time * 1.3) * sin(uv.y * 19.0 - time * 0.9);
        col.rgb += caustic;
    }
    float shade = 1.0 - smoothstep(0.28, 0.5, r) * 0.38;
    col.rgb *= shade;
    float hi = 1.0 - smoothstep(0.0, 0.17, length(c - float2(-0.16, -0.18)));
    col.rgb += hi * 0.5;
    float rim = smoothstep(0.43, 0.5, r) * 0.25;
    col.rgb += rim;
    float edge = 1.0 - smoothstep(0.485, 0.5, r);
    return half4(col.rgb * edge, edge);
}
"""
