package com.ruvo.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruvo.engine.Bucket

/**
 * Ruvo's two moods.
 * Daybreak (light): warm rice-paper canvas, ink type, saturated accents that read like printed colour.
 * Night swim (dark): deep sea-blue canvas where the accents glow, as if lit from inside the liquid.
 * Every colour in the app comes from here.
 */
@Immutable
data class RuvoColors(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val raised: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val hairline: Color,
    /** Fixed: already promised. */
    val fixed: Color,
    /** Fuel: free to spend. Also the Orb's liquid. */
    val fuel: Color,
    /** Future: protected for goals. */
    val future: Color,
    val warning: Color,
    /** Highlights, perks and the goal-complete glow. */
    val sun: Color,
    /** Text and icons placed on an accent fill. */
    val onAccent: Color,
    val orbCore: Color,
    val orbGlow: Color,
    val orbDeep: Color,
    val scrim: Color,
) {
    fun bucket(b: Bucket): Color = when (b) {
        Bucket.FIXED -> fixed
        Bucket.FUEL -> fuel
        Bucket.FUTURE -> future
    }
}

val Daybreak = RuvoColors(
    isDark = false,
    canvas = Color(0xFFF4EFE7),
    surface = Color(0xFFFFFCF7),
    raised = Color(0xFFFFFFFF),
    ink = Color(0xFF16131D),
    inkSoft = Color(0xFF5A5464),
    inkFaint = Color(0xFF8E8898),
    hairline = Color(0xFFE5DDD0),
    fixed = Color(0xFF5B4BFF),
    fuel = Color(0xFF0FB894),
    future = Color(0xFFFF6A2B),
    warning = Color(0xFFD92D4A),
    sun = Color(0xFFFFB400),
    onAccent = Color(0xFF16131D),
    orbCore = Color(0xFF8BF7DD),
    orbGlow = Color(0xFF12C9A5),
    orbDeep = Color(0xFF07705D),
    scrim = Color(0x6616131D),
)

val NightSwim = RuvoColors(
    isDark = true,
    canvas = Color(0xFF070A12),
    surface = Color(0xFF0F141E),
    raised = Color(0xFF161D2A),
    ink = Color(0xFFEEF1F6),
    inkSoft = Color(0xFFA9B1BF),
    inkFaint = Color(0xFF6E7686),
    hairline = Color(0xFF222A38),
    fixed = Color(0xFF9A8CFF),
    fuel = Color(0xFF3CF2C6),
    future = Color(0xFFFF8F5A),
    warning = Color(0xFFFF5C78),
    sun = Color(0xFFFFD25E),
    onAccent = Color(0xFF06110E),
    orbCore = Color(0xFFC4FFF2),
    orbGlow = Color(0xFF3CF2C6),
    orbDeep = Color(0xFF0A5E52),
    scrim = Color(0x99000000),
)

/** Spacing on a 4 dp grid, plus the few radii the app uses. */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 28.dp
    val xxl = 40.dp
    /** Side gutter on every screen. */
    val gutter = 20.dp
    /** Space kept clear at the bottom for the floating dock and the Orb. */
    val dockClearance = 184.dp
}

object Radius {
    val card = 28.dp
    val inner = 18.dp
    val chip = 999.dp
    val sheet = 34.dp
}

val LocalRuvoColors = staticCompositionLocalOf { Daybreak }
