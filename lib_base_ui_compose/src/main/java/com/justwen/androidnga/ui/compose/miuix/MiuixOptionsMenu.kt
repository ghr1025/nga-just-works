package com.justwen.androidnga.ui.compose.miuix

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.Window
import androidx.annotation.DrawableRes
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.get
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One visible options-menu entry of a legacy host, ready to render in a Miuix top bar. */
class MiuixMenuEntry internal constructor(
    val item: MenuItem,
    val title: String,
    val icon: Drawable?,
    val asAction: Boolean,
    val enabled: Boolean,
)

/** A plain Miuix action for screens that do not use an Android options menu. */
class MiuixAction(
    val title: String,
    val icon: ImageVector? = null,
    @param:DrawableRes val iconRes: Int? = null,
    val alwaysShow: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Builds the host's options menu exactly as the classic toolbar does: the Activity and every
 * attached fragment receive onCreateOptionsMenu/onPrepareOptionsMenu through the window
 * callback, so menu logic stays with its existing owners.
 */
@Composable
fun rememberHostOptionsMenu(activity: Activity?): List<MiuixMenuEntry> {
    val chrome = (activity as? MiuixChromeOwner)?.miuixChrome
    val generation = chrome?.menuGeneration ?: 0
    var resumeCount by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val entries by produceState(emptyList(), activity, generation, resumeCount) {
        value = activity?.let(::buildHostOptionsMenu).orEmpty()
    }
    return entries
}

private fun buildHostOptionsMenu(activity: Activity): List<MiuixMenuEntry> {
    val decor = activity.window?.decorView ?: return emptyList()
    val menu = PopupMenu(activity, decor).menu
    if (!activity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu)) return emptyList()
    activity.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, menu)
    val entries = ArrayList<MiuixMenuEntry>(menu.size())
    for (index in 0 until menu.size()) {
        val item = menu[index]
        if (!item.isVisible) continue
        entries += MiuixMenuEntry(
            item = item,
            title = item.title?.toString().orEmpty(),
            icon = item.icon,
            asAction = item.requestsActionButton(),
            enabled = item.isEnabled,
        )
    }
    return entries
}

/**
 * The classic toolbar promotes `ifRoom`/`always` items to icons. MenuItemImpl is the only
 * holder of that flag, and AppCompat's PopupMenu always produces MenuItemImpl instances.
 */
@SuppressLint("RestrictedApi")
private fun MenuItem.requestsActionButton(): Boolean {
    val impl = this as? MenuItemImpl ?: return false
    return impl.requiresActionButton() || impl.requestsActionButton()
}

/** Dispatches a selection the same way the classic toolbar does, then refreshes the menu. */
fun Activity.performHostMenuItem(item: MenuItem) {
    onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item)
    (this as? MiuixChromeOwner)?.miuixChrome?.invalidateMenu()
}

/**
 * Renders host menu entries: action entries become icon buttons (at most [maxActions]) and the
 * rest go to a Miuix overflow popup. [iconFor] lets the app map menu ids to Miuix icons.
 */
@Composable
fun RowScope.MiuixHostMenuActions(
    activity: Activity?,
    entries: List<MiuixMenuEntry>,
    iconFor: (MenuItem) -> ImageVector? = { null },
    maxActions: Int = 2,
    extraActions: List<MiuixAction> = emptyList(),
) {
    val promoted = entries.filter { it.asAction && (it.icon != null || iconFor(it.item) != null) }
        .take(maxActions)
    val overflow = entries.filterNot { it in promoted }
    val actions = extraActions.map { it.toHostAction() } + promoted.map { entry ->
        HostAction(entry.title, iconFor(entry.item), null, entry.icon, true, entry.enabled) {
            activity?.performHostMenuItem(entry.item)
        }
    } + overflow.map { entry ->
        HostAction(entry.title, null, null, null, false, entry.enabled) {
            activity?.performHostMenuItem(entry.item)
        }
    }
    MiuixActionRow(actions)
}

/** Renders plain [MiuixAction]s with the same icon/overflow split as host menus. */
@Composable
fun RowScope.MiuixActions(actions: List<MiuixAction>) {
    MiuixActionRow(actions.map { it.toHostAction() })
}

private fun MiuixAction.toHostAction() = HostAction(title, icon, iconRes, null, alwaysShow, enabled, onClick)

private class HostAction(
    val title: String,
    val vector: ImageVector?,
    val iconRes: Int?,
    val drawable: Drawable?,
    val asIcon: Boolean,
    val enabled: Boolean,
    val onClick: () -> Unit,
) {
    val hasIcon: Boolean get() = vector != null || iconRes != null || drawable != null
}

@Composable
private fun RowScope.MiuixActionRow(actions: List<HostAction>) {
    val icons = actions.filter { it.asIcon && it.hasIcon }
    val overflow = actions.filterNot { it in icons }
    icons.forEach { action ->
        key(action.title) {
            IconButton(
                onClick = action.onClick,
                enabled = action.enabled,
                modifier = Modifier.padding(end = if (overflow.isEmpty() && action === icons.last()) 16.dp else 0.dp),
            ) {
                ActionIcon(action)
            }
        }
    }
    if (overflow.isNotEmpty()) {
        MiuixOverflowMenu(
            items = overflow.map { it.title },
            enabled = overflow.map { it.enabled },
            onItemClick = { index -> overflow[index].onClick() },
        )
    }
}

@Composable
private fun ActionIcon(action: HostAction) {
    val tint = MiuixTheme.colorScheme.onBackground
    val vector = action.vector
    if (vector != null) {
        Icon(imageVector = vector, contentDescription = action.title, tint = tint)
        return
    }
    val iconRes = action.iconRes
    if (iconRes != null) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = action.title,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        return
    }
    val drawable = action.drawable ?: return
    val sizePx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val bitmap = remember(drawable, sizePx) {
        drawable.mutate().toBitmap(sizePx, sizePx).asImageBitmap()
    }
    Icon(bitmap = bitmap, contentDescription = action.title, tint = tint, modifier = Modifier.size(24.dp))
}

/** The HyperOS "more" button with a context-menu style popup. */
@Composable
fun MiuixOverflowMenu(
    items: List<String>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: List<Boolean> = emptyList(),
) {
    var showPopup by remember { mutableStateOf(false) }
    var holdDown by remember { mutableStateOf(false) }
    IconButton(
        modifier = modifier.padding(end = 16.dp),
        onClick = {
            showPopup = true
            holdDown = true
        },
        holdDownState = holdDown,
    ) {
        Icon(
            imageVector = MiuixIcons.More,
            contentDescription = "更多选项",
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
    WindowListPopup(
        show = showPopup,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd,
        onDismissRequest = { showPopup = false },
        onDismissFinished = { holdDown = false },
    ) {
        val dismiss = LocalDismissState.current
        ListPopupColumn {
            items.forEachIndexed { index, title ->
                key(index) {
                    DropdownImpl(
                        text = title,
                        optionSize = items.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = { selected ->
                            dismiss?.invoke()
                            if (enabled.getOrElse(selected) { true }) onItemClick(selected)
                        },
                    )
                }
            }
        }
    }
}
