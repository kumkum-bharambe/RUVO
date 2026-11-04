package com.ruvo.feature.guide

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruvo.design.Covers
import com.ruvo.design.Ruvo
import com.ruvo.design.lighten
import com.ruvo.design.pressable
import com.ruvo.engine.guide.Library
import kotlin.math.sin

/** Stable, well-spread hash so a book always gets the same cover. */
private fun hashOf(s: String): Int = s.fold(7) { a, c -> (a * 31 + c.code) and 0x7fffffff }

/**
 * A book on the shelf: a flat, poster-style cover with its own little picture of what the book is about, a
 * spine shadow and a white label card. Every book has a different picture and colour pairing.
 */
@Composable
fun BookCover(
    book: Library.Book,
    read: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val bg = backgroundFor(book.id)
    val dark = Ruvo.colors.isDark
    val height = width * 1.42f
    val corners = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
    val interaction = remember { MutableInteractionSource() }
    var m = modifier.size(width, height).pressable(interaction)
        .shadow(14.dp, corners, ambientColor = Ruvo.colors.shadow, spotColor = Ruvo.colors.shadow)
        .clip(corners)
    if (onClick != null) {
        m = m.clickable(interaction, indication = null, role = Role.Button, onClickLabel = "Open ${book.title}", onClick = onClick)
    }
    Box(m.semantics { contentDescription = "${book.title}, ${book.minutes} minute read${if (read) ", finished" else ""}" }) {
        Canvas(Modifier.fillMaxSize()) {
            // Two colours fading into each other, and a soft glow of a third, so every cover is lively.
            val (second, third) = accentsFor(book.id, bg)
            drawRect(Brush.linearGradient(listOf(bg.lighten(0.18f), second), Offset(0f, 0f), Offset(size.width, size.height)))
            drawCircle(Brush.radialGradient(listOf(third.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.85f), size.width * 0.8f), size.width * 0.8f, Offset(size.width * 0.1f, size.height * 0.85f))
            drawIllustration(book.id)
            // Spine: a darker band and a soft highlight, like a printed paperback.
            drawRect(Color.Black.copy(alpha = 0.16f), size = Size(size.width * 0.045f, size.height))
            drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), startX = size.width * 0.045f, endX = size.width * 0.16f))
            // At night the covers dim a touch, so a shelf of them glows instead of glaring.
            if (dark) drawRect(Color.Black.copy(alpha = 0.22f))
        }
        if (width >= 100.dp) Text(
            if (read) "Read" else "${book.pages.size} pages",
            Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(99.dp)).background(Covers.labelInk).padding(horizontal = 8.dp, vertical = 3.dp),
            style = Ruvo.type.eyebrow, color = Covers.label,
        )
        if (showLabel) {
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Covers.label).padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(book.title, style = Ruvo.type.label, color = Covers.labelInk, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(if (read) "Finished" else "${book.minutes} min read", style = Ruvo.type.caption, color = Covers.labelInk.copy(alpha = 0.6f))
            }
        }
    }
}

// The cover palette: the brand's yellow, orange, purple and night blue, with white, black and a few soft tints.
private val Y = Color(0xFFFFC23D)
private val YD = Color(0xFFE59A0C)
private val YL = Color(0xFFFFDB6E)
private val O = Color(0xFFFF8A45)
private val P = Color(0xFF8B6CFF)
private val DP = Color(0xFF4B2FC0)
private val L = Color(0xFFCDBFFF)
private val N = Color(0xFF141B4D)
private val W = Color(0xFFFFFFFF)
private val PK = Color(0xFFFF8FB0)
private val PKD = Color(0xFFE0668C)
private val C = Color(0xFFFFF1D6)

/** Each book's background, chosen with its picture so neighbours on a shelf never match. */
private fun backgroundFor(id: String): Color = when (id) {
    "upi" -> P; "subs" -> Y; "hostel" -> N; "503020" -> C
    "balance" -> O; "cashflow" -> L; "where" -> W; "fixed" -> DP
    "first" -> Y; "protect" -> N; "buffer" -> PK; "cushion" -> P
    "small" -> O; "pause" -> C; "purpose" -> N; "irregular" -> L
    "split" -> Y; "discounts" -> PK; "college" -> O; "invest" -> P
    "saveinvest" -> W; "risk" -> N; "emergency" -> Y; "beforeinvest" -> O
    else -> Covers.palettes[hashOf(id) % Covers.palettes.size].first
}

