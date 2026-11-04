@file:OptIn(ExperimentalTextApi::class)

package com.ruvo.design

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Two families, each with one job.
 * Fraunces, a variable serif with optical sizes, is Ruvo's voice: numbers, headlines, the runway.
 *   Display sizes use the largest optical size so the strokes get crisp and high-contrast; headlines are a little
 *   soft (SOFT axis) so the voice reads warm rather than stern.
 * Space Grotesk sets the interface: labels, body text and amounts in lists, with tabular figures.
 * Both include the ₹ sign.
 */
private fun fraunces(weight: Int, opsz: Float, soft: Float = 0f, italic: Boolean = false) = Font(
    resId = R.font.fraunces,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
        FontVariation.Setting("SOFT", soft),
        FontVariation.Setting("WONK", 0f),
    ),
)

private fun grotesk(weight: Int) = Font(
    resId = R.font.space_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** The runway number and other big figures. */
val FrauncesDisplay = FontFamily(fraunces(800, 144f), fraunces(600, 144f))

/** Headlines and titles: warm, slightly soft. */
val FrauncesHeadline = FontFamily(fraunces(700, 48f, soft = 50f), fraunces(600, 24f, soft = 50f))

val Grotesk = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

@Immutable
data class RuvoType(
    /** The runway number. */
    val hero: TextStyle,
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    /** Small labels above numbers: RUNWAY, FUEL. Used sparingly. */
    val eyebrow: TextStyle,
    val caption: TextStyle,
    /** Amounts in lists and cards, with equal-width digits. */
    val amount: TextStyle,
)

private const val TABULAR = "tnum"

/** The type scale. The context parameter is kept so callers need not change. */
@Suppress("UNUSED_PARAMETER")
fun ruvoType(context: Context?): RuvoType = RuvoType(
    hero = TextStyle(fontFamily = FrauncesDisplay, fontWeight = FontWeight(800), fontSize = 128.sp, lineHeight = 112.sp, letterSpacing = (-0.045).em, fontFeatureSettings = "lnum, $TABULAR"),
    display = TextStyle(fontFamily = FrauncesDisplay, fontWeight = FontWeight(600), fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.025).em, fontFeatureSettings = "lnum"),
    headline = TextStyle(fontFamily = FrauncesHeadline, fontWeight = FontWeight(700), fontSize = 29.sp, lineHeight = 33.sp, letterSpacing = (-0.015).em),
    title = TextStyle(fontFamily = FrauncesHeadline, fontWeight = FontWeight(600), fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.005).em),
    body = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 23.sp),
    bodyStrong = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 23.sp),
    label = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight(500), fontSize = 14.sp, lineHeight = 18.sp),
    eyebrow = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight(700), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.14.em),
    caption = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp),
    amount = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight(600), fontSize = 17.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR),
)

val DefaultType = ruvoType(null)

val LocalRuvoType = staticCompositionLocalOf { DefaultType }
