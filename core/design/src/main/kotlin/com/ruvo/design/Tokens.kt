package com.ruvo.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruvo.engine.Bucket

/**
 * Ruvo's two moods in one palette: sunny yellow, warm orange, violet purple and a deep night blue, with white
 * and black. Daybreak (light) sits on a soft lavender canvas with white cards; Night swim (dark) is true black.
 *
 * The three buckets keep one colour each in both moods:
 *   Fuel (yours to spend) = yellow, Future (protected for goals) = orange, Fixed (already planned) = purple.
 * Shortfalls use a rose that is never confused with the orange. Every colour in the app comes from here.
 */
@Immutable
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
    /** Text and icons on a bucket or soft-amber card: dark ink on the pastels of Daybreak, light in Night swim. */
    val onAccent: Color,
    /** Ruvo's brand: a dark royal blue. Buttons, the chosen tab, chips, the logo and the main cards. */
    val brand: Color,
    val brandDeep: Color,
    /** Text and icons on a brand fill. Always light. */
    val onBrand: Color,
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
    /** Soft coloured shadow under raised elements. */
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

    /**
     * Top-left to bottom-right fill for a filled bucket card. Daybreak uses soft pastels with dark ink. Night swim
     * uses deep cards with only a whisper of the bucket's colour, so nothing glares on black; the bucket's name
     * carries the colour instead.
     */
    fun bucketBrush(b: Bucket): Brush = if (isDark) when (b) {
        Bucket.FIXED -> Brush.linearGradient(listOf(Color(0xFF1E1B36), Color(0xFF141322)))
        Bucket.FUEL -> Brush.linearGradient(listOf(Color(0xFF26221A), Color(0xFF17150F)))
        Bucket.FUTURE -> Brush.linearGradient(listOf(Color(0xFF281D19), Color(0xFF181210)))
    } else when (b) {
        Bucket.FIXED -> Brush.linearGradient(listOf(fixed.lighten(0.62f), fixed.lighten(0.3f)))
        Bucket.FUEL -> Brush.linearGradient(listOf(sun.lighten(0.5f), fuel.lighten(0.15f)))
        Bucket.FUTURE -> Brush.linearGradient(listOf(future.lighten(0.6f), future.lighten(0.25f)))
    }

    /** Brand blue safe for text and small icons on the canvas: the brand by day, a lighter blue at night. */
    val brandInk: Color get() = if (isDark) brand.lighten(0.42f) else brand

    /** The main brand cards: deep blue in both moods, with light text. */
    val heroBrush: Brush get() = Brush.linearGradient(listOf(heroTop, heroBottom))
    /** Brand fills: primary buttons, the chosen tab and segment, the add buttons. */
    val brandBrush: Brush get() = Brush.linearGradient(listOf(brand.lighten(if (isDark) 0.1f else 0.18f), brandDeep))
    /** A soft card for invitations (Marketplace, New this week, payments to sort): pale amber by day, deep blue-grey at night. */
    val softAmberBrush: Brush get() = if (isDark) Brush.linearGradient(listOf(Color(0xFF1A2040), Color(0xFF11152B)))
        else Brush.linearGradient(listOf(sun.lighten(0.55f), fuel.lighten(0.2f)))
}

/** Mixes a colour toward white. */
fun Color.lighten(amount: Float) = Color(
    red + (1f - red) * amount,
    green + (1f - green) * amount,
    blue + (1f - blue) * amount,
    alpha,
)

private val Navy = Color(0xFF141B4D)
private val Amber = Color(0xFFFFC23D)
private val AmberDeep = Color(0xFFFFA51F)
private val Butter = Color(0xFFFFDB6E)
private val Violet = Color(0xFF8B6CFF)
private val VioletDeep = Color(0xFF6A4BF0)
private val Tangerine = Color(0xFFFF8A45)
private val TangerineDeep = Color(0xFFF2651F)

private val Royal = Color(0xFF2338A8)
private val RoyalDeep = Color(0xFF16216E)