// Small drawing helpers in cover fractions: x and widths are fractions of the width, y and heights of the height.
private fun DrawScope.dot(c: Color, r: Float, x: Float, y: Float) = drawCircle(c, size.width * r, Offset(size.width * x, size.height * y))

private fun DrawScope.box(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 0.03f) =
    drawRoundRect(c, Offset(size.width * x, size.height * y), Size(size.width * w, size.height * h), CornerRadius(size.width * r))

private fun DrawScope.poly(c: Color, vararg p: Float) = drawPath(
    Path().apply {
        moveTo(size.width * p[0], size.height * p[1])
        var i = 2
        while (i < p.size) { lineTo(size.width * p[i], size.height * p[i + 1]); i += 2 }
        close()
    },
    c,
)

private fun DrawScope.stroke(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, wdt: Float = 0.035f) =
    drawLine(c, Offset(size.width * x1, size.height * y1), Offset(size.width * x2, size.height * y2), size.width * wdt, StrokeCap.Round)

private fun DrawScope.coin(x: Float, y: Float, r: Float = 0.09f) {
    dot(YD, r, x, y + 0.006f); dot(Y, r, x, y); dot(YL, r * 0.6f, x, y)
}

/** One card in the subscriptions fan, rotated about its own centre. */
private fun DrawScope.card(c: Color, degrees: Float, x: Float, y: Float) {
    val w = size.width
    val h = size.height
    rotate(degrees, Offset(w * (x + 0.22f), h * (y + 0.14f))) {
        drawRoundRect(c, Offset(w * x, h * y), Size(w * 0.44f, h * 0.28f), CornerRadius(w * 0.05f))
        drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(w * (x + 0.05f), h * (y + 0.05f)), Size(w * 0.12f, h * 0.06f), CornerRadius(w * 0.02f))
    }
}

