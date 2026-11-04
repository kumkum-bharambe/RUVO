package com.ruvo.feature.goals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Each goal gets a small picture of what it's for instead of a jar: a Goa beach for a trip, a laptop on a desk,
 * a phone, an umbrella over coins for an emergency cushion, books and a cap for fees, a mountain for anything
 * else. Night swim turns the sky to night. The scenes move a little (waves, steam, rain, a waving flag) and
 * lean with the phone's tilt; reduced motion keeps them still.
 */
enum class GoalScene(val label: String) { BEACH("Trip"), LAPTOP("Laptop"), PHONE("Phone"), CUSHION("Cushion"), STUDY("Study"), MOUNTAIN("Big goal") }

/** Shapes of 10 and above are a picture the user picked; below 10 the picture follows the goal's name. */
const val PICKED_SCENE = 10

fun sceneFor(name: String, shape: Int): GoalScene {
    if (shape >= PICKED_SCENE) return GoalScene.entries.getOrElse(shape - PICKED_SCENE) { GoalScene.MOUNTAIN }
    val n = name.lowercase()
    fun has(vararg words: String) = words.any { it in n }
    return when {
        has("goa", "trip", "travel", "beach", "holiday", "vacation", "tour", "manali", "kerala") -> GoalScene.BEACH
        has("laptop", "macbook", "computer", "ipad", "tablet") -> GoalScene.LAPTOP
        has("phone", "iphone", "mobile", "pixel", "oneplus") -> GoalScene.PHONE
        has("emergency", "cushion", "safety", "rainy", "backup", "buffer") -> GoalScene.CUSHION
        has("course", "fee", "college", "exam", "study", "class", "book", "semester") -> GoalScene.STUDY
        else -> GoalScene.MOUNTAIN
    }
}

private class Sky(val top: Color, val bottom: Color, val night: Boolean)

@Composable
fun GoalIllustration(scene: GoalScene, tilt: Float, modifier: Modifier, complete: Boolean = false) {
    val dark = Ruvo.colors.isDark
    val reduced = Ruvo.reducedMotion
    val loop = rememberInfiniteTransition(label = "scene")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(7_000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val lean by animateFloatAsState(tilt, Motion.bouncy(), label = "lean")
    val phase = if (reduced) 0.2f else t
    val sky = when {
        dark -> Sky(Color(0xFF0A1236), Color(0xFF26346E), true)
        scene == GoalScene.BEACH -> Sky(Color(0xFF7CC8F5), Color(0xFFFFE6BF), false)
        else -> Sky(Color(0xFFCFE6FA), Color(0xFFFFF2DA), false)
    }
    Canvas(modifier.clip(RoundedCornerShape(Radius.inner))) {
        drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom)))
        val s = min(size.width, size.height * 1.25f)
        drawSun(sky, s, phase, complete)
        when (scene) {
            GoalScene.BEACH -> beach(sky, s, phase, lean)
            GoalScene.LAPTOP -> laptop(sky, s, phase)
            GoalScene.PHONE -> phone(sky, s, phase, lean)
            GoalScene.CUSHION -> cushion(sky, s, phase)
            GoalScene.STUDY -> study(sky, s, lean)
            GoalScene.MOUNTAIN -> mountain(sky, s, phase, lean)
        }
    }
}

private fun wave(t: Float) = sin(t * 2f * PI.toFloat())

private fun DrawScope.drawSun(sky: Sky, s: Float, t: Float, complete: Boolean) {
    val c = Offset(size.width * 0.2f, size.height * 0.26f)
    val r = s * 0.1f
    if (sky.night) {
        drawCircle(Brush.radialGradient(listOf(Color(0x40FFF1B8), Color(0x00FFF1B8)), c, r * 2.4f), r * 2.4f, c)
        drawCircle(Color(0xFFFFF1B8), r, c)
        drawCircle(sky.top, r * 0.86f, c + Offset(r * 0.45f, -r * 0.3f))
        listOf(0.42f to 0.12f, 0.62f to 0.22f, 0.86f to 0.1f, 0.52f to 0.34f, 0.94f to 0.3f).forEachIndexed { i, (x, y) ->
            val twinkle = 0.45f + 0.55f * ((wave(t + i * 0.23f) + 1f) / 2f)
            drawCircle(Color.White.copy(alpha = twinkle), s * 0.009f, Offset(size.width * x, size.height * y))
        }
    } else {
        val glow = 1f + 0.08f * wave(t) + if (complete) 0.4f else 0f
        drawCircle(Brush.radialGradient(listOf(Color(0x80FDB913), Color(0x00FDB913)), c, r * 2.6f * glow), r * 2.6f * glow, c)
        drawCircle(Color(0xFFFDB913), r, c)
    }
}