val Daybreak = RuvoColors(
    isDark = false,
    // A cool, slightly blue paper, so the brand blue feels at home.
    canvas = Color(0xFFEDF0FA),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFFFFFFF),
    glass = Color(0x99FFFFFF),
    glassEdge = Color(0xFFFFFFFF),
    ink = Navy,
    inkSoft = Color(0xFF545B7E),
    inkFaint = Color(0xFF81879F),
    hairline = Color(0xFFDDE2F0),
    fixed = Violet,
    fixedDeep = VioletDeep,
    fuel = Amber,
    fuelDeep = AmberDeep,
    future = Tangerine,
    futureDeep = TangerineDeep,
    fixedInk = Color(0xFF5A3BDB),
    fuelInk = Color(0xFF8F5B00),
    futureInk = Color(0xFFB7450B),
    warning = Color(0xFFD93A63),
    sun = Butter,
    onAccent = Navy,
    brand = Royal,
    brandDeep = RoyalDeep,
    onBrand = Color(0xFFFFFFFF),
    // The brand cards: deep royal blue, white text.
    heroTop = Color(0xFF2A41B8),
    heroBottom = Color(0xFF141E66),
    onHero = Color(0xFFFFFFFF),
    glowWarm = Color(0x66FFC23D),
    glowCool = Color(0x804D6BFF),
    orbCore = Color(0xFFFFF1B8),
    orbGlow = Amber,
    orbDeep = Color(0xFFD9860A),
    scrim = Color(0x73141B4D),
    shadow = Color(0x33141B4D),
)

val NightSwim = RuvoColors(
    isDark = true,
    // True black: the canvas is #000, cards are barely lifted greys, and nothing glows behind them.
    canvas = Color(0xFF000000),
    surface = Color(0xFF111114),
    raised = Color(0xFF1A1A1F),
    glass = Color(0x1AFFFFFF),
    glassEdge = Color(0x33FFFFFF),
    ink = Color(0xFFF5F5F7),
    inkSoft = Color(0xFFA9A9B2),
    inkFaint = Color(0xFF6E6E78),
    hairline = Color(0xFF232328),
    // At night the bucket colours step back a little, so they label rather than shout.
    fixed = Color(0xFF9B8BEB),
    fixedDeep = Color(0xFF7765D6),
    fuel = Color(0xFFEDBE4E),
    fuelDeep = Color(0xFFD9A22F),
    future = Color(0xFFEC9263),
    futureDeep = Color(0xFFD9733F),
    fixedInk = Color(0xFFB3A7F2),
    fuelInk = Color(0xFFF2CB6B),
    futureInk = Color(0xFFF2A780),
    warning = Color(0xFFF2708C),
    sun = Color(0xFFF2D27A),
    onAccent = Color(0xFFF2F3FA),
    // Brighter than by day, so the brand still reads on black.
    brand = Color(0xFF4A66F0),
    brandDeep = Color(0xFF2E45C9),
    onBrand = Color(0xFFFFFFFF),
    heroTop = Color(0xFF1B2766),
    heroBottom = Color(0xFF0C1138),
    onHero = Color(0xFFFFFFFF),
    // Only a breath of blue at the corners, so glass has something to catch; the canvas stays black.
    glowWarm = Color(0x14EDBE4E),
    glowCool = Color(0x2E4A66F0),
    orbCore = Color(0xFFFFF1B8),
    orbGlow = Amber,
    orbDeep = Color(0xFFB86E06),
    scrim = Color(0xCC000000),
    shadow = Color(0x80000000),
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
    val gutter = 16.dp
    /** Space kept clear at the bottom for the floating dock and the Orb above it. */
    val dockClearance = 196.dp
}

object Radius {
    val card = 20.dp
    val inner = 14.dp
    val chip = 999.dp
    val sheet = 30.dp
}

/**
 * Book-cover palettes for the Guide shelf: background, shape, highlight. Printed-poster colours from the
 * brand, the same in both moods, because covers are objects, not surfaces.
 */
object Covers {
    private val Navy = Color(0xFF0F1B3D)
    private val White = Color(0xFFFFFFFF)
    private val Amber = Color(0xFFFDB913)
    private val Sky = Color(0xFF4A9BEA)
    private val Lavender = Color(0xFFB79CF2)
    private val Coral = Color(0xFFF26B1D)
    private val Cream = Color(0xFFFFE2B8)
    private val Ice = Color(0xFFCFE6FA)

    val palettes: List<Triple<Color, Color, Color>> = listOf(
        Triple(Amber, Navy, White),
        Triple(Navy, Amber, Sky),
        Triple(Sky, Navy, White),
        Triple(Lavender, Navy, Amber),
        Triple(Coral, Navy, Cream),
        Triple(White, Navy, Coral),
        Triple(Navy, Lavender, Amber),
        Triple(Ice, Navy, Amber),
        Triple(Cream, Coral, Navy),
    )
    val label: Color = White
    val labelInk: Color = Navy
}

val LocalRuvoColors = staticCompositionLocalOf { Daybreak }
