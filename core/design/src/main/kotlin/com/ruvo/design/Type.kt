@file:OptIn(ExperimentalTextApi::class)

package com.ruvo.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Anek Latin, one variable family for the whole brand. The width axis does the expressive work:
 * the runway number is set wide and heavy, body text sits at normal width.
 */
private fun anek(weight: Int, width: Float) = Font(
    resId = R.font.anek_latin,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

val AnekWide = FontFamily(anek(760, 125f), anek(600, 125f))
val AnekText = FontFamily(anek(400, 100f), anek(500, 100f), anek(600, 100f), anek(700, 100f))
val AnekCondensed = FontFamily(anek(600, 80f), anek(700, 80f))

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
    /** Small caps-style labels above numbers: RUNWAY, FUEL. */
    val eyebrow: TextStyle,
    val caption: TextStyle,
    /** Amounts in lists and cards, with equal-width digits. */
    val amount: TextStyle,
)

private const val TABULAR = "tnum"

val DefaultType = RuvoType(
    hero = TextStyle(fontFamily = AnekWide, fontWeight = FontWeight(760), fontSize = 124.sp, lineHeight = 112.sp, letterSpacing = (-0.04).em, fontFeatureSettings = TABULAR),
    display = TextStyle(fontFamily = AnekWide, fontWeight = FontWeight(600), fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em, fontFeatureSettings = TABULAR),
    headline = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(700), fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em),
    title = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(600), fontSize = 19.sp, lineHeight = 24.sp),
    body = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 23.sp),
    bodyStrong = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 23.sp),
    label = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(500), fontSize = 14.sp, lineHeight = 18.sp),
    eyebrow = TextStyle(fontFamily = AnekCondensed, fontWeight = FontWeight(700), fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.12.em),
    caption = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp),
    amount = TextStyle(fontFamily = AnekText, fontWeight = FontWeight(600), fontSize = 17.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR),
)

val LocalRuvoType = staticCompositionLocalOf { DefaultType }
