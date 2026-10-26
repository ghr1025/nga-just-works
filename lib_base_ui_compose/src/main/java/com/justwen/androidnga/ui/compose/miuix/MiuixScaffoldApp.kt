package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.runtime.Composable
import com.justwen.androidnga.ui.compose.widget.OptionMenuData
import com.justwen.androidnga.ui.compose.widget.TopAppBarData
import com.justwen.androidnga.ui.compose.widget.TopAppBarNavigationIcon
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix counterpart of the classic `ScaffoldApp`: the same [TopAppBarData] / FAB inputs drive a
 * [MiuixPage], so Compose screens built on the shared base classes switch shell without
 * duplicating their title, navigation and menu wiring.
 */
@Composable
fun MiuixScaffoldApp(
    topAppBarData: TopAppBarData,
    fabClick: (() -> Unit)? = null,
    largeTitle: Boolean = true,
    content: @Composable (MiuixPageContent) -> Unit,
) {
    MiuixPage(
        title = topAppBarData.title,
        largeTitle = largeTitle,
        navIcon = when (topAppBarData.navigationIcon) {
            TopAppBarNavigationIcon.Back -> MiuixNavIcon.Back
            TopAppBarNavigationIcon.Menu -> MiuixNavIcon.Menu
        },
        onNavigationClick = topAppBarData.navigationIconAction,
        actions = {
            MiuixActions(topAppBarData.optionMenuData.orEmpty().map { it.toMiuixAction() })
        },
        floatingActionButton = {
            if (fabClick != null) {
                MiuixAddButton(onClick = fabClick)
            }
        },
        content = content,
    )
}

/** The HyperOS round primary "add" button used for compose/post entry points. */
@Composable
fun MiuixAddButton(onClick: () -> Unit, contentDescription: String = "Add") {
    FloatingActionButton(onClick = onClick) {
        Icon(
            imageVector = MiuixIcons.Add,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.onPrimary,
        )
    }
}

fun OptionMenuData.toMiuixAction(): MiuixAction = MiuixAction(
    title = title.orEmpty(),
    iconRes = icon,
    alwaysShow = type == OptionMenuData.OPTION_MENU_TYPE_ALWAYS_SHOW,
    onClick = action,
)