private fun DrawScope.beach(sky: Sky, s: Float, t: Float, lean: Float) {
    val w = size.width
    val h = size.height
    val seaTop = h * 0.56f
    val sandTop = h * 0.74f
    val sea = if (sky.night) Color(0xFF1B3B7C) else Color(0xFF1F8FD1)
    val seaDeep = if (sky.night) Color(0xFF102657) else Color(0xFF136FAE)
    drawRect(Brush.verticalGradient(listOf(seaDeep, sea), startY = seaTop, endY = sandTop), Offset(0f, seaTop), Size(w, sandTop - seaTop))
    // Two rolling lines of surf.
    for (k in 0..1) {
        val y0 = seaTop + (sandTop - seaTop) * (0.35f + k * 0.35f)
        val p = Path().apply {
            moveTo(0f, y0)
            var x = 0f
            while (x <= w) { lineTo(x, y0 + wave(x / (s * 0.35f) + t * (if (k == 0) 1f else -1f)) * s * 0.008f); x += 6f }
        }
        drawPath(p, Color.White.copy(alpha = if (sky.night) 0.25f else 0.45f), style = Stroke(s * 0.008f, cap = StrokeCap.Round))
    }
    // A little sailboat drifting across.
    val bx = w * (0.15f + 0.5f * ((t * 0.5f) % 1f))
    val by = seaTop + (sandTop - seaTop) * 0.3f + wave(t * 2f) * s * 0.006f
    drawPath(Path().apply { moveTo(bx - s * 0.06f, by); lineTo(bx + s * 0.06f, by); lineTo(bx + s * 0.04f, by + s * 0.025f); lineTo(bx - s * 0.04f, by + s * 0.025f); close() }, Color(0xFF0F1B3D))
    drawPath(Path().apply { moveTo(bx, by - s * 0.01f); lineTo(bx, by - s * 0.1f); lineTo(bx + s * 0.05f, by - s * 0.01f); close() }, Color.White)
    // Sand with a foamy edge.
    val sand = if (sky.night) Color(0xFF8E7656) else Color(0xFFF7D9A3)
    val sandPath = Path().apply {
        moveTo(0f, h)
        lineTo(0f, sandTop)
        var x = 0f
        while (x <= w) { lineTo(x, sandTop + wave(x / (s * 0.5f) + t) * s * 0.012f); x += 6f }
        lineTo(w, h); close()
    }
    drawPath(sandPath, sand)
    drawPath(sandPath, Color.White.copy(alpha = 0.5f), style = Stroke(s * 0.01f))
    // Palm tree leaning in the breeze and with the phone.
    val sway = wave(t) * s * 0.012f + lean * s * 0.04f
    val base = Offset(w * 0.78f, h * 0.97f)
    val top = Offset(w * 0.78f - s * 0.08f + sway, h * 0.34f)
    val trunk = Path().apply { moveTo(base.x, base.y); quadraticTo(base.x + s * 0.06f, (base.y + top.y) / 2, top.x, top.y) }
    drawPath(trunk, if (sky.night) Color(0xFF5B3A22) else Color(0xFF8B5A2B), style = Stroke(s * 0.04f, cap = StrokeCap.Round))
    val leaf = if (sky.night) Color(0xFF1E5C40) else Color(0xFF2FA45E)
    listOf(-150f, -110f, -60f, -20f, 20f).forEachIndexed { i, a ->
        rotate(a + wave(t + i * 0.1f) * 4f + lean * 8f, top) {
            val len = s * (0.2f - (i % 2) * 0.03f)
            drawPath(
                Path().apply {
                    moveTo(top.x, top.y)
                    quadraticTo(top.x + len * 0.5f, top.y - len * 0.22f, top.x + len, top.y + len * 0.12f)
                    quadraticTo(top.x + len * 0.5f, top.y + len * 0.02f, top.x, top.y)
                    close()
                },
                leaf,
            )
        }
    }
    drawCircle(Color(0xFF6B4423), s * 0.018f, top + Offset(s * 0.01f, s * 0.02f))
    drawCircle(Color(0xFF6B4423), s * 0.016f, top + Offset(-s * 0.02f, s * 0.025f))
    // A beach umbrella on the left.
    val ux = w * 0.3f
    val uy = sandTop + s * 0.02f
    drawLine(Color.White, Offset(ux, uy - s * 0.1f), Offset(ux, h * 0.95f), s * 0.012f, StrokeCap.Round)
    drawArc(Color(0xFFF26B1D), 180f, 180f, true, Offset(ux - s * 0.11f, uy - s * 0.19f), Size(s * 0.22f, s * 0.18f))
    drawArc(Color.White, 240f, 60f, true, Offset(ux - s * 0.11f, uy - s * 0.19f), Size(s * 0.22f, s * 0.18f))
}

