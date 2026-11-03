package com.ruvo.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ruvo's own icon set: 24 dp grid, 1.9 dp rounded strokes, soft liquid corners. No emoji anywhere.
 * Tint them with Icon(tint = ...); the stroke colour here is a placeholder that tinting replaces.
 */
object RuvoIcons {

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block,
            )
        }.build()

    /** Today: a sunrise over the day strip. */
    val Today = icon("today") {
        moveTo(3f, 18f); lineTo(21f, 18f)
        moveTo(6.5f, 14.5f); curveTo(6.5f, 11.5f, 9f, 9f, 12f, 9f); curveTo(15f, 9f, 17.5f, 11.5f, 17.5f, 14.5f)
        moveTo(12f, 3.5f); lineTo(12f, 5.5f)
        moveTo(5.2f, 6.2f); lineTo(6.6f, 7.6f)
        moveTo(18.8f, 6.2f); lineTo(17.4f, 7.6f)
        moveTo(7f, 21f); lineTo(17f, 21f)
    }

    /** Money: three stacked bucket bands. */
    val Money = icon("money") {
        moveTo(5f, 5f); lineTo(19f, 5f); quadTo(20.5f, 5f, 20.5f, 6.5f); lineTo(20.5f, 8f); quadTo(20.5f, 9.5f, 19f, 9.5f); lineTo(5f, 9.5f); quadTo(3.5f, 9.5f, 3.5f, 8f); lineTo(3.5f, 6.5f); quadTo(3.5f, 5f, 5f, 5f); close()
        moveTo(3.5f, 13f); lineTo(20.5f, 13f)
        moveTo(3.5f, 17f); lineTo(14f, 17f)
        moveTo(3.5f, 20.5f); lineTo(9f, 20.5f)
    }

    /** Goals: a jar with liquid inside. */
    val Goals = icon("goals") {
        moveTo(8f, 3.5f); lineTo(16f, 3.5f)
        moveTo(9f, 3.5f); lineTo(9f, 6f); curveTo(6.5f, 7f, 5f, 9f, 5f, 12f); lineTo(5f, 17.5f); quadTo(5f, 20.5f, 8f, 20.5f); lineTo(16f, 20.5f); quadTo(19f, 20.5f, 19f, 17.5f); lineTo(19f, 12f); curveTo(19f, 9f, 17.5f, 7f, 15f, 6f); lineTo(15f, 3.5f)
        moveTo(5.5f, 14f); curveTo(8f, 12.8f, 10f, 15.2f, 12f, 14f); curveTo(14f, 12.8f, 16f, 15.2f, 18.5f, 14f)
    }

    val Plus = icon("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    val Close = icon("close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    val Back = icon("back") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    val Forward = icon("forward") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    val Check = icon("check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }

    /** Fixed bucket: a pinned note. */
    val Fixed = icon("fixed") {
        moveTo(6f, 4f); lineTo(18f, 4f); quadTo(20f, 4f, 20f, 6f); lineTo(20f, 18f); quadTo(20f, 20f, 18f, 20f); lineTo(6f, 20f); quadTo(4f, 20f, 4f, 18f); lineTo(4f, 6f); quadTo(4f, 4f, 6f, 4f); close()
        moveTo(8f, 9f); lineTo(16f, 9f); moveTo(8f, 13f); lineTo(16f, 13f); moveTo(8f, 17f); lineTo(12f, 17f)
    }

    /** Fuel bucket: a drop. */
    val Fuel = icon("fuel") {
        moveTo(12f, 3f); curveTo(12f, 3f, 5.5f, 10f, 5.5f, 14.5f); curveTo(5.5f, 18f, 8.5f, 21f, 12f, 21f); curveTo(15.5f, 21f, 18.5f, 18f, 18.5f, 14.5f); curveTo(18.5f, 10f, 12f, 3f, 12f, 3f); close()
        moveTo(9f, 15f); curveTo(9f, 16.7f, 10.3f, 18f, 12f, 18f)
    }

    /** Future bucket: a seed sprouting. */
    val Future = icon("future") {
        moveTo(12f, 21f); lineTo(12f, 11f)
        moveTo(12f, 13f); curveTo(12f, 8.5f, 8.5f, 6f, 4.5f, 6f); curveTo(4.5f, 10f, 7.5f, 13f, 12f, 13f)
        moveTo(12f, 11f); curveTo(12f, 6.5f, 15f, 3.5f, 19.5f, 3.5f); curveTo(19.5f, 8f, 16.5f, 11f, 12f, 11f)
    }

    val Split = icon("split") {
        moveTo(12f, 21f); lineTo(12f, 13f); lineTo(6f, 7f); moveTo(6f, 11f); lineTo(6f, 7f); lineTo(10f, 7f)
        moveTo(12f, 13f); lineTo(18f, 7f); moveTo(14f, 7f); lineTo(18f, 7f); lineTo(18f, 11f)
    }

    val Message = icon("message") {
        moveTo(5f, 5f); lineTo(19f, 5f); quadTo(21f, 5f, 21f, 7f); lineTo(21f, 15f); quadTo(21f, 17f, 19f, 17f); lineTo(10f, 17f); lineTo(5.5f, 20.5f); lineTo(6f, 17f); lineTo(5f, 17f); quadTo(3f, 17f, 3f, 15f); lineTo(3f, 7f); quadTo(3f, 5f, 5f, 5f); close()
        moveTo(7.5f, 11f); lineTo(16.5f, 11f)
    }

    val Lock = icon("lock") {
        moveTo(6.5f, 10.5f); lineTo(17.5f, 10.5f); quadTo(19.5f, 10.5f, 19.5f, 12.5f); lineTo(19.5f, 18.5f); quadTo(19.5f, 20.5f, 17.5f, 20.5f); lineTo(6.5f, 20.5f); quadTo(4.5f, 20.5f, 4.5f, 18.5f); lineTo(4.5f, 12.5f); quadTo(4.5f, 10.5f, 6.5f, 10.5f); close()
        moveTo(8f, 10.5f); lineTo(8f, 7.5f); curveTo(8f, 5.3f, 9.8f, 3.5f, 12f, 3.5f); curveTo(14.2f, 3.5f, 16f, 5.3f, 16f, 7.5f); lineTo(16f, 10.5f)
        moveTo(12f, 14.5f); lineTo(12f, 16.5f)
    }

    val Settings = icon("settings") {
        moveTo(4f, 7f); lineTo(14f, 7f); moveTo(18f, 7f); lineTo(20f, 7f)
        moveTo(16f, 5f); curveTo(17.1f, 5f, 18f, 5.9f, 18f, 7f); curveTo(18f, 8.1f, 17.1f, 9f, 16f, 9f); curveTo(14.9f, 9f, 14f, 8.1f, 14f, 7f); curveTo(14f, 5.9f, 14.9f, 5f, 16f, 5f); close()
        moveTo(4f, 17f); lineTo(6f, 17f); moveTo(10f, 17f); lineTo(20f, 17f)
        moveTo(8f, 15f); curveTo(9.1f, 15f, 10f, 15.9f, 10f, 17f); curveTo(10f, 18.1f, 9.1f, 19f, 8f, 19f); curveTo(6.9f, 19f, 6f, 18.1f, 6f, 17f); curveTo(6f, 15.9f, 6.9f, 15f, 8f, 15f); close()
    }

    /** Ruvo speaking: three rising ripples. */
    val Spark = icon("spark") {
        moveTo(12f, 3.5f); curveTo(12.6f, 8.5f, 15.5f, 11.4f, 20.5f, 12f); curveTo(15.5f, 12.6f, 12.6f, 15.5f, 12f, 20.5f); curveTo(11.4f, 15.5f, 8.5f, 12.6f, 3.5f, 12f); curveTo(8.5f, 11.4f, 11.4f, 8.5f, 12f, 3.5f); close()
    }

    /** Perks: a ticket. */
    val Perk = icon("perk") {
        moveTo(4f, 7f); lineTo(20f, 7f); lineTo(20f, 10f); curveTo(18.9f, 10f, 18f, 10.9f, 18f, 12f); curveTo(18f, 13.1f, 18.9f, 14f, 20f, 14f); lineTo(20f, 17f); lineTo(4f, 17f); lineTo(4f, 14f); curveTo(5.1f, 14f, 6f, 13.1f, 6f, 12f); curveTo(6f, 10.9f, 5.1f, 10f, 4f, 10f); close()
        moveTo(14f, 7f); lineTo(14f, 8.5f); moveTo(14f, 11f); lineTo(14f, 13f); moveTo(14f, 15.5f); lineTo(14f, 17f)
    }

    val Calendar = icon("calendar") {
        moveTo(5.5f, 5.5f); lineTo(18.5f, 5.5f); quadTo(20.5f, 5.5f, 20.5f, 7.5f); lineTo(20.5f, 18.5f); quadTo(20.5f, 20.5f, 18.5f, 20.5f); lineTo(5.5f, 20.5f); quadTo(3.5f, 20.5f, 3.5f, 18.5f); lineTo(3.5f, 7.5f); quadTo(3.5f, 5.5f, 5.5f, 5.5f); close()
        moveTo(3.5f, 10f); lineTo(20.5f, 10f); moveTo(8f, 3.5f); lineTo(8f, 7f); moveTo(16f, 3.5f); lineTo(16f, 7f)
    }

    val Cart = icon("cart") {
        moveTo(3f, 4f); lineTo(5.5f, 4f); lineTo(7.5f, 15f); lineTo(18f, 15f); lineTo(20f, 7.5f); lineTo(6.3f, 7.5f)
        moveTo(9f, 19.5f); lineTo(9.01f, 19.5f); moveTo(17f, 19.5f); lineTo(17.01f, 19.5f)
    }

    val Speaker = icon("speaker") {
        moveTo(4f, 9.5f); lineTo(7.5f, 9.5f); lineTo(12f, 5.5f); lineTo(12f, 18.5f); lineTo(7.5f, 14.5f); lineTo(4f, 14.5f); close()
        moveTo(15.5f, 9f); curveTo(16.5f, 10f, 16.5f, 14f, 15.5f, 15f)
        moveTo(18f, 6.5f); curveTo(20.5f, 9f, 20.5f, 15f, 18f, 17.5f)
    }

    val Send = icon("send") { moveTo(4f, 12f); lineTo(20f, 4f); lineTo(14f, 20f); lineTo(11f, 13f); close() }

    val Book = icon("book") {
        moveTo(12f, 6.5f); curveTo(9.5f, 5f, 6.5f, 4.5f, 3.5f, 5f); lineTo(3.5f, 18.5f); curveTo(6.5f, 18f, 9.5f, 18.5f, 12f, 20f); curveTo(14.5f, 18.5f, 17.5f, 18f, 20.5f, 18.5f); lineTo(20.5f, 5f); curveTo(17.5f, 4.5f, 14.5f, 5f, 12f, 6.5f); close()
        moveTo(12f, 6.5f); lineTo(12f, 20f)
    }

    fun forBucket(b: com.ruvo.engine.Bucket) = when (b) {
        com.ruvo.engine.Bucket.FIXED -> Fixed
        com.ruvo.engine.Bucket.FUEL -> Fuel
        com.ruvo.engine.Bucket.FUTURE -> Future
    }
}
