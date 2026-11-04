package com.ruvo.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruvo.engine.Bucket

/**
 * Ruvo's two moods in the "Ticket & Ledger" language: Indian print culture, newsprint and rubber stamps.
 * Daybreak (light): warm paper, ink type, ruled lines; the runway ticket is printed in ink.
 * Night swim (dark): ink-blue night with paper-coloured type; the runway ticket turns to paper.
 *
 * Colours are named after things every Indian student knows:
 *   Haldi (turmeric) = Fuel, yours to spend. Peacock = Future, protected. Neel (indigo) = Fixed, already planned.
 *   Sindoor (vermilion) = shortfalls and the stamp, never blame.
 * Every colour in the app comes from here.
 */@Immutable
data class RuvoColors(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val raised: Color,
    /** Frosted card fill drawn over the glowing canvas. */
    val glass: Color,
    /** The 1 dp light edge on glass, brighter at the top. */
    val glassEdge: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val hairline: Color,
    /** Fixed: already planned. */
    val fixed: Color,
    val fixedDeep: Color,
    /** Fuel: yours to spend. Also the Orb's liquid. */
    val fuel: Color,
    val fuelDeep: Color,
    /** Future: protected for goals. */
    val future: Color,
    val futureDeep: Color,
    /** Accent versions dark enough to set as text on the canvas. */
    val fixedInk: Color,
    val fuelInk: Color,
    val futureInk: Color,
    /** Shortfalls and money going out. A warm coral, never an alarm red. */
    val warning: Color,
    /** Highlights, perks and the goal-complete glow. */
    val sun: Color,
    /** Text and icons placed on an accent fill. */
    val onAccent: Color,
    /** The navy hero card. */
    val heroTop: Color,
    val heroBottom: Color,
    val onHero: Color,
    /** Ambient canvas glows. */
    val glowWarm: Color,
    val glowCool: Color,
    val orbCore: Color,
    val orbGlow: Color,
    val orbDeep: Color,
    val scrim: Color,
    /** The hard offset shadow under stamped elements: ink in Daybreak, paper in Night swim. */
    val shadow: Color,
) {
    fun bucket(b: Bucket): Color = when (b) {
        Bucket.FIXED -> fixed
        Bucket.FUEL -> fuel
        Bucket.FUTURE -> future
    }

    fun bucketInk(b: Bucket): Color = when (b) {
        Bucket.FIXED -> fixedInk
        Bucket.FUEL -> fuelInk
        Bucket.FUTURE -> futureInk
    }

    /** Top-left to bottom-right fill for a filled bucket card. */
    fun bucketBrush(b: Bucket): Brush = when (b) {
        Bucket.FIXED -> Brush.verticalGradient(listOf(fixed, fixed))
        Bucket.FUEL -> Brush.verticalGradient(listOf(fuel, fuel))
        Bucket.FUTURE -> Brush.verticalGradient(listOf(future, future))
    }

    /** The runway ticket and other "printed" panels: ink in Daybreak, paper in Night swim. */
    val heroBrush: Brush get() = Brush.verticalGradient(listOf(heroTop, heroBottom))
    /** Haldi, printed flat. */
    val amberBrush: Brush get() = Brush.verticalGradient(listOf(fuel, fuel))
}

/** Mixes a colour toward white. */
fun Color.lighten(amount: Float) = Color(
    red + (1f - red) * amount,
    green + (1f - green) * amount,
    blue + (1f - blue) * amount,
    alpha,
)

private val Paper = Color(0xFFF4EEE3)
private val Ink = Color(0xFF1C1A17)

