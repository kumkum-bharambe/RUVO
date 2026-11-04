package com.ruvo.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * App-icon style tiles for known merchants: the brand's own colour and a stylised mark in Ruvo's typeface, so a
 * Swiggy order reads as Swiggy at a glance. These are look-alike marks drawn in code, not the brands' artwork.
 * Unknown merchants fall back to their category's icon; people get a round monogram.
 */
private enum class Accent { NONE, SMILE, WAVES, PLAY, RING, BAR }

private data class Brand(
    val bg: Color,
    val fg: Color,
    val mark: String,
    val scale: Float = 0.5f,
    val italic: Boolean = false,
    val accent: Accent = Accent.NONE,
    val accentColor: Color = fg,
)

/** Checked in order, so more specific keys come first. Keys are lowercase with spaces removed. */
private val BRANDS: List<Pair<String, Brand>> = listOf(
    "instamart" to Brand(Color(0xFFE2177A), Color.White, "im", 0.42f),
    "swiggy" to Brand(Color(0xFFFC8019), Color.White, "S", 0.56f),
    "zomato" to Brand(Color(0xFFE23744), Color.White, "z", 0.62f, italic = true),
    "zepto" to Brand(Color(0xFF5A189A), Color.White, "z", 0.6f, accent = Accent.BAR, accentColor = Color(0xFFFF3269)),
    "blinkit" to Brand(Color(0xFFF8CB46), Color(0xFF0C831F), "b", 0.62f),
    "uber" to Brand(Color(0xFF000000), Color.White, "Uber", 0.3f),
    "ola" to Brand(Color(0xFF1C1C1C), Color(0xFFD4E157), "ola", 0.36f),
    "rapido" to Brand(Color(0xFFF9C933), Color(0xFF111111), "r", 0.62f, italic = true),
    "redbus" to Brand(Color(0xFFD84E55), Color.White, "rb", 0.42f),
    "irctc" to Brand(Color(0xFF213D7A), Color.White, "IR", 0.38f, accent = Accent.BAR, accentColor = Color(0xFFF58220)),
    "metro" to Brand(Color(0xFF0B5CAD), Color.White, "M", 0.48f, accent = Accent.BAR, accentColor = Color(0xFFE53935)),
    "localtrain" to Brand(Color(0xFF7B2D26), Color.White, "LT", 0.38f),
    "amazon" to Brand(Color(0xFF232F3E), Color.White, "a", 0.56f, accent = Accent.SMILE, accentColor = Color(0xFFFF9900)),
    "flipkart" to Brand(Color(0xFF2874F0), Color(0xFFFFE11B), "f", 0.62f, italic = true),
    "myntra" to Brand(Color(0xFFFF3F6C), Color.White, "M", 0.54f),
    "nykaa" to Brand(Color(0xFFFC2779), Color.White, "N", 0.52f),
    "meesho" to Brand(Color(0xFF9F2089), Color.White, "m", 0.58f),
    "decathlon" to Brand(Color(0xFF0082C3), Color.White, "D", 0.52f),
    "croma" to Brand(Color(0xFF0B1F2A), Color(0xFF12DAA8), "c", 0.62f),
    "dmart" to Brand(Color(0xFF00843D), Color(0xFFFFD200), "D", 0.54f),
    "jiomart" to Brand(Color(0xFF0F3CC9), Color.White, "jm", 0.42f),
    "jiohotstar" to Brand(Color(0xFF0B1240), Color.White, "JH", 0.38f, accent = Accent.BAR, accentColor = Color(0xFF1F80E0)),
    "hotstar" to Brand(Color(0xFF0B1240), Color.White, "JH", 0.38f, accent = Accent.BAR, accentColor = Color(0xFF1F80E0)),
    "jio" to Brand(Color(0xFF0F3CC9), Color.White, "jio", 0.36f),
    "airtel" to Brand(Color(0xFFE40000), Color.White, "a", 0.6f, italic = true),
    "vi" to Brand(Color(0xFFED1C24), Color(0xFFFFD100), "Vi", 0.44f),
    "tatapower" to Brand(Color(0xFF0067B1), Color.White, "T", 0.52f),
    "mahavitaran" to Brand(Color(0xFF1E6F3C), Color.White, "MV", 0.36f),
    "spotify" to Brand(Color(0xFF121212), Color(0xFF1DB954), "", accent = Accent.WAVES),
    "netflix" to Brand(Color(0xFF000000), Color(0xFFE50914), "N", 0.62f),
    "youtube" to Brand(Color(0xFFFF0000), Color.White, "", accent = Accent.PLAY),
    "primevideo" to Brand(Color(0xFF00A8E1), Color.White, "prime", 0.26f, accent = Accent.SMILE, accentColor = Color.White),
    "starbucks" to Brand(Color(0xFF00704A), Color.White, "S", 0.42f, accent = Accent.RING),
    "thirdwave" to Brand(Color(0xFF151515), Color(0xFFF5E6C8), "TW", 0.38f),
    "chaipoint" to Brand(Color(0xFFF2A900), Color(0xFF4A2511), "cp", 0.44f),
    "chaayos" to Brand(Color(0xFF1C5C3B), Color(0xFFF3D27A), "C", 0.54f),
    "domino" to Brand(Color(0xFF006491), Color.White, "D", 0.5f, accent = Accent.BAR, accentColor = Color(0xFFE31837)),
    "mcdonald" to Brand(Color(0xFFDA291C), Color(0xFFFFC72C), "M", 0.62f),
    "pvr" to Brand(Color(0xFF141414), Color(0xFFFFCB05), "PVR", 0.3f),
    "bookmyshow" to Brand(Color(0xFFF84464), Color.White, "bms", 0.34f),
    "steam" to Brand(Color(0xFF171A21), Color(0xFF66C0F4), "S", 0.42f, accent = Accent.RING),
    "unacademy" to Brand(Color(0xFF08BD80), Color.White, "U", 0.54f),
    "udemy" to Brand(Color(0xFF1C1D1F), Color(0xFFA435F0), "U", 0.56f),
    "coursera" to Brand(Color(0xFF0056D2), Color.White, "C", 0.56f),
    "pharmeasy" to Brand(Color(0xFF10847E), Color.White, "P", 0.52f),
    "netmeds" to Brand(Color(0xFF32AEB1), Color.White, "N", 0.52f),
    "paytm" to Brand(Color(0xFF00BAF2), Color(0xFF002E6E), "paytm", 0.26f),
    "phonepe" to Brand(Color(0xFF5F259F), Color.White, "Pe", 0.44f),
)