private fun DrawScope.laptop(sky: Sky, s: Float, t: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.54f
    val desk = h * 0.8f
    drawRect(if (sky.night) Color(0xFF2D2640) else Color(0xFFE6C79E), Offset(0f, desk), Size(w, h - desk))
    drawRect(Color.Black.copy(alpha = 0.08f), Offset(0f, desk), Size(w, s * 0.015f))
    val sw = s * 0.62f
    val sh = s * 0.4f
    val left = cx - sw / 2
    val top = desk - s * 0.03f - sh
    drawRoundRect(Color(0xFF1F2533), Offset(left, top), Size(sw, sh), CornerRadius(s * 0.03f))
    val inset = s * 0.022f
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFF1B2A6B), Color(0xFF4A9BEA)), Offset(left, top), Offset(left + sw, top + sh)),
        Offset(left + inset, top + inset), Size(sw - inset * 2, sh - inset * 2), CornerRadius(s * 0.015f),
    )
    // Three rising bars on the screen: Ruvo's own mark.
    val bw = sw * 0.07f
    listOf(0.35f to Color.White, 0.55f to Color(0xFFFDB913), 0.8f to Color(0xFF8CC8FF)).forEachIndexed { i, (frac, col) ->
        val bh = sh * 0.5f * frac
        drawRoundRect(col, Offset(cx - bw * 2f + i * bw * 1.5f, top + sh * 0.75f - bh), Size(bw, bh), CornerRadius(bw / 2))
    }
    drawPath(Path().apply {
        moveTo(cx - s * 0.4f, desk); lineTo(cx + s * 0.4f, desk); lineTo(cx + s * 0.35f, desk - s * 0.035f); lineTo(cx - s * 0.35f, desk - s * 0.035f); close()
    }, Color(0xFFC9CED8))
    // A mug with curling steam.
    val mx = w * 0.12f + s * 0.02f
    drawRoundRect(Color(0xFFFDB913), Offset(mx, desk - s * 0.12f), Size(s * 0.09f, s * 0.11f), CornerRadius(s * 0.02f))
    drawArc(Color(0xFFFDB913), -90f, 180f, false, Offset(mx + s * 0.065f, desk - s * 0.1f), Size(s * 0.05f, s * 0.05f), style = Stroke(s * 0.014f))
    for (k in 0..1) {
        val x0 = mx + s * (0.025f + k * 0.035f)
        val p = Path().apply {
            moveTo(x0, desk - s * 0.14f)
            for (j in 1..8) lineTo(x0 + wave(t * 2f + j * 0.12f + k * 0.3f) * s * 0.012f, desk - s * 0.14f - j * s * 0.012f)
        }
        drawPath(p, (if (sky.night) Color.White else Color(0xFF0F1B3D)).copy(alpha = 0.35f), style = Stroke(s * 0.008f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.phone(sky: Sky, s: Float, t: Float, lean: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.55f
    val ground = h * 0.9f
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(cx - s * 0.2f, ground - s * 0.02f), Size(s * 0.4f, s * 0.05f))
    // Floating bubbles behind the phone.
    listOf(0.2f to 0.55f, 0.82f to 0.4f, 0.9f to 0.72f).forEachIndexed { i, (x, y) ->
        drawCircle(Color.White.copy(alpha = if (sky.night) 0.14f else 0.55f), s * (0.03f + i * 0.01f), Offset(w * x, h * y + wave(t + i * 0.3f) * s * 0.02f))
    }
    val pw = s * 0.32f
    val ph = s * 0.62f
    val float = wave(t) * s * 0.015f
    rotate(-8f + lean * 6f, Offset(cx, ground - ph / 2)) {
        val left = cx - pw / 2
        val top = ground - s * 0.06f - ph + float
        drawRoundRect(Color(0xFF111318), Offset(left, top), Size(pw, ph), CornerRadius(s * 0.05f))
        val i = s * 0.014f
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFB79CF2), Color(0xFF4A9BEA)), top, top + ph),
            Offset(left + i, top + i), Size(pw - i * 2, ph - i * 2), CornerRadius(s * 0.04f),
        )
        drawRoundRect(Color(0xFF111318), Offset(cx - pw * 0.18f, top + i * 2f), Size(pw * 0.36f, s * 0.022f), CornerRadius(s * 0.011f))
        val cell = (pw - i * 2) / 4f
        for (r in 0..2) for (col in 0..2) {
            drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(left + i + cell * (0.5f + col * 1.05f), top + ph * 0.22f + r * cell * 1.1f), Size(cell * 0.75f, cell * 0.75f), CornerRadius(cell * 0.22f))
        }
    }
}

