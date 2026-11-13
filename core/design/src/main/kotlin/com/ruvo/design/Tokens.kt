package com.ruvo.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruvo.engine.Bucket

/**
 * Ruvo's two moods, drawn as frosted white glass over a glowing blue sky. Daybreak (light) is a soft periwinkle
 * canvas with milky white glass cards; Night swim (dark) is black, lit by a deep blue glow at the top and a few
 * soft colour lights, with smoky glass cards.
 *
 * Colour jobs, the same in both moods:
 *   Brand blue: the main actions, Before you buy, the balance, money coming in, the logo.
 *   Yellow: the Marketplace, and Fuel (yours to spend).
 *   Orange: Future (protected for goals). Purple: Fixed (already planned).
 *   A pearly pastel: the review. Rose: shortfalls only, never an alarm red.
 * Selected things are white pills (segments, the dock) or ink pills (chips), so blue stays for actions.
 * Every colour in the app comes from here.
 */
@Immutable
data class RuvoColors(
    val isDark: Boolean,
    /** The canvas at the bottom of the screen, and behind sheets. */
    val canvas: Color,
    /** The canvas at the very top, behind the header: deeper blue, fading into [canvas]. */
    val canvasTop: Color,
    val surface: Color,
    val raised: Color,
    /** Frosted glass card fill, bottom and top (the top catches more light). */
    val glass: Color,
    val glassTop: Color,
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
    /** Text and icons on a bucket card: dark ink on the pastels of Daybreak, light in Night swim. */
    val onAccent: Color,
    /** Ruvo's brand: a royal blue. Main buttons, Before you buy, the balance, money in, the logo. */
    val brand: Color,
    val brandDeep: Color,
    /** Text and icons on a brand fill. Always light. */
    val onBrand: Color,
    /** The blue brand cards. */
    val heroTop: Color,
    val heroBottom: Color,
    val onHero: Color,
    /** Soft lights on the canvas: a warm sun, a blue and a blush pink. */
    val glowWarm: Color,
    val glowCool: Color,
    val glowPink: Color,
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
        // A shade lighter than the canvas so names and amounts stay clearly readable at night.
        Bucket.FIXED -> Brush.linearGradient(listOf(Color(0xFF2B2550), Color(0xFF1A1830)))
        Bucket.FUEL -> Brush.linearGradient(listOf(Color(0xFF362E1B), Color(0xFF1E1B13)))
        Bucket.FUTURE -> Brush.linearGradient(listOf(Color(0xFF3B281D), Color(0xFF1F1813)))
    } else when (b) {
        Bucket.FIXED -> Brush.linearGradient(listOf(fixed.lighten(0.62f), fixed.lighten(0.3f)))
        Bucket.FUEL -> Brush.linearGradient(listOf(sun.lighten(0.5f), fuel.lighten(0.15f)))
        Bucket.FUTURE -> Brush.linearGradient(listOf(future.lighten(0.6f), future.lighten(0.25f)))
    }

    /** Brand blue safe for text and small icons on the canvas: the brand by day, a lighter blue at night. */
    val brandInk: Color get() = if (isDark) brand.lighten(0.42f) else brand
    /** A light wash of brand blue, for small fills such as the money-in card. Text on it uses [brandInk]. */
    val brandTint: Color get() = brand.copy(alpha = if (isDark) 0.22f else 0.15f)

    /** The blue brand cards (Before you buy, the balance): royal blue in both moods, with light text. */
    val heroBrush: Brush get() = Brush.linearGradient(listOf(heroTop, heroBottom))
    /** Brand fills: primary buttons and the add buttons. */
    val brandBrush: Brush get() = Brush.linearGradient(listOf(brand.lighten(if (isDark) 0.1f else 0.18f), brandDeep))
    /** The Marketplace's colour everywhere: a sunny yellow in both moods, always with [onLight] text. */
    val marketBrush: Brush get() = if (isDark) Brush.linearGradient(listOf(Color(0xFFF7D774), Color(0xFFE5AC2E)))
        else Brush.linearGradient(listOf(Color(0xFFFFE795), Color(0xFFFFC23D)))
    /** A pearly pastel sheen (sky, lilac, blush, butter) for the review. Always with [onLight] text. */
    val irisBrush: Brush get() = if (isDark) Brush.linearGradient(listOf(Color(0xFF9DB5F2), Color(0xFFB7A2EC), Color(0xFFE3A9C6), Color(0xFFE8CB86)))
        else Brush.linearGradient(listOf(Color(0xFFC3D6FF), Color(0xFFDCCBFF), Color(0xFFFFD3E4), Color(0xFFFFEBB3)))
    /** Navy text for the yellow and pearly cards, which stay light in both moods. */
    val onLight: Color get() = Color(0xFF141B4D)
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
    // A soft periwinkle sky, a little deeper at the top, so white glass cards float on it.
    canvas = Color(0xFFE4E9FB),
    canvasTop = Color(0xFFCCD6FA),
    surface = Color(0xFFFFFFFF),
    raised = Color(0xFFFFFFFF),
    glass = Color(0x99FFFFFF),
    glassTop = Color(0xE6FFFFFF),
    glassEdge = Color(0xFFFFFFFF),
    ink = Navy,
    inkSoft = Color(0xFF4B5276),
    inkFaint = Color(0xFF666D8C),
    hairline = Color(0xFFD3DAF0),
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
    heroTop = Color(0xFF3550D6),
    heroBottom = Color(0xFF16216E),
    onHero = Color(0xFFFFFFFF),
    glowWarm = Color(0x66FFC23D),
    glowCool = Color(0x8C5B78FF),
    glowPink = Color(0x59FF8FB8),
    orbCore = Color(0xFFFFF1B8),
    orbGlow = Amber,
    orbDeep = Color(0xFFD9860A),
    scrim = Color(0x73141B4D),
    shadow = Color(0x332338A8),
)

val NightSwim = RuvoColors(
    isDark = true,
    // Black, with a deep blue night sky at the top that fades out by the middle of the screen.
    canvas = Color(0xFF000000),
    canvasTop = Color(0xFF0A1238),
    surface = Color(0xFF121320),
    raised = Color(0xFF1B1C28),
    glass = Color(0x0FFFFFFF),
    glassTop = Color(0x21FFFFFF),
    glassEdge = Color(0x38FFFFFF),
    ink = Color(0xFFF5F5F7),
    inkSoft = Color(0xFFABADBA),
    inkFaint = Color(0xFF80828F),
    hairline = Color(0xFF26272F),
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
    heroTop = Color(0xFF3A56E6),
    heroBottom = Color(0xFF17227A),
    onHero = Color(0xFFFFFFFF),
    // Soft lights so the glass has something to catch; most of the canvas stays black.
    glowWarm = Color(0x1AEDBE4E),
    glowCool = Color(0x474A66F0),
    glowPink = Color(0x24FF5C9A),
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
    val card = 22.dp
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

    /**
     * Bright, saturated colours for small coloured things that must read on white glass and on black alike: book
     * spines on the reading shelf and brand initials in the Marketplace. Each pairs with its own text colour.
     */
    val brights: List<Pair<Color, Color>> = listOf(
        Color(0xFFFFC23D) to Navy,
        Color(0xFFFF8A45) to Navy,
        Color(0xFF8B6CFF) to White,
        Color(0xFF3550D6) to White,
        Color(0xFFFF6FA8) to Navy,
        Color(0xFF4A9BEA) to White,
    )
}

val LocalRuvoColors = staticCompositionLocalOf { Daybreak }
