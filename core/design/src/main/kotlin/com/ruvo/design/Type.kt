@file:OptIn(ExperimentalTextApi::class)

package com.ruvo.design

import android.content.Context
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import android.graphics.Typeface as AndroidTypeface
import android.graphics.fonts.Font as AndroidFont
import android.graphics.fonts.FontFamily as AndroidFontFamily

/**
 * Outfit, a rounded geometric face, for the whole brand: heavy and tight for numbers, open for reading.
 * Outfit has no ₹ sign, so on Android 10+ each weight is a typeface that falls back to Anek Latin at the
 * same weight for ₹ only. Older phones fall back to the system ₹.
 */
private fun outfitResource(weight: Int) = Font(
    resId = R.font.outfit,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun outfitFamily(context: Context?, weight: Int): FontFamily {
    if (context != null && Build.VERSION.SDK_INT >= 29) {
        runCatching {
            val res = context.resources
            val main = AndroidFontFamily.Builder(
                AndroidFont.Builder(res, R.font.outfit).setWeight(weight).setFontVariationSettings("'wght' $weight").build(),
            ).build()
            val rupee = AndroidFontFamily.Builder(
                AndroidFont.Builder(res, R.font.anek_latin).setWeight(weight).setFontVariationSettings("'wght' $weight, 'wdth' 100").build(),
            ).build()
            val typeface = AndroidTypeface.CustomFallbackBuilder(main)
                .addCustomFallback(rupee)
                .setSystemFallback("sans-serif")
                .setStyle(FontStyle(weight, FontStyle.FONT_SLANT_UPRIGHT))
                .build()
            return FontFamily(Typeface(typeface))
        }
    }
    return FontFamily(outfitResource(weight))
}

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
    /** Small spaced labels above numbers: RUNWAY, FUEL. */
    val eyebrow: TextStyle,
    val caption: TextStyle,
    /** Amounts in lists and cards, with equal-width digits. */
    val amount: TextStyle,
)

private const val TABULAR = "tnum"

/** Builds the type scale. Pass a context to get the ₹ fallback; without one, Outfit alone is used. */
fun ruvoType(context: Context?): RuvoType {
    val w400 = outfitFamily(context, 400)
    val w500 = outfitFamily(context, 500)
    val w600 = outfitFamily(context, 600)
    val w700 = outfitFamily(context, 700)
    val w800 = outfitFamily(context, 800)
    return RuvoType(
        hero = TextStyle(fontFamily = w800, fontWeight = FontWeight(800), fontSize = 112.sp, lineHeight = 104.sp, letterSpacing = (-0.055).em, fontFeatureSettings = TABULAR),
        display = TextStyle(fontFamily = w800, fontWeight = FontWeight(800), fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.035).em, fontFeatureSettings = TABULAR),
        headline = TextStyle(fontFamily = w700, fontWeight = FontWeight(700), fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.025).em),
        title = TextStyle(fontFamily = w700, fontWeight = FontWeight(700), fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em),
        body = TextStyle(fontFamily = w400, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 23.sp),
        bodyStrong = TextStyle(fontFamily = w600, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 23.sp),
        label = TextStyle(fontFamily = w500, fontWeight = FontWeight(500), fontSize = 14.sp, lineHeight = 18.sp),
        eyebrow = TextStyle(fontFamily = w700, fontWeight = FontWeight(700), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.em),
        caption = TextStyle(fontFamily = w400, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp),
        amount = TextStyle(fontFamily = w700, fontWeight = FontWeight(700), fontSize = 17.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR),
    )
}

val DefaultType = ruvoType(null)

val LocalRuvoType = staticCompositionLocalOf { DefaultType }