private fun DrawScope.cushion(sky: Sky, s: Float, t: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.52f
    val ground = h * 0.9f
    val canopyLeft = cx - s * 0.3f
    val canopyRight = cx + s * 0.3f
    val canopyTop = h * 0.2f
    // Rain everywhere except under the umbrella.
    val rain = if (sky.night) Color(0xFF8CC8FF) else Color(0xFF4A9BEA)
    for (i in 0 until 14) {
        val x = w * ((i * 0.137f + 0.05f) % 1f)
        if (x > canopyLeft - s * 0.02f && x < canopyRight + s * 0.02f) continue
        val y = h * (((t * 1.6f) + i * 0.29f) % 1f)
        drawLine(rain.copy(alpha = 0.6f), Offset(x, y), Offset(x - s * 0.015f, y + s * 0.06f), s * 0.01f, StrokeCap.Round)
    }
    drawRect(if (sky.night) Color(0xFF233057) else Color(0xFFD9E8F5), Offset(0f, ground), Size(w, h - ground))
    // Coins stacked safely underneath.
    for (k in 0 until 4) {
        val y = ground - s * 0.035f * (k + 1)
        drawOval(Color(0xFFB86E06), Offset(cx - s * 0.11f, y + s * 0.012f), Size(s * 0.22f, s * 0.06f))
        drawOval(Color(0xFFFDB913), Offset(cx - s * 0.11f, y), Size(s * 0.22f, s * 0.06f))
    }
    // Handle and canopy.
    drawLine(Color(0xFF0F1B3D), Offset(cx, canopyTop + s * 0.1f), Offset(cx, ground - s * 0.2f), s * 0.016f, StrokeCap.Round)
    drawArc(Color(0xFF0F1B3D), 0f, 180f, false, Offset(cx, ground - s * 0.24f), Size(s * 0.07f, s * 0.07f), style = Stroke(s * 0.016f, cap = StrokeCap.Round))
    val canopy = Path().apply {
        moveTo(canopyLeft, canopyTop + s * 0.16f)
        cubicTo(canopyLeft, canopyTop - s * 0.04f, canopyRight, canopyTop - s * 0.04f, canopyRight, canopyTop + s * 0.16f)
        val scallops = 4
        val step = (canopyRight - canopyLeft) / scallops
        for (k in scallops - 1 downTo 0) {
            val x1 = canopyLeft + step * k
            quadraticTo(x1 + step / 2, canopyTop + s * 0.1f, x1, canopyTop + s * 0.16f)
        }
        close()
    }
    drawPath(canopy, Color(0xFFF26B1D))
    drawPath(canopy, Color.White.copy(alpha = 0.18f), style = Stroke(s * 0.008f))
}