/** A small picture of what each book is about, drawn in the top part of the cover above the label. */
private fun DrawScope.drawIllustration(id: String) {
    val w = size.width
    val h = size.height
    when (id) {
        "upi" -> { // A phone showing a QR code, with a need chip and a want chip either side.
            box(N, 0.32f, 0.1f, 0.4f, 0.44f, 0.06f); box(W, 0.36f, 0.15f, 0.32f, 0.33f, 0.03f)
            listOf(0, 2, 6, 8, 4).forEach { i -> box(N, 0.39f + (i % 3) * 0.09f, 0.18f + (i / 3) * 0.09f, 0.07f, 0.07f, 0.01f) }
            box(Y, 0.06f, 0.24f, 0.2f, 0.08f, 0.04f); box(PK, 0.76f, 0.34f, 0.2f, 0.08f, 0.04f)
        }
        "subs" -> { // Fanned subscription cards with a repeat loop.
            card(N, -14f, 0.2f, 0.2f); card(O, -4f, 0.26f, 0.22f); card(W, 8f, 0.32f, 0.24f)
            drawArc(N, 200f, 250f, false, Offset(w * 0.64f, h * 0.05f), Size(w * 0.24f, w * 0.24f), style = Stroke(w * 0.04f))
            poly(N, 0.62f, 0.09f, 0.71f, 0.05f, 0.71f, 0.14f)
        }
        "hostel" -> { // A hostel block at night: lit windows and a crescent moon.
            dot(Y, 0.1f, 0.8f, 0.1f); dot(N, 0.08f, 0.84f, 0.08f)
            box(O, 0.18f, 0.2f, 0.56f, 0.38f, 0.02f); poly(DP, 0.13f, 0.21f, 0.46f, 0.08f, 0.79f, 0.21f)
            for (r in 0..2) for (cc in 0..2) box(if ((r + cc) % 2 == 0) Y else N, 0.24f + cc * 0.16f, 0.26f + r * 0.1f, 0.1f, 0.065f, 0.01f)
        }
        "503020" -> { // A ring split 50 / 30 / 20.
            val d = w * 0.52f
            val tl = Offset(w * 0.5f - d / 2, h * 0.3f - d / 2)
            drawArc(P, -90f, 180f, false, tl, Size(d, d), style = Stroke(w * 0.14f))
            drawArc(O, 90f, 108f, false, tl, Size(d, d), style = Stroke(w * 0.14f))
            drawArc(Y, 198f, 72f, false, tl, Size(d, d), style = Stroke(w * 0.14f))
        }
        "balance" -> { // A balance: what you have against what you can use.
            stroke(N, 0.5f, 0.1f, 0.5f, 0.52f); stroke(N, 0.16f, 0.18f, 0.84f, 0.14f); box(N, 0.36f, 0.5f, 0.28f, 0.05f)
            stroke(N, 0.2f, 0.18f, 0.12f, 0.34f, 0.015f); stroke(N, 0.2f, 0.18f, 0.3f, 0.34f, 0.015f); box(W, 0.08f, 0.34f, 0.26f, 0.04f)
            coin(0.21f, 0.29f, 0.06f)
            stroke(N, 0.8f, 0.14f, 0.7f, 0.3f, 0.015f); stroke(N, 0.8f, 0.14f, 0.9f, 0.3f, 0.015f); box(W, 0.66f, 0.3f, 0.28f, 0.04f)
            box(P, 0.72f, 0.21f, 0.14f, 0.09f, 0.02f)
        }
        "cashflow" -> { // A river of money with coins drifting along it.
            drawPath(
                Path().apply {
                    moveTo(0f, h * 0.36f); cubicTo(w * 0.3f, h * 0.2f, w * 0.5f, h * 0.5f, w, h * 0.3f); lineTo(w, h * 0.42f)
                    cubicTo(w * 0.5f, h * 0.62f, w * 0.3f, h * 0.32f, 0f, h * 0.48f); close()
                },
                P,
            )
            coin(0.2f, 0.33f, 0.06f); coin(0.52f, 0.41f, 0.07f); coin(0.82f, 0.34f, 0.06f)
            dot(O, 0.08f, 0.8f, 0.1f)
        }
        "where" -> { // A map pin and the dotted path your money took.
            for (i in 0..6) dot(N, 0.018f, 0.12f + i * 0.09f, 0.5f - sin(i * 0.9f) * 0.05f)
            dot(O, 0.16f, 0.62f, 0.2f); poly(O, 0.48f, 0.25f, 0.76f, 0.25f, 0.62f, 0.44f); dot(W, 0.065f, 0.62f, 0.2f)
            coin(0.14f, 0.5f, 0.06f)
        }
        "fixed" -> { // A calendar page pinned up, with things already ticked off.
            box(W, 0.2f, 0.12f, 0.6f, 0.44f, 0.05f); box(O, 0.2f, 0.12f, 0.6f, 0.09f, 0.05f)
            dot(Y, 0.05f, 0.5f, 0.11f)
            for (r in 0..1) for (cc in 0..2) box(if (r == 0 && cc < 2) P else L, 0.27f + cc * 0.17f, 0.27f + r * 0.13f, 0.12f, 0.09f, 0.02f)
        }
        "first" -> { // A piggy bank with a coin on its way in.
            dot(PK, 0.27f, 0.5f, 0.34f); dot(PK, 0.1f, 0.76f, 0.33f); dot(PKD, 0.03f, 0.79f, 0.33f)
            poly(PK, 0.36f, 0.17f, 0.42f, 0.08f, 0.48f, 0.16f); box(PKD, 0.42f, 0.15f, 0.16f, 0.025f, 0.01f)
            box(PK, 0.33f, 0.5f, 0.08f, 0.08f); box(PK, 0.59f, 0.5f, 0.08f, 0.08f); dot(N, 0.025f, 0.63f, 0.29f)
            coin(0.24f, 0.1f, 0.06f)
        }
        "protect" -> { // A shield keeping a coin safe.
            poly(P, 0.5f, 0.06f, 0.82f, 0.14f, 0.78f, 0.4f, 0.5f, 0.58f, 0.22f, 0.4f, 0.18f, 0.14f)
            poly(L, 0.5f, 0.12f, 0.74f, 0.18f, 0.71f, 0.38f, 0.5f, 0.51f, 0.29f, 0.38f, 0.26f, 0.18f)
            coin(0.5f, 0.31f, 0.11f)
        }
        "buffer" -> { // An hourglass of spare days.
            box(N, 0.26f, 0.06f, 0.48f, 0.04f); box(N, 0.26f, 0.54f, 0.48f, 0.04f)
            poly(W, 0.3f, 0.1f, 0.7f, 0.1f, 0.53f, 0.32f, 0.7f, 0.54f, 0.3f, 0.54f, 0.47f, 0.32f)
            poly(Y, 0.37f, 0.14f, 0.63f, 0.14f, 0.5f, 0.3f); poly(Y, 0.36f, 0.54f, 0.64f, 0.54f, 0.5f, 0.42f)
            stroke(Y, 0.5f, 0.3f, 0.5f, 0.44f, 0.012f)
        }
        "cushion" -> { // A puffy cushion with a coin resting on it.
            drawRoundRect(L, Offset(w * 0.12f, h * 0.32f), Size(w * 0.76f, h * 0.22f), CornerRadius(w * 0.18f))
            drawRoundRect(W, Offset(w * 0.18f, h * 0.35f), Size(w * 0.64f, h * 0.06f), CornerRadius(w * 0.1f))
            listOf(0.13f to 0.33f, 0.87f to 0.33f, 0.13f to 0.53f, 0.87f to 0.53f).forEach { (x, y) -> dot(L, 0.05f, x, y) }
            coin(0.5f, 0.25f, 0.1f)
        }
        "small" -> { // Little coin stacks climbing into a big one.
            listOf(1, 2, 3, 5).forEachIndexed { i, n ->
                repeat(n) { k -> box(YD, 0.12f + i * 0.2f, 0.53f - k * 0.07f, 0.16f, 0.06f, 0.03f); box(Y, 0.12f + i * 0.2f, 0.52f - k * 0.07f, 0.16f, 0.05f, 0.03f) }
            }
            stroke(N, 0.14f, 0.3f, 0.5f, 0.1f, 0.04f); poly(N, 0.44f, 0.06f, 0.58f, 0.06f, 0.52f, 0.18f)
        }
        "pause" -> { // A pause button: a day to think before buying.
            dot(O, 0.3f, 0.5f, 0.3f); box(N, 0.38f, 0.19f, 0.08f, 0.22f, 0.03f); box(N, 0.54f, 0.19f, 0.08f, 0.22f, 0.03f)
            dot(P, 0.07f, 0.16f, 0.1f); dot(Y, 0.04f, 0.86f, 0.52f)
        }
        "purpose" -> { // A bullseye with the arrow right in it.
            dot(W, 0.3f, 0.46f, 0.32f); dot(O, 0.22f, 0.46f, 0.32f); dot(W, 0.14f, 0.46f, 0.32f); dot(O, 0.06f, 0.46f, 0.32f)
            stroke(Y, 0.46f, 0.32f, 0.86f, 0.08f, 0.03f); poly(Y, 0.84f, 0.03f, 0.94f, 0.05f, 0.88f, 0.12f)
        }
        "irregular" -> { // Months of different heights, planned from the lowest.
            listOf(0.3f, 0.18f, 0.36f, 0.12f, 0.26f).forEachIndexed { i, v -> box(if (i == 3) O else P, 0.1f + i * 0.17f, 0.56f - v, 0.12f, v, 0.03f) }
            for (i in 0..7) stroke(N, 0.06f + i * 0.12f, 0.44f, 0.11f + i * 0.12f, 0.44f, 0.015f)
        }
        "split" -> { // A pizza shared four ways.
            dot(O, 0.32f, 0.5f, 0.3f); dot(C, 0.27f, 0.5f, 0.3f)
            listOf(0.38f to 0.2f, 0.63f to 0.22f, 0.36f to 0.4f, 0.62f to 0.4f, 0.44f to 0.27f).forEach { (x, y) -> dot(P, 0.04f, x, y) }
            stroke(O, 0.5f, 0.11f, 0.5f, 0.49f, 0.025f); stroke(O, 0.23f, 0.3f, 0.77f, 0.3f, 0.025f)
        }
        "discounts" -> { // A price tag with a percent sign.
            poly(W, 0.18f, 0.12f, 0.62f, 0.12f, 0.86f, 0.34f, 0.62f, 0.56f, 0.18f, 0.56f)
            dot(PK, 0.04f, 0.7f, 0.34f)
            dot(N, 0.05f, 0.32f, 0.24f); dot(N, 0.05f, 0.5f, 0.44f); stroke(N, 0.52f, 0.2f, 0.3f, 0.48f, 0.03f)
        }
        "college" -> { // A graduation cap on a stack of books.
            box(P, 0.2f, 0.46f, 0.6f, 0.08f, 0.02f); box(W, 0.26f, 0.38f, 0.54f, 0.08f, 0.02f); box(N, 0.22f, 0.3f, 0.56f, 0.08f, 0.02f)
            box(N, 0.36f, 0.17f, 0.28f, 0.1f, 0.02f); poly(N, 0.5f, 0.06f, 0.88f, 0.15f, 0.5f, 0.24f, 0.12f, 0.15f)
            stroke(Y, 0.82f, 0.15f, 0.84f, 0.3f, 0.015f); dot(Y, 0.03f, 0.84f, 0.31f)
        }
        "invest" -> { // A tree growing from a coin pile.
            stroke(N, 0.5f, 0.52f, 0.5f, 0.26f, 0.05f); stroke(N, 0.5f, 0.36f, 0.64f, 0.24f, 0.03f)
            dot(Y, 0.16f, 0.44f, 0.18f); dot(O, 0.14f, 0.64f, 0.16f); dot(YL, 0.07f, 0.58f, 0.08f)
            coin(0.36f, 0.53f, 0.07f); coin(0.64f, 0.53f, 0.07f); coin(0.5f, 0.56f, 0.07f)
        }
        "saveinvest" -> { // A jar that keeps, and a line that may grow.
            box(L, 0.1f, 0.24f, 0.32f, 0.32f, 0.06f); box(P, 0.1f, 0.38f, 0.32f, 0.18f, 0.06f); box(N, 0.16f, 0.2f, 0.2f, 0.05f, 0.02f)
            stroke(O, 0.52f, 0.52f, 0.64f, 0.38f); stroke(O, 0.64f, 0.38f, 0.74f, 0.44f); stroke(O, 0.74f, 0.44f, 0.88f, 0.18f)
            poly(O, 0.82f, 0.15f, 0.94f, 0.11f, 0.92f, 0.23f)
        }
        "risk" -> { // A paper boat riding the waves, the sun overhead.
            dot(Y, 0.1f, 0.78f, 0.12f)
            drawPath(
                Path().apply {
                    moveTo(0f, h * 0.44f)
                    var x = 0f
                    while (x <= w) { lineTo(x, h * 0.44f + sin(x / w * 12f) * h * 0.025f); x += 4f }
                    lineTo(w, h * 0.6f); lineTo(0f, h * 0.6f); close()
                },
                P,
            )
            poly(W, 0.24f, 0.38f, 0.68f, 0.38f, 0.58f, 0.46f, 0.32f, 0.46f); poly(W, 0.44f, 0.2f, 0.44f, 0.37f, 0.6f, 0.37f)
        }
        "emergency" -> { // An umbrella over a coin, with rain all around.
            for (i in 0..5) stroke(N, 0.08f + i * 0.17f, 0.04f + (i % 2) * 0.06f, 0.06f + i * 0.17f, 0.1f + (i % 2) * 0.06f, 0.02f)
            drawArc(P, 180f, 180f, true, Offset(w * 0.14f, h * 0.16f), Size(w * 0.72f, w * 0.46f))
            stroke(N, 0.5f, 0.32f, 0.5f, 0.5f, 0.03f); coin(0.5f, 0.52f, 0.08f)
        }
        "beforeinvest" -> { // A clipboard checklist, mostly ticked.
            box(W, 0.24f, 0.1f, 0.52f, 0.48f, 0.04f); box(N, 0.38f, 0.06f, 0.24f, 0.07f, 0.03f)
            repeat(3) { i ->
                val y = 0.2f + i * 0.12f
                box(P, 0.31f, y, 0.08f, 0.06f, 0.015f)
                stroke(N, 0.45f, y + 0.03f, 0.68f, y + 0.03f, 0.025f)
                if (i < 2) { stroke(W, 0.325f, y + 0.03f, 0.345f, y + 0.045f, 0.015f); stroke(W, 0.345f, y + 0.045f, 0.38f, y + 0.012f, 0.015f) }
            }
        }
        else -> coin(0.5f, 0.3f, 0.16f)
    }
}

/** The second and third colours for a cover: picked from the palette by the book, never repeating its background. */
private fun accentsFor(id: String, bg: Color): Pair<Color, Color> {
    val wheel = listOf(Y, O, PK, P, L, C, DP)
    val pool = wheel.filter { it != bg }
    val h = hashOf(id)
    val second = pool[h % pool.size]
    val third = pool.filter { it != second }[(h / 7) % (pool.size - 1)]
    // Dark covers carry light pictures, so they deepen into another dark colour instead of a pale one.
    if (bg == N) return DP to third
    if (bg == DP) return N to third
    // Soften the second colour so the picture stays the star.
    return second.lighten(0.25f) to third
}
