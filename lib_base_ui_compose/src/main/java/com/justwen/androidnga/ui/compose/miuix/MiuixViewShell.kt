package com.justwen.androidnga.ui.compose.miuix

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Miuix chrome for a classic View screen: a small Miuix top app bar (title and options menu of
 * the host Activity, back navigation) above the existing content view.
 *
 * The classic root stays attached but hidden until the first composition, so view bindings made
 * right after inflation still resolve; the page then moves [content] into its body. Use it for
 * screens whose content is plain Views (forms, WebViews, pagers) that keep their logic.
 */
fun miuixViewShell(
    activity: Activity,
    classicRoot: View,
    content: View,
    largeTitle: Boolean = false,
    floatingAction: (@Composable () -> Unit)? = null,
): View {
    classicRoot.visibility = View.GONE
    val compose = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NgaMiuixTheme {
                MiuixViewShellPage(activity, content, largeTitle, floatingAction)
            }
        }
    }
    return FrameLayout(activity).apply {
        if (classicRoot !== content) {
            addView(classicRoot, ViewGroup.LayoutParams(0, 0))
        }
        addView(compose, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}

@Composable
private fun MiuixViewShellPage(
    activity: Activity,
    content: View,
    largeTitle: Boolean,
    floatingAction: (@Composable () -> Unit)?,
) {
    val chrome = (activity as? MiuixChromeOwner)?.miuixChrome
    val menu = rememberHostOptionsMenu(activity)
    MiuixPage(
        title = chrome?.title.orEmpty(),
        largeTitle = largeTitle,
        navIcon = MiuixNavIcon.Back,
        onNavigationClick = {
            (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() ?: activity.finish()
        },
        actions = { MiuixHostMenuActions(activity, menu) },
        floatingActionButton = { floatingAction?.invoke() },
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = page.padding.calculateTopPadding()),
        ) {
            AndroidView(
                factory = {
                    (content.parent as? ViewGroup)?.removeView(content)
                    content.visibility = View.VISIBLE
                    content
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