private fun DrawScope.study(sky: Sky, s: Float, lean: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val desk = h * 0.88f
    drawRect(if (sky.night) Color(0xFF2D2640) else Color(0xFFE6C79E), Offset(0f, desk), Size(w, h - desk))
    val books = listOf(Color(0xFF4A9BEA) to 0.56f, Color(0xFFB79CF2) to 0.5f, Color(0xFFF26B1D) to 0.6f)
    var y = desk
    books.forEachIndexed { i, (col, bw) ->
        val bh = s * 0.09f
        val x = cx - s * bw / 2 + (i - 1) * s * 0.02f
        y -= bh
        drawRoundRect(col, Offset(x, y), Size(s * bw, bh), CornerRadius(s * 0.012f))
        drawRect(Color.White.copy(alpha = 0.85f), Offset(x + s * bw - s * 0.06f, y + bh * 0.2f), Size(s * 0.04f, bh * 0.6f))
        drawLine(Color.Black.copy(alpha = 0.12f), Offset(x + s * 0.04f, y + bh * 0.5f), Offset(x + s * bw * 0.6f, y + bh * 0.5f), s * 0.006f)
    }
    // Graduation cap with a tassel that swings with the phone.
    val capTop = y - s * 0.07f
    drawPath(Path().apply { moveTo(cx - s * 0.12f, capTop + s * 0.05f); lineTo(cx + s * 0.12f, capTop + s * 0.05f); lineTo(cx + s * 0.1f, y); lineTo(cx - s * 0.1f, y); close() }, Color(0xFF0F1B3D))
    drawPath(Path().apply { moveTo(cx, capTop - s * 0.03f); lineTo(cx + s * 0.24f, capTop + s * 0.03f); lineTo(cx, capTop + s * 0.09f); lineTo(cx - s * 0.24f, capTop + s * 0.03f); close() }, Color(0xFF16244F))
    val tx = cx + s * 0.18f + lean * s * 0.03f
    drawLine(Color(0xFFFDB913), Offset(cx, capTop + s * 0.03f), Offset(cx + s * 0.18f, capTop + s * 0.04f), s * 0.01f, StrokeCap.Round)
    drawLine(Color(0xFFFDB913), Offset(cx + s * 0.18f, capTop + s * 0.04f), Offset(tx, capTop + s * 0.14f), s * 0.012f, StrokeCap.Round)
    drawCircle(Color(0xFFFDB913), s * 0.016f, Offset(tx, capTop + s * 0.15f))
}

private fun DrawScope.mountain(sky: Sky, s: Float, t: Float, lean: Float) {
    val w = size.width
    val h = size.height
    val back = if (sky.night) Color(0xFF3A3F7A) else Color(0xFFB79CF2)
    val front = if (sky.night) Color(0xFF1B2556) else Color(0xFF0F1B3D)
    drawPath(Path().apply { moveTo(w * 0.05f, h); lineTo(w * 0.35f, h * 0.4f); lineTo(w * 0.62f, h); close() }, back)
    val peak = Offset(w * 0.62f, h * 0.24f)
    drawPath(Path().apply { moveTo(w * 0.28f, h); lineTo(peak.x, peak.y); lineTo(w * 1.0f, h); close() }, front)
    drawPath(Path().apply {
        moveTo(peak.x, peak.y); lineTo(peak.x + s * 0.09f, peak.y + s * 0.13f); lineTo(peak.x + s * 0.03f, peak.y + s * 0.1f)
        lineTo(peak.x - s * 0.02f, peak.y + s * 0.14f); lineTo(peak.x - s * 0.08f, peak.y + s * 0.11f); close()
    }, Color.White)
    drawPath(Path().apply { moveTo(0f, h); quadraticTo(w * 0.3f, h * 0.8f, w * 0.6f, h * 0.92f); quadraticTo(w * 0.85f, h * 1.0f, w, h * 0.86f); lineTo(w, h); close() }, if (sky.night) Color(0xFF1F5C40) else Color(0xFF7BC47F))
    // A flag at the top, waving.
    val pole = peak - Offset(0f, s * 0.16f)
    drawLine(Color(0xFFFFFFFF), peak, pole, s * 0.012f, StrokeCap.Round)
    val flap = wave(t * 2f) * s * 0.015f + lean * s * 0.02f
    drawPath(Path().apply {
        moveTo(pole.x, pole.y)
        quadraticTo(pole.x + s * 0.06f, pole.y + flap, pole.x + s * 0.12f, pole.y + s * 0.02f)
        quadraticTo(pole.x + s * 0.06f, pole.y + s * 0.05f + flap, pole.x, pole.y + s * 0.07f)
        close()
    }, Color(0xFFFDB913))
}
