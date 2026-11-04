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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruvo.design.Covers
import com.ruvo.design.Ruvo
import com.ruvo.design.hardShadow
import com.ruvo.design.pressable
import com.ruvo.engine.guide.Library

/** Stable, well-spread hash so a book always gets the same cover. */
private fun hashOf(s: String): Int = s.fold(7) { a, c -> (a * 31 + c.code) and 0x7fffffff }

/**
 * A book on the shelf: a flat, poster-style cover generated from the title, with a spine shadow and a white
 * label card. Same book, same cover, every time.
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
    val h = hashOf(book.id + book.title)
    val (bg, shape, accent) = Covers.palettes[h % Covers.palettes.size]
    val motif = (h / 7) % 6
    val height = width * 1.42f
    val corners = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    val interaction = remember { MutableInteractionSource() }
    var m = modifier.size(width, height).pressable(interaction)
        .hardShadow(corners, 4.dp, Ruvo.colors.ink.copy(alpha = 0.85f))
        .clip(corners)
    if (onClick != null) {
        m = m.clickable(interaction, indication = null, role = Role.Button, onClickLabel = "Open ${book.title}", onClick = onClick)
    }
    Box(m.semantics { contentDescription = "${book.title}, ${book.minutes} minute read${if (read) ", finished" else ""}" }) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(bg)
            drawMotif(motif, shape, accent)
            // Spine: a darker band and a soft highlight, like a printed paperback.
            drawRect(Color.Black.copy(alpha = 0.16f), size = Size(size.width * 0.045f, size.height))
            drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), startX = size.width * 0.045f, endX = size.width * 0.16f))
        }
        if (width >= 100.dp) Text(
            if (read) "Read" else "${book.pages.size} pages",
            Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(2.dp)).background(Covers.labelInk).padding(horizontal = 8.dp, vertical = 3.dp),
            style = Ruvo.type.eyebrow, color = Covers.label,
        )
        if (showLabel) {
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Covers.label).padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(book.title, style = Ruvo.type.label, color = Covers.labelInk, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(if (read) "Finished" else "${book.minutes} min read", style = Ruvo.type.caption, color = Covers.labelInk.copy(alpha = 0.6f))
            }
        }
    }
}

/** Six flat motifs: sun, rings, stacked pills, folded corner, dots, peaks. */
private fun DrawScope.drawMotif(motif: Int, a: Color, b: Color) {
    val w = size.width
    val h = size.height
    when (motif) {
        0 -> {
            drawCircle(a, w * 0.42f, Offset(w * 0.7f, h * 0.32f))
            drawCircle(b, w * 0.24f, Offset(w * 0.7f, h * 0.32f))
            drawRect(a.copy(alpha = 0.9f), Offset(0f, h * 0.6f), Size(w, h * 0.06f))
        }
        1 -> listOf(0.52f, 0.4f, 0.28f, 0.16f).forEachIndexed { i, r ->
            drawCircle(if (i % 2 == 0) a else b, w * r, Offset(w * 0.2f, h * 0.22f))
        }
        2 -> {
            val r = CornerRadius(h * 0.06f)
            drawRoundRect(a, Offset(w * 0.08f, h * 0.44f), Size(w * 0.56f, h * 0.12f), r)
            drawRoundRect(b, Offset(w * 0.24f, h * 0.29f), Size(w * 0.56f, h * 0.12f), r)
            drawRoundRect(a, Offset(w * 0.4f, h * 0.14f), Size(w * 0.52f, h * 0.12f), r)
            drawCircle(b, w * 0.08f, Offset(w * 0.18f, h * 0.18f))
        }
        3 -> {
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h * 0.5f); lineTo(0f, h * 0.32f); close() }, a)
            drawCircle(b, w * 0.17f, Offset(w * 0.7f, h * 0.2f))
            drawPath(Path().apply { moveTo(0f, h * 0.6f); lineTo(w, h * 0.46f); lineTo(w, h * 0.55f); lineTo(0f, h * 0.7f); close() }, b)
        }
        4 -> {
            for (i in 0 until 24) {
                val cx = w * (0.12f + (i % 6) * 0.16f)
                val cy = h * (0.08f + (i / 6) * 0.1f)
                drawCircle(if (i % 3 == 0) b else a, w * (0.03f + (i * 7 % 5) * 0.008f), Offset(cx, cy))
            }
            drawPath(Path().apply { moveTo(0f, h * 0.6f); quadraticBezierTo(w * 0.25f, h * 0.5f, w * 0.5f, h * 0.6f); quadraticBezierTo(w * 0.75f, h * 0.7f, w, h * 0.6f); lineTo(w, h); lineTo(0f, h); close() }, a)
        }
        else -> {
            drawPath(Path().apply { moveTo(w * 0.05f, h * 0.56f); lineTo(w * 0.37f, h * 0.12f); lineTo(w * 0.69f, h * 0.56f); close() }, a)
            drawPath(Path().apply { moveTo(w * 0.4f, h * 0.56f); lineTo(w * 0.72f, h * 0.17f); lineTo(w, h * 0.56f); close() }, b)
            drawCircle(a, w * 0.08f, Offset(w * 0.8f, h * 0.1f), style = Stroke(w * 0.03f))
        }
    }
}