private fun brandFor(name: String): Brand? {
    val key = name.lowercase().replace(Regex("[^a-z0-9]"), "")
    if (key == "vi") return BRANDS.first { it.first == "vi" }.second
    return BRANDS.firstOrNull { (k, _) -> k != "vi" && key.contains(k) }?.second
}

/** A merchant's logo tile. [category] picks the fallback icon when the merchant isn't a known brand. */
@Composable
fun MerchantLogo(name: String, category: String, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(size * 0.3f)
    val brand = brandFor(name)
    if (brand == null) {
        val tint = categoryTint(category)
        Box(modifier.size(size).clip(shape).background(tint.copy(alpha = if (c.isDark) 0.2f else 0.16f)), contentAlignment = Alignment.Center) {
            Icon(RuvoIcons.forCategory(category), null, Modifier.size(size * 0.5f), tint = if (c.isDark) tint else c.ink)
        }
        return
    }
    val fontSize = with(LocalDensity.current) { (size * brand.scale).toSp() }
    Box(modifier.size(size).clip(shape).background(brand.bg), contentAlignment = Alignment.Center) {
        if (brand.accent != Accent.NONE) Canvas(Modifier.fillMaxSize()) {
            val w = size.toPx()
            val stroke = w * 0.065f
            when (brand.accent) {
                Accent.SMILE -> {
                    val smile = Path().apply {
                        moveTo(w * 0.26f, w * 0.7f)
                        quadraticBezierTo(w * 0.5f, w * 0.84f, w * 0.74f, w * 0.68f)
                    }
                    drawPath(smile, brand.accentColor, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawLine(brand.accentColor, Offset(w * 0.68f, w * 0.64f), Offset(w * 0.76f, w * 0.68f), stroke * 0.9f, StrokeCap.Round)
                }
                Accent.WAVES -> {
                    drawCircle(brand.fg, w * 0.33f)
                    for (i in 0..2) {
                        val y = w * (0.4f + i * 0.11f)
                        val inset = w * (0.3f + i * 0.04f)
                        val arc = Path().apply {
                            moveTo(inset, y)
                            quadraticBezierTo(w * 0.5f, y - w * 0.07f, w - inset, y + w * 0.03f)
                        }
                        drawPath(arc, brand.bg, style = Stroke(stroke * (1.1f - i * 0.18f), cap = StrokeCap.Round))
                    }
                }
                Accent.PLAY -> {
                    drawRoundRect(Color.White, Offset(w * 0.2f, w * 0.29f), Size(w * 0.6f, w * 0.42f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f))
                    val tri = Path().apply {
                        moveTo(w * 0.44f, w * 0.4f); lineTo(w * 0.6f, w * 0.5f); lineTo(w * 0.44f, w * 0.6f); close()
                    }
                    drawPath(tri, brand.bg)
                }
                Accent.RING -> {
                    drawCircle(brand.fg, w * 0.34f, style = Stroke(stroke))
                }
                Accent.BAR -> {
                    drawLine(brand.accentColor, Offset(w * 0.3f, w * 0.8f), Offset(w * 0.7f, w * 0.8f), stroke, StrokeCap.Round)
                }
                Accent.NONE -> Unit
            }
        }
        // A faint top sheen makes the tile read as a real app icon rather than a flat swatch.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent))))
        if (brand.mark.isNotEmpty()) Text(
            brand.mark,
            style = Ruvo.type.title.copy(
                fontSize = fontSize, lineHeight = fontSize, fontWeight = FontWeight(800),
                fontStyle = if (brand.italic) FontStyle.Italic else FontStyle.Normal, letterSpacing = (-0.04).em,
            ),
            color = brand.fg, maxLines = 1, textAlign = TextAlign.Center,
            modifier = if (brand.accent == Accent.SMILE || brand.accent == Accent.BAR) Modifier.padding(bottom = size * 0.12f) else Modifier,
        )
    }
}

/** A person: their initial on a soft round tile. Used for splits and money owed. */
@Composable
fun PersonAvatar(name: String, accent: Color, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val c = Ruvo.colors
    Box(modifier.size(size).clip(CircleShape).background(accent.copy(alpha = if (c.isDark) 0.24f else 0.22f)), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?", style = Ruvo.type.bodyStrong, color = if (c.isDark) accent else c.ink)
    }
}

@Composable
private fun categoryTint(category: String): Color {
    val c = Ruvo.colors
    return when (category) {
        "Income", "Transfer" -> c.future
        "Bills", "Rent", "Education", "Subscriptions" -> c.fixed
        else -> c.fuel
    }
}
