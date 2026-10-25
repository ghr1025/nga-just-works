package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** Navigation affordance shown at the start of a Miuix top app bar. */
enum class MiuixNavIcon { None, Back, Menu }

/**
 * What [MiuixPage] hands to its content: the Scaffold padding and the page scroll wiring.
 *
 * Miuix 0.8.8 ships Java 21 bytecode while the project's kapt/javac run on JDK 17, so Kotlin
 * declarations in kapt modules must not mention Miuix types in their signatures. Pages pass
 * this lib-owned type around instead of Miuix's ScrollBehavior.
 */
@Stable
class MiuixPageContent internal constructor(
    val padding: PaddingValues,
    internal val scrollBehavior: ScrollBehavior,
) {
    /** For the page's main vertical scroller: overscroll, end haptic and the collapsing title. */
    val scrollModifier: Modifier
        get() = Modifier.miuixPageScroll(scrollBehavior)
}

/**
 * Standard Miuix page: Scaffold with a collapsing large-title [TopAppBar] (or a
 * [SmallTopAppBar] when [largeTitle] is false), wired to the content's nested scroll through
 * [MiuixPageContent.scrollModifier].
 */
@Composable
fun MiuixPage(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: Boolean = true,
    navIcon: MiuixNavIcon = MiuixNavIcon.Back,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    bottomBar: @Composable () -> Unit = {},
    topBarExtra: (@Composable () -> Unit)? = null,
    content: @Composable (page: MiuixPageContent) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = modifier,
        topBar = {
            val navigation: @Composable () -> Unit = {
                if (navIcon != MiuixNavIcon.None && onNavigationClick != null) {
                    MiuixNavigationIcon(navIcon, onNavigationClick)
                }
            }
            Column {
                if (largeTitle) {
                    TopAppBar(
                        title = title,
                        scrollBehavior = scrollBehavior,
                        navigationIcon = navigation,
                        actions = actions,
                    )
                } else {
                    SmallTopAppBar(
                        title = title,
                        scrollBehavior = scrollBehavior,
                        navigationIcon = navigation,
                        actions = actions,
                    )
                }
                topBarExtra?.invoke()
            }
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        bottomBar = bottomBar,
    ) { padding ->
        // Miuix top app bars pad the display cutout and side navigation bars horizontally; the
        // page body takes the same insets so cards line up with the title in landscape.
        Box(
            Modifier.windowInsetsPadding(
                WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal),
            ),
        ) {
            content(MiuixPageContent(padding, scrollBehavior))
        }
    }
}

@Composable
fun MiuixNavigationIcon(navIcon: MiuixNavIcon, onClick: () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    IconButton(
        modifier = Modifier.padding(start = 16.dp),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.graphicsLayer {
                if (navIcon == MiuixNavIcon.Back && layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            },
            imageVector = if (navIcon == MiuixNavIcon.Menu) NgaMiuixIcons.Menu else MiuixIcons.Back,
            contentDescription = if (navIcon == MiuixNavIcon.Menu) "打开侧边栏" else "返回",
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Scroll modifiers every Miuix page list uses: end-of-list haptic and the collapsing top app bar
 * connection, as in the Miuix example pages. The HyperOS overscroll itself comes from
 * MiuixTheme's LocalOverscrollFactory, so adding overScrollVertical() here would stack it twice.
 */
internal fun Modifier.miuixPageScroll(scrollBehavior: ScrollBehavior?): Modifier = this
    .scrollEndHaptic()
    .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
    .fillMaxHeight()

/**
 * Content padding for a scrolling list placed in [MiuixPage]: the Scaffold top padding plus the
 * navigation bar so the last item clears gesture/button navigation.
 */
@Composable
fun miuixListPadding(padding: PaddingValues, extraBottom: Dp = 12.dp): PaddingValues {
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        top = padding.calculateTopPadding(),
        bottom = maxOf(padding.calculateBottomPadding(), navigation) + extraBottom,
    )
}

/** Fills the page below the top app bar; for non-list content such as WebView or pagers. */
@Composable
fun MiuixPageBox(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding()),
    ) {
        content()
    }
}
