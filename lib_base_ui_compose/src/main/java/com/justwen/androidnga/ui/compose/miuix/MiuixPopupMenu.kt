package com.justwen.androidnga.ui.compose.miuix

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.get
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.function.Consumer
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.theme.LocalDismissState

/**
 * Shows an Android [Menu] as the Miuix floating context menu (the popup the Miuix top bar's
 * "more" button opens), anchored to [anchor]: white rounded list, dimmed window, opening from the
 * anchor's corner. It is the Miuix counterpart of `PopupMenu(context, anchor).show()`, so the
 * caller keeps building and preparing the menu as before and only swaps how it is shown.
 *
 * Visible items are rendered in order; disabled items are shown but ignore taps. The popup lives
 * in a ComposeView added to the activity's decor view for the time it is open.
 *
 * A ComposeView needs the view-tree lifecycle owner. Fragment hosts that put their content into
 * android.R.id.content without setContentView (the thread reader) never set it on the decor
 * view, so [ComponentActivity.initializeViewTreeOwners] sets it first; it is the AndroidX hook
 * setContentView itself uses and keeps owners that are already set.
 */
object MiuixPopupMenu {

    /**
     * Opens from the anchor like a context menu: right edges aligned, below the anchor when the
     * menu fits under it, otherwise above it (ContextMenuPositionProvider with TopEnd only opens
     * downwards, so for a button low on the screen it was pushed to the bottom edge).
     */
    internal val AnchoredMenuPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset = anchoredMenuOffset(anchorBounds, windowBounds, popupContentSize, popupMargin)

        override fun getMargins(): PaddingValues =
            ListPopupDefaults.ContextMenuPositionProvider.getMargins()
    }

    /** [onItemClick] receives the chosen enabled item (java.util.function.Consumer, for Java callers). */
    @JvmStatic
    fun show(anchor: View, menu: Menu, onItemClick: Consumer<MenuItem>) {
        val activity = anchor.context.findActivity() as? ComponentActivity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        val decor = activity.window?.decorView as? ViewGroup ?: return
        val items = (0 until menu.size()).map { menu[it] }.filter { it.isVisible }
        if (items.isEmpty()) return
        activity.initializeViewTreeOwners()

        // Anchor bounds relative to the decor view, which the popup host covers.
        val anchorOnScreen = IntArray(2).also(anchor::getLocationInWindow)
        val decorOnScreen = IntArray(2).also(decor::getLocationInWindow)
        val left = anchorOnScreen[0] - decorOnScreen[0]
        val top = anchorOnScreen[1] - decorOnScreen[1]
        val width = anchor.width
        val height = anchor.height

        lateinit var host: ComposeView
        // The popup is a Dialog window: remove the host with the page so it cannot outlive it.
        val pageGone = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                decor.removeView(host)
            }
        }
        host = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                NgaMiuixTheme {
                    var show by remember { mutableStateOf(true) }
                    val density = LocalDensity.current
                    // An invisible box with the anchor's bounds: WindowListPopup positions itself
                    // against its parent layout, like the top bar's overflow button. It sits in a
                    // full-size Box because the root of a MATCH_PARENT ComposeView gets fixed
                    // constraints, which would stretch a top-level size() box to the whole screen
                    // and open the menu against the screen instead of the anchor.
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(left, top) }
                                .size(with(density) { width.toDp() }, with(density) { height.toDp() }),
                        ) {
                            WindowListPopup(
                                show = show,
                                popupPositionProvider = AnchoredMenuPositionProvider,
                                alignment = PopupPositionProvider.Align.TopEnd,
                                onDismissRequest = { show = false },
                                onDismissFinished = {
                                    activity.lifecycle.removeObserver(pageGone)
                                    decor.post { decor.removeView(host) }
                                },
                            ) {
                                val dismiss = LocalDismissState.current
                                ListPopupColumn {
                                    items.forEachIndexed { index, item ->
                                        key(item.itemId, index) {
                                            DropdownImpl(
                                                text = item.title?.toString().orEmpty(),
                                                optionSize = items.size,
                                                isSelected = false,
                                                index = index,
                                                onSelectedIndexChange = { selected ->
                                                    dismiss?.invoke()
                                                    val chosen = items[selected]
                                                    if (chosen.isEnabled) onItemClick.accept(chosen)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        decor.addView(
            host,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        activity.lifecycle.addObserver(pageGone)
    }
}

/**
 * Position of a menu of [popup] size for an anchor (window coordinates): right-aligned with the
 * anchor, below it if there is room under it, else above it, else as low as fits; always kept
 * inside [window] minus [margin].
 */
internal fun anchoredMenuOffset(anchor: IntRect, window: IntRect, popup: IntSize, margin: IntRect): IntOffset {
    val x = (anchor.right - popup.width - margin.right)
        .coerceIn(window.left + margin.left, (window.right - popup.width - margin.right).coerceAtLeast(window.left + margin.left))
    val roomBelow = window.bottom - margin.bottom - anchor.bottom
    val roomAbove = anchor.top - (window.top + margin.top)
    val y = when {
        popup.height <= roomBelow -> anchor.bottom
        popup.height <= roomAbove -> anchor.top - popup.height
        else -> window.bottom - margin.bottom - popup.height
    }.coerceIn(window.top + margin.top, (window.bottom - margin.bottom - popup.height).coerceAtLeast(window.top + margin.top))
    return IntOffset(x, y)
}
