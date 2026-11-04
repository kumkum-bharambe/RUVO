package com.ruvo.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LogoInk = Color(0xFF1C1A17)
private val LogoPaper = Color(0xFFF4EEE3)
private val LogoHaldi = Color(0xFFF0B232)
private val LogoNeela = Color(0xFF4A8EDB)

/** The Ruvo mark: three rising bars, paper, haldi and neela, on an ink tile. No coin. */
@Composable
fun RuvoLogo(modifier: Modifier = Modifier, size: Dp = 56.dp, tile: Color = LogoInk) {
    Canvas(modifier.size(size).semantics { contentDescription = "Ruvo" }) {
        val s = this.size.width
        drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.2f))
        val w = s * 0.1f
        val gap = s * 0.04f
        val base = s * 0.68f
        val x0 = (s - (3 * w + 2 * gap)) / 2
        listOf(0.16f to LogoPaper, 0.26f to LogoHaldi, 0.36f to LogoNeela).forEachIndexed { i, (h, col) ->
            val height = s * h
            drawRoundRect(col, Offset(x0 + i * (w + gap), base - height), Size(w, height), CornerRadius(w / 2))
        }
    }
}

/**
 * The splash: calm and dark. A soft haldi glow, the mark fading up, "ruvo." and its line, and a thin loading
 * line along the bottom. No bounce, no confetti. Always dark, whatever the mood, so it reads as a cover.
 */
@Composable
fun RuvoSplash(modifier: Modifier = Modifier) {
    val reduced = Ruvo.reducedMotion
    val mark = remember { Animatable(if (reduced) 1f else 0f) }
    val words = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        launch { mark.animateTo(1f, Motion.gentle()) }
        delay(260)
        words.animateTo(1f, Motion.gentle())
    }
    val loading = rememberInfiniteTransition(label = "loading")
    val sweep by loading.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    Box(
        modifier.fillMaxSize().background(Color(0xFF11131C)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(LogoHaldi.copy(alpha = 0.22f), Color.Transparent), Offset(size.width / 2, size.height * 0.42f), size.width * 0.8f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            RuvoLogo(
                Modifier.graphicsLayer { alpha = mark.value; val sc = 0.92f + 0.08f * mark.value; scaleX = sc; scaleY = sc },
                size = 84.dp, tile = Color(0xFF1C1A17),
            )
            Spacer(Modifier.height(Space.lg))
            Column(
                Modifier.graphicsLayer { alpha = words.value; translationY = (1f - words.value) * 24f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ruvo.", style = Ruvo.type.display, color = LogoPaper)
                Spacer(Modifier.height(Space.xxs))
                Text("Your money, without the mental load.", style = Ruvo.type.label, color = LogoPaper.copy(alpha = 0.6f))
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Space.xxl)
                .width(120.dp).height(2.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp)).background(LogoPaper.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier.fillMaxWidth(0.35f).height(2.dp)
                    .graphicsLayer { translationX = ((if (reduced) 0.3f else sweep) * 1.35f - 0.35f) * 120.dp.toPx() }
                    .background(LogoHaldi),
            )
        }
    }
}
