package com.ruvo.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * The PIN so far as four dots. [shake] changes each time a PIN is wrong, which shakes the row once
 * (skipped under reduced motion). Dots fill with ink as digits arrive; they never show the digits.
 */
@Composable
fun PinDots(entered: Int, shake: Int, modifier: Modifier = Modifier, length: Int = 4, error: Boolean = false) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val x = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake == 0 || reduced) return@LaunchedEffect
        for (v in listOf(14f, -12f, 9f, -6f, 3f, 0f)) x.animateTo(v, Motion.snappy())
    }
    Row(
        modifier.graphicsLayer { translationX = x.value * density }
            .clearAndSetSemantics { contentDescription = "$entered of $length digits entered" },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        repeat(length) { i ->
            val on = i < entered
            val fill by animateColorAsState(
                when { error -> c.warning; on -> c.ink; else -> c.ink.copy(alpha = 0f) }, label = "dot$i",
            )
            Box(Modifier.size(14.dp).clip(CircleShape).background(fill).border(1.5.dp, if (error) c.warning else c.ink.copy(alpha = 0.5f), CircleShape))
        }
    }
}

/** A phone-style number pad: 1 to 9, 0 and delete, each a 72 dp circle with a light tick. */
@Composable
fun PinPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Ruvo.colors
    val view = LocalView.current
    val rows = listOf("123", "456", "789", " 0<")
    Column(modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f }, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                row.forEach { k ->
                    val base = Modifier.size(72.dp).clip(CircleShape)
                    when (k) {
                        ' ' -> Box(base)
                        '<' -> Box(
                            base.clickable(enabled = enabled, role = Role.Button, onClickLabel = "Delete last digit") { Haptics.tick(view); onDelete() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(RuvoIcons.Back, "Delete", Modifier.size(24.dp), tint = c.ink) }
                        else -> Box(
                            base.background(c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f))
                                .clickable(enabled = enabled, role = Role.Button) { Haptics.tick(view); onDigit(k) },
                            contentAlignment = Alignment.Center,
                        ) { Text(k.toString(), style = Ruvo.type.headline, color = c.ink) }
                    }
                }
            }
        }
    }
}
