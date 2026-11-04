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
 * Ruvo's own icon set: 24 dp grid, 1.7 dp rounded strokes, soft liquid corners. No emoji anywhere.
 * Tint them with Icon(tint = ...); the stroke colour here is a placeholder that tinting replaces.
 */
object RuvoIcons {

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block,
            )
        }.build()

    private fun solid(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f, strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
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

    /** Speak: a microphone. */
    val Mic = icon("mic") {
        moveTo(12f, 3.5f); curveTo(13.66f, 3.5f, 15f, 4.84f, 15f, 6.5f); lineTo(15f, 11.5f); curveTo(15f, 13.16f, 13.66f, 14.5f, 12f, 14.5f)
        curveTo(10.34f, 14.5f, 9f, 13.16f, 9f, 11.5f); lineTo(9f, 6.5f); curveTo(9f, 4.84f, 10.34f, 3.5f, 12f, 3.5f); close()
        moveTo(5.5f, 11f); curveTo(5.5f, 14.6f, 8.4f, 17.5f, 12f, 17.5f); curveTo(15.6f, 17.5f, 18.5f, 14.6f, 18.5f, 11f)
        moveTo(12f, 17.5f); lineTo(12f, 20.5f)
    }

    /** Show: an open eye. */
    val Eye = icon("eye") {
        moveTo(2.5f, 12f); curveTo(5f, 7f, 8.5f, 5f, 12f, 5f); curveTo(15.5f, 5f, 19f, 7f, 21.5f, 12f)
        curveTo(19f, 17f, 15.5f, 19f, 12f, 19f); curveTo(8.5f, 19f, 5f, 17f, 2.5f, 12f); close()
        moveTo(12f, 9f); curveTo(13.7f, 9f, 15f, 10.3f, 15f, 12f); curveTo(15f, 13.7f, 13.7f, 15f, 12f, 15f)
        curveTo(10.3f, 15f, 9f, 13.7f, 9f, 12f); curveTo(9f, 10.3f, 10.3f, 9f, 12f, 9f); close()
    }

    /** Hide: the eye, struck through. */
    val EyeOff = icon("eyeOff") {
        moveTo(2.5f, 12f); curveTo(5f, 7f, 8.5f, 5f, 12f, 5f); curveTo(15.5f, 5f, 19f, 7f, 21.5f, 12f)
        curveTo(19f, 17f, 15.5f, 19f, 12f, 19f); curveTo(8.5f, 19f, 5f, 17f, 2.5f, 12f); close()
        moveTo(4f, 4f); lineTo(20f, 20f)
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

    /** An idea worth noticing: a bulb with a filament. Replaces the old four-point spark everywhere. */
    val Bulb = icon("bulb") {
        moveTo(9f, 17.5f); lineTo(15f, 17.5f); moveTo(10f, 20.5f); lineTo(14f, 20.5f)
        moveTo(9f, 17.5f); lineTo(9f, 15.6f); curveTo(7.2f, 14.4f, 6f, 12.4f, 6f, 10.2f); curveTo(6f, 6.8f, 8.7f, 4f, 12f, 4f)
        curveTo(15.3f, 4f, 18f, 6.8f, 18f, 10.2f); curveTo(18f, 12.4f, 16.8f, 14.4f, 15f, 15.6f); lineTo(15f, 17.5f)
        moveTo(10.5f, 11f); lineTo(12f, 13f); lineTo(13.5f, 11f)
    }

    /** Save for later: a ribbon bookmark. */
    val Bookmark = icon("bookmark") {
        moveTo(7f, 3.5f); lineTo(17f, 3.5f); quadTo(18.5f, 3.5f, 18.5f, 5f); lineTo(18.5f, 20.5f); lineTo(12f, 16f); lineTo(5.5f, 20.5f); lineTo(5.5f, 5f); quadTo(5.5f, 3.5f, 7f, 3.5f); close()
    }

    /** Saved: the same ribbon, filled. */
    val BookmarkFilled = solid("bookmarkFilled") {
        moveTo(7f, 3.5f); lineTo(17f, 3.5f); quadTo(18.5f, 3.5f, 18.5f, 5f); lineTo(18.5f, 20.5f); lineTo(12f, 16f); lineTo(5.5f, 20.5f); lineTo(5.5f, 5f); quadTo(5.5f, 3.5f, 7f, 3.5f); close()
    }

    /** A month at a glance: three bars on a baseline. */
    val Chart = icon("chart") {
        moveTo(3.5f, 20.5f); lineTo(20.5f, 20.5f)
        moveTo(6.5f, 17f); lineTo(6.5f, 12f); moveTo(12f, 17f); lineTo(12f, 6f); moveTo(17.5f, 17f); lineTo(17.5f, 9.5f)
    }

    /** On track: a target with a centre dot. */
    val Target = icon("target") {
        moveTo(12f, 3.5f); curveTo(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f); curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f); curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f); curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f); close()
        moveTo(12f, 8f); curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f); curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f); curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f); curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f); close()
        moveTo(12f, 12f); lineTo(12.01f, 12f)
    }

    /** Marketplace: a shopfront with a scalloped awning. */
    val Store = icon("store") {
        moveTo(4f, 9f); lineTo(5.5f, 4f); lineTo(18.5f, 4f); lineTo(20f, 9f)
        moveTo(4f, 9f); curveTo(4f, 10.4f, 5.1f, 11.5f, 6.5f, 11.5f); curveTo(7.9f, 11.5f, 9.2f, 10.4f, 9.2f, 9f); curveTo(9.2f, 10.4f, 10.6f, 11.5f, 12f, 11.5f)
        curveTo(13.4f, 11.5f, 14.8f, 10.4f, 14.8f, 9f); curveTo(14.8f, 10.4f, 16.1f, 11.5f, 17.5f, 11.5f); curveTo(18.9f, 11.5f, 20f, 10.4f, 20f, 9f); close()
        moveTo(5.5f, 11.5f); lineTo(5.5f, 20f); lineTo(18.5f, 20f); lineTo(18.5f, 11.5f)
        moveTo(10f, 20f); lineTo(10f, 15.5f); lineTo(14f, 15.5f); lineTo(14f, 20f)
    }

    val User = icon("user") {
        moveTo(12f, 4f); curveTo(14.2f, 4f, 16f, 5.8f, 16f, 8f); curveTo(16f, 10.2f, 14.2f, 12f, 12f, 12f); curveTo(9.8f, 12f, 8f, 10.2f, 8f, 8f); curveTo(8f, 5.8f, 9.8f, 4f, 12f, 4f); close()
        moveTo(4.5f, 20.5f); curveTo(5.3f, 16.8f, 8.3f, 14.5f, 12f, 14.5f); curveTo(15.7f, 14.5f, 18.7f, 16.8f, 19.5f, 20.5f)
    }

    val Bell = icon("bell") {
        moveTo(6f, 16.5f); lineTo(6f, 10.5f); curveTo(6f, 7.2f, 8.7f, 4.5f, 12f, 4.5f); curveTo(15.3f, 4.5f, 18f, 7.2f, 18f, 10.5f); lineTo(18f, 16.5f); lineTo(19.5f, 18f); lineTo(4.5f, 18f); close()
        moveTo(10f, 20.5f); lineTo(14f, 20.5f)
    }

    val Shield = icon("shield") {
        moveTo(12f, 3.5f); lineTo(19f, 6f); lineTo(19f, 11.5f); curveTo(19f, 15.8f, 16f, 19.2f, 12f, 20.5f); curveTo(8f, 19.2f, 5f, 15.8f, 5f, 11.5f); lineTo(5f, 6f); close()
        moveTo(9f, 12f); lineTo(11.2f, 14.2f); lineTo(15.2f, 10f)
    }

    val Download = icon("download") {
        moveTo(12f, 4f); lineTo(12f, 15f); moveTo(7.5f, 10.5f); lineTo(12f, 15f); lineTo(16.5f, 10.5f); moveTo(5f, 19.5f); lineTo(19f, 19.5f)
    }

    val Trash = icon("trash") {
        moveTo(4.5f, 6.5f); lineTo(19.5f, 6.5f); moveTo(9.5f, 6.5f); lineTo(9.5f, 4f); lineTo(14.5f, 4f); lineTo(14.5f, 6.5f)
        moveTo(6.5f, 6.5f); lineTo(7.5f, 19f); quadTo(7.6f, 20.5f, 9f, 20.5f); lineTo(15f, 20.5f); quadTo(16.4f, 20.5f, 16.5f, 19f); lineTo(17.5f, 6.5f)
    }

    val Help = icon("help") {
        moveTo(12f, 3.5f); curveTo(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f); curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f); curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f); curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f); close()
        moveTo(9.5f, 9.5f); curveTo(9.5f, 8.1f, 10.6f, 7.2f, 12f, 7.2f); curveTo(13.4f, 7.2f, 14.5f, 8.1f, 14.5f, 9.4f); curveTo(14.5f, 11.2f, 12f, 11.4f, 12f, 13.3f)
        moveTo(12f, 16.6f); lineTo(12.01f, 16.6f)
    }

    val Moon = icon("moon") {
        moveTo(19.5f, 14.5f); curveTo(18.4f, 15f, 17.2f, 15.2f, 16f, 15.2f); curveTo(11.7f, 15.2f, 8.5f, 12f, 8.5f, 7.7f); curveTo(8.5f, 6.5f, 8.8f, 5.3f, 9.3f, 4.3f)
        curveTo(6f, 5.4f, 3.5f, 8.5f, 3.5f, 12.2f); curveTo(3.5f, 16.8f, 7.2f, 20.5f, 11.8f, 20.5f); curveTo(15.4f, 20.5f, 18.5f, 18.1f, 19.5f, 14.5f); close()
    }

    val Sun = icon("sun") {
        moveTo(12f, 8f); curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f); curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f); curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f); curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f); close()
        moveTo(12f, 2.5f); lineTo(12f, 4.5f); moveTo(12f, 19.5f); lineTo(12f, 21.5f); moveTo(2.5f, 12f); lineTo(4.5f, 12f); moveTo(19.5f, 12f); lineTo(21.5f, 12f)
        moveTo(5.3f, 5.3f); lineTo(6.7f, 6.7f); moveTo(17.3f, 17.3f); lineTo(18.7f, 18.7f); moveTo(5.3f, 18.7f); lineTo(6.7f, 17.3f); moveTo(17.3f, 6.7f); lineTo(18.7f, 5.3f)
    }

    val Wallet = icon("wallet") {
        moveTo(5.5f, 6f); lineTo(18f, 6f); quadTo(20.5f, 6f, 20.5f, 8.5f); lineTo(20.5f, 17.5f); quadTo(20.5f, 20f, 18f, 20f); lineTo(6f, 20f); quadTo(3.5f, 20f, 3.5f, 17.5f); lineTo(3.5f, 8f); quadTo(3.5f, 6f, 5.5f, 6f); close()
        moveTo(5.5f, 6f); lineTo(15.5f, 3.5f); lineTo(16.5f, 6f)
        moveTo(15.5f, 12f); lineTo(20.5f, 12f); lineTo(20.5f, 15.5f); lineTo(15.5f, 15.5f); quadTo(14f, 15.5f, 14f, 13.75f); quadTo(14f, 12f, 15.5f, 12f); close()
    }

    // Category marks: the fallback logo for a merchant Ruvo has no brand tile for.

    val Food = icon("food") {
        moveTo(3.5f, 12f); lineTo(20.5f, 12f); curveTo(20.5f, 16.4f, 16.7f, 20f, 12f, 20f); curveTo(7.3f, 20f, 3.5f, 16.4f, 3.5f, 12f); close()
        moveTo(9f, 8.5f); curveTo(8f, 7.5f, 10f, 6f, 9f, 4.5f); moveTo(13f, 8.5f); curveTo(12f, 7.5f, 14f, 6f, 13f, 4.5f)
    }

    val Basket = icon("basket") {
        moveTo(3.5f, 9.5f); lineTo(20.5f, 9.5f); lineTo(18.5f, 19f); quadTo(18.2f, 20.5f, 16.7f, 20.5f); lineTo(7.3f, 20.5f); quadTo(5.8f, 20.5f, 5.5f, 19f); close()
        moveTo(8f, 9.5f); lineTo(11f, 4f); moveTo(16f, 9.5f); lineTo(13f, 4f); moveTo(9.5f, 13.5f); lineTo(9.5f, 16.5f); moveTo(14.5f, 13.5f); lineTo(14.5f, 16.5f)
    }

    val Bag = icon("bag") {
        moveTo(5.5f, 8f); lineTo(18.5f, 8f); lineTo(19.5f, 20.5f); lineTo(4.5f, 20.5f); close()
        moveTo(9f, 10.5f); lineTo(9f, 7f); curveTo(9f, 5f, 10.3f, 3.5f, 12f, 3.5f); curveTo(13.7f, 3.5f, 15f, 5f, 15f, 7f); lineTo(15f, 10.5f)
    }

    val Ride = icon("ride") {
        moveTo(4f, 15.5f); lineTo(4f, 12f); lineTo(6.5f, 7f); quadTo(7f, 6f, 8.2f, 6f); lineTo(15.8f, 6f); quadTo(17f, 6f, 17.5f, 7f); lineTo(20f, 12f); lineTo(20f, 15.5f); close()
        moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(7f, 15.5f); lineTo(7f, 18.5f); moveTo(17f, 15.5f); lineTo(17f, 18.5f)
    }

    val Bolt = icon("bolt") { moveTo(13.5f, 3f); lineTo(5.5f, 13.5f); lineTo(11.5f, 13.5f); lineTo(10.5f, 21f); lineTo(18.5f, 10.5f); lineTo(12.5f, 10.5f); close() }

    val Play = icon("play") {
        moveTo(5.5f, 5f); lineTo(18.5f, 5f); quadTo(20.5f, 5f, 20.5f, 7f); lineTo(20.5f, 17f); quadTo(20.5f, 19f, 18.5f, 19f); lineTo(5.5f, 19f); quadTo(3.5f, 19f, 3.5f, 17f); lineTo(3.5f, 7f); quadTo(3.5f, 5f, 5.5f, 5f); close()
        moveTo(10f, 9f); lineTo(15f, 12f); lineTo(10f, 15f); close()
    }

    val Cap = icon("cap") {
        moveTo(2.5f, 9.5f); lineTo(12f, 5f); lineTo(21.5f, 9.5f); lineTo(12f, 14f); close()
        moveTo(6.5f, 11.5f); lineTo(6.5f, 16f); curveTo(8f, 17.8f, 10f, 18.5f, 12f, 18.5f); curveTo(14f, 18.5f, 16f, 17.8f, 17.5f, 16f); lineTo(17.5f, 11.5f)
        moveTo(21.5f, 9.5f); lineTo(21.5f, 14f)
    }

    val Health = icon("health") {
        moveTo(12f, 20f); curveTo(7f, 16.5f, 3.5f, 13.3f, 3.5f, 9.3f); curveTo(3.5f, 6.6f, 5.6f, 4.5f, 8.2f, 4.5f); curveTo(9.8f, 4.5f, 11.2f, 5.3f, 12f, 6.6f)
        curveTo(12.8f, 5.3f, 14.2f, 4.5f, 15.8f, 4.5f); curveTo(18.4f, 4.5f, 20.5f, 6.6f, 20.5f, 9.3f); curveTo(20.5f, 13.3f, 17f, 16.5f, 12f, 20f); close()
    }

    val Repeat = icon("repeat") {
        moveTo(4f, 11f); lineTo(4f, 9.5f); quadTo(4f, 7f, 6.5f, 7f); lineTo(19f, 7f); moveTo(16f, 4f); lineTo(19f, 7f); lineTo(16f, 10f)
        moveTo(20f, 13f); lineTo(20f, 14.5f); quadTo(20f, 17f, 17.5f, 17f); lineTo(5f, 17f); moveTo(8f, 20f); lineTo(5f, 17f); lineTo(8f, 14f)
    }

    /** Money between people: two arrows passing. */
    val Transfer = icon("transfer") {
        moveTo(4f, 8f); lineTo(18f, 8f); moveTo(14.5f, 4.5f); lineTo(18f, 8f); lineTo(14.5f, 11.5f)
        moveTo(20f, 16f); lineTo(6f, 16f); moveTo(9.5f, 12.5f); lineTo(6f, 16f); lineTo(9.5f, 19.5f)
    }

    fun forCategory(category: String) = when (category) {
        "Food" -> Food
        "Groceries" -> Basket
        "Shopping" -> Bag
        "Transport" -> Ride
        "Bills", "Rent" -> Bolt
        "Entertainment" -> Play
        "Subscriptions" -> Repeat
        "Education" -> Cap
        "Health" -> Health
        "Income", "Transfer" -> Transfer
        else -> Wallet
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
