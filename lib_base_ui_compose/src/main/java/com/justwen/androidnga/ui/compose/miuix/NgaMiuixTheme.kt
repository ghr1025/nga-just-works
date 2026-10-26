package com.justwen.androidnga.ui.compose.miuix

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Root theme for every Miuix screen.
 *
 * The app keeps owning day/night through `ThemeManager`, so the controller uses an explicit
 * Light/Dark scheme instead of following the system. A Material 2 palette derived from the same
 * Miuix scheme is provided outside [MiuixTheme] so that legacy Material content embedded in a
 * Miuix page harmonises, while Miuix keeps its own indication and overscroll.
 */
@Composable
fun NgaMiuixTheme(content: @Composable () -> Unit) {
    val dark = remember { NgaUiStyle.isNightMode() }
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity()?.also(::prepareWindow) }
    val miuixColors = remember(dark) { if (dark) darkColorScheme() else lightColorScheme() }
    val controller = remember(dark) {
        ThemeController(if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
    }
    val materialColors = remember(dark) { materialColorsOf(miuixColors, dark) }
    MaterialTheme(colors = materialColors) {
        MiuixTheme(controller = controller) {
            MiuixSystemBars(activity, dark)
            CompositionLocalProvider(LocalNgaMiuixStyle provides true, content = content)
        }
    }
}

internal val LocalNgaMiuixStyle = staticCompositionLocalOf { false }

/**
 * True inside [NgaMiuixTheme]. Shared composables that keep one implementation for both
 * appearances (for example the reorderable board tabs) use it to switch to Miuix tokens.
 */
val isMiuixComposition: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalNgaMiuixStyle.current

/**
 * Miuix popups and dialogs register `NavigationBackHandler`, which requires the view-tree
 * NavigationEventDispatcherOwner. ComponentActivity installs it only from setContentView(), so
 * fragment hosts that place their content directly into `android.R.id.content` need it here.
 * Edge-to-edge is enabled so that Miuix bars own the status and navigation bar insets on every
 * supported API level, matching the enforced behaviour of targetSdk 35+.
 */
private fun prepareWindow(activity: Activity) {
    val window = activity.window ?: return
    val decor = window.decorView
    if (activity is ComponentActivity && decor.findViewTreeNavigationEventDispatcherOwner() == null) {
        decor.setViewTreeNavigationEventDispatcherOwner(activity)
    }
    WindowCompat.setDecorFitsSystemWindows(window, false)
}

@Composable
private fun MiuixSystemBars(activity: Activity?, dark: Boolean) {
    val window = activity?.window ?: return
    SideEffect {
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Transparent.toArgb()
        window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private fun materialColorsOf(colors: Colors, dark: Boolean) = if (dark) {
    darkColors(
        primary = colors.primary,
        primaryVariant = colors.primaryVariant,
        secondary = colors.primary,
        background = colors.surface,
        surface = colors.surfaceContainer,
        error = colors.error,
        onPrimary = colors.onPrimary,
        onSecondary = colors.onPrimary,
        onBackground = colors.onSurface,
        onSurface = colors.onSurfaceContainer,
        onError = colors.onError,
    )
} else {
    lightColors(
        primary = colors.primary,
        primaryVariant = colors.primaryVariant,
        secondary = colors.primary,
        background = colors.surface,
        surface = colors.surfaceContainer,
        error = colors.error,
        onPrimary = colors.onPrimary,
        onSecondary = colors.onPrimary,
        onBackground = colors.onSurface,
        onSurface = colors.onSurfaceContainer,
        onError = colors.onError,
    )
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
