package com.ruvo.design

import android.app.Activity
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** User's choice in Settings. SYSTEM follows the phone. */
enum class ThemeChoice { SYSTEM, DAYBREAK, NIGHT_SWIM }

/** True when the phone's "Remove animations" setting is on. Everything then becomes instant fades. */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun RuvoTheme(choice: ThemeChoice = ThemeChoice.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.DAYBREAK -> false
        ThemeChoice.NIGHT_SWIM -> true
    }
    val colors = if (dark) NightSwim else Daybreak
    val context = LocalContext.current
    val reduced = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val type = remember(context) { ruvoType(context) }
    val material = if (dark) {
        darkColorScheme(
            primary = colors.fuel, onPrimary = colors.onAccent, secondary = colors.fixed, tertiary = colors.future,
            background = colors.canvas, onBackground = colors.ink, surface = colors.surface, onSurface = colors.ink,
            surfaceVariant = colors.raised, onSurfaceVariant = colors.inkSoft, outline = colors.hairline, error = colors.warning,
            surfaceContainerLow = colors.surface, surfaceContainer = colors.surface, surfaceContainerHigh = colors.raised,
        )
    } else {
        lightColorScheme(
            primary = colors.ink, onPrimary = colors.surface, secondary = colors.fixed, tertiary = colors.future,
            background = colors.canvas, onBackground = colors.ink, surface = colors.surface, onSurface = colors.ink,
            surfaceVariant = colors.raised, onSurfaceVariant = colors.inkSoft, outline = colors.hairline, error = colors.warning,
            surfaceContainerLow = colors.surface, surfaceContainer = colors.surface, surfaceContainerHigh = colors.raised,
        )
    }
    // Status and navigation bar icons follow the app's mood, not the phone's: dark icons on Daybreak.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
            (ctx as? Activity)?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalRuvoColors provides colors,
        LocalRuvoType provides type,
        LocalReducedMotion provides reduced,
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

/** Shorthand: `Ruvo.colors.fuel`, `Ruvo.type.hero`. */
object Ruvo {
    val colors: RuvoColors
        @Composable @ReadOnlyComposable get() = LocalRuvoColors.current
    val type: RuvoType
        @Composable @ReadOnlyComposable get() = LocalRuvoType.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

/**
 * The haptic vocabulary from the plan. Small, consistent, never decorative.
 * Uses View haptics so it works on every phone from Android 8 without the vibrate permission.
 */
object Haptics {
    /** Confirm a card in the sort stack. */
    fun tick(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    /** Before you buy result, a goal pour landing. */
    fun medium(view: View) = view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

    /** Firm click at the end of a pour or a confirm. */
    fun confirm(view: View) = view.performHapticFeedback(
        if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
    )

    /** Buffer goes negative: a calm double tick, never an alarm. */
    fun attention(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        view.postDelayed({ view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }, 90)
    }

    /** Mark shared: two light ticks. */
    fun split(view: View) = attention(view)

    /** Income arrives: a soft long buzz. */
    fun relief(view: View) = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
