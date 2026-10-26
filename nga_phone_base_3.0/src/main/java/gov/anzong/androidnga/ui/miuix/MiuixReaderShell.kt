package gov.anzong.androidnga.ui.miuix

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeOwner
import com.justwen.androidnga.ui.compose.miuix.MiuixHostMenuActions
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixPage
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.rememberHostOptionsMenu
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixCapsuleShape

/** Page tabs state the reader fragment publishes to its Miuix shell. */
class MiuixReaderTabsState {
    var pages by mutableStateOf<List<String>>(emptyList())
    var selected by mutableIntStateOf(0)
}

/** Callbacks from the Miuix reader shell to the classic reader fragment. */
class MiuixReaderCallbacks(
    val onTabSelected: (Int) -> Unit,
    /** Tap on the already selected page: back to the top of that page. */
    val onTabReselected: () -> Unit,
    /** Null keeps the title inert, as in the classic reader. */
    val onTitleClick: (() -> Unit)?,
    /** Null hides the reply button (cached threads). */
    val onReply: (() -> Unit)?,
    /** Long-press repeat on the reply button: refresh the current page. */
    val onReplyLongPressRepeat: (() -> Unit)?,
    val longPressRepeatMillis: Long,
)

/**
 * Wraps the classic reader so the Miuix appearance gets a Miuix top app bar, page tab row and
 * reply button, while the floors keep rendering in the existing views. Floors are NGA HTML in
 * WebViews (quotes, images, votes), which Compose cannot replace without rewriting the reader;
 * the classic views keep every reader behaviour (anchors, prefetch, caching).
 *
 * [content] is the part of [classicRoot] shown in the Miuix page body (it may be the root
 * itself). The classic tree stays in the returned wrapper until the first composition so view
 * bindings made in onViewCreated still resolve; the page then moves [content] into its body.
 */
fun miuixReaderShell(
    classicRoot: View,
    content: View,
    activity: Activity,
    tabs: MiuixReaderTabsState,
    callbacks: MiuixReaderCallbacks,
): View {
    classicRoot.visibility = View.GONE
    val compose = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NgaMiuixTheme {
                MiuixReaderPage(activity, content, tabs, callbacks)
            }
        }
    }
    return FrameLayout(activity).apply {
        // The classic root stays attached (hidden) so ButterKnife-bound views keep working.
        addView(classicRoot, ViewGroup.LayoutParams(0, 0))
        addView(compose, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}

@Composable
private fun MiuixReaderPage(
    activity: Activity,
    content: View,
    tabs: MiuixReaderTabsState,
    callbacks: MiuixReaderCallbacks,
) {
    val chrome = (activity as? MiuixChromeOwner)?.miuixChrome
    val menu = rememberHostOptionsMenu(activity)
    MiuixPage(
        title = chrome?.title.orEmpty(),
        largeTitle = false,
        navIcon = MiuixNavIcon.Back,
        onNavigationClick = {
            (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() ?: activity.finish()
        },
        actions = { MiuixHostMenuActions(activity, menu) },
        onTitleClick = callbacks.onTitleClick,
        topBarExtra = {
            if (tabs.pages.size > 1) {
                TabRow(
                    tabs = tabs.pages,
                    selectedTabIndex = tabs.selected.coerceIn(0, tabs.pages.lastIndex),
                    onTabSelected = { index ->
                        if (index == tabs.selected) callbacks.onTabReselected() else callbacks.onTabSelected(index)
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                )
            }
        },
        floatingActionButton = {
            val reply = callbacks.onReply
            if (reply != null) {
                ReplyButton(
                    onClick = reply,
                    onLongPressRepeat = callbacks.onReplyLongPressRepeat,
                    repeatMillis = callbacks.longPressRepeatMillis,
                )
            }
        },
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

/**
 * Miuix primary round button. Tap replies; holding it refreshes the current page immediately
 * and then every [repeatMillis] while held, like the classic button's LongPressRepeater.
 */
@Composable
private fun ReplyButton(onClick: () -> Unit, onLongPressRepeat: (() -> Unit)?, repeatMillis: Long) {
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(holding) {
        while (holding && onLongPressRepeat != null) {
            onLongPressRepeat()
            delay(repeatMillis)
        }
    }
    // Scaffold already lifts the FAB above the navigation bar. Same look as Miuix FloatingActionButton, but on the non-clickable Surface so the gesture
    // detector below owns both the tap and the hold-to-refresh.
    Surface(
        modifier = Modifier
            .semantics {
                role = Role.Button
                contentDescription = "回复"
            }
            .pointerInput(onLongPressRepeat) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = if (onLongPressRepeat != null) ({ holding = true }) else null,
                    onPress = {
                        tryAwaitRelease()
                        holding = false
                    },
                )
            },
        shape = miuixCapsuleShape(),
        color = MiuixTheme.colorScheme.primary,
        shadowElevation = FloatingActionButtonDefaults.ShadowElevation,
    ) {
        Box(
            modifier = Modifier.defaultMinSize(
                minWidth = FloatingActionButtonDefaults.MinWidth,
                minHeight = FloatingActionButtonDefaults.MinHeight,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Edit,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimary,
            )
        }
    }
}