val Daybreak = RuvoColors(
    isDark = false,
    canvas = Paper,
    surface = Color(0xFFFBF8F2),
    raised = Color(0xFFFFFFFF),
    glass = Color(0xFFFBF8F2),
    glassEdge = Color(0xFFD9CEBC),
    ink = Ink,
    inkSoft = Color(0xFF5F594F),
    inkFaint = Color(0xFF8C857A),
    hairline = Color(0xFFE3D9C9),
    fixed = Color(0xFF8E99E2),
    fixedDeep = Color(0xFF6E7BD2),
    fuel = Color(0xFFF0B232),
    fuelDeep = Color(0xFFDC9819),
    future = Color(0xFF4FAA9E),
    futureDeep = Color(0xFF358D82),
    fixedInk = Color(0xFF3A47A6),
    fuelInk = Color(0xFF8A5A00),
    futureInk = Color(0xFF0F6B62),
    warning = Color(0xFFD5432A),
    sun = Color(0xFFF7CB4D),
    onAccent = Ink,
    heroTop = Color(0xFF26231E),
    heroBottom = Ink,
    onHero = Paper,
    glowWarm = Color(0x24F0B232),
    glowCool = Color(0x1A8E99E2),
    orbCore = Color(0xFFFFF3C4),
    orbGlow = Color(0xFFF0B232),
    orbDeep = Color(0xFFB9770F),
    scrim = Color(0x801C1A17),
    shadow = Ink,
)

val NightSwim = RuvoColors(
    isDark = true,
    canvas = Color(0xFF11131C),
    surface = Color(0xFF191C28),
    raised = Color(0xFF212535),
    glass = Color(0xFF191C28),
    glassEdge = Color(0xFF2E3346),
    ink = Color(0xFFF1EADD),
    inkSoft = Color(0xFFB3AC9F),
    inkFaint = Color(0xFF7D786C),
    hairline = Color(0xFF2A2E3D),
    fixed = Color(0xFF9DA7F2),
    fixedDeep = Color(0xFF7884E2),
    fuel = Color(0xFFF5B84A),
    fuelDeep = Color(0xFFE09A22),
    future = Color(0xFF5CC3B5),
    futureDeep = Color(0xFF389C8F),
    fixedInk = Color(0xFFB1BAF7),
    fuelInk = Color(0xFFF7C668),
    futureInk = Color(0xFF74D3C6),
    warning = Color(0xFFFF6E50),
    sun = Color(0xFFF9D46A),
    onAccent = Color(0xFF14130F),
    heroTop = Color(0xFFF6F0E5),
    heroBottom = Color(0xFFE9DFCD),
    onHero = Ink,
    glowWarm = Color(0x1FF5B84A),
    glowCool = Color(0x269DA7F2),
    orbCore = Color(0xFFFFF3C4),
    orbGlow = Color(0xFFF5B84A),
    orbDeep = Color(0xFFB0700C),
    scrim = Color(0xB3050608),
    shadow = Color(0xFFF1EADD),
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
    /** Space kept clear at the bottom for the floating dock and the Orb above it. */
    val dockClearance = 196.dp
}

/**
 * Crisp corners: paper is cut, not moulded. Only living things (the Orb, avatars) are round.
 */
object Radius {
    val card = 6.dp
    val inner = 4.dp
    val chip = 4.dp
    val sheet = 14.dp
    /** Depth of the hard offset shadow under stamped elements. */
    val stamp = 4.dp
}

/**
 * Book-cover palettes for the Guide shelf: background, shape, highlight. Printed-poster colours from the
 * brand, the same in both moods, because covers are objects, not surfaces.
 */
object Covers {
    private val Ink = Color(0xFF1C1A17)
    private val Paper = Color(0xFFF4EEE3)
    private val Haldi = Color(0xFFF0B232)
    private val Peacock = Color(0xFF2F8C81)
    private val Neel = Color(0xFF3A47A6)
    private val Sindoor = Color(0xFFD5432A)
    private val Rose = Color(0xFFF2B8A8)
    private val Mint = Color(0xFFBFE3D9)

    val palettes: List<Triple<Color, Color, Color>> = listOf(
        Triple(Haldi, Ink, Paper),
        Triple(Ink, Haldi, Sindoor),
        Triple(Peacock, Paper, Haldi),
        Triple(Neel, Paper, Haldi),
        Triple(Sindoor, Paper, Ink),
        Triple(Paper, Ink, Sindoor),
        Triple(Ink, Paper, Peacock),
        Triple(Mint, Ink, Sindoor),
        Triple(Rose, Neel, Ink),
    )
    val label: Color = Paper
    val labelInk: Color = Ink
}
val LocalRuvoColors = staticCompositionLocalOf { Daybreak }
