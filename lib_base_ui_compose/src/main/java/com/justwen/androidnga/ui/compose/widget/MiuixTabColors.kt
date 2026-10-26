package com.justwen.androidnga.ui.compose.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.justwen.androidnga.ui.compose.miuix.isMiuixComposition
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape

/**
 * Miuix TabRow look for the shared reorderable tab row; null outside the Miuix appearance.
 * Matches Miuix TabRowDefaults: a rounded track in the surface colour, the selected tab as a
 * rounded block in surfaceContainer, selected text bold onBackground, others summary grey.
 */
@Immutable
internal class MiuixTabColors(
    val track: Color,
    val selectedBackground: Color,
    val selectedContent: Color,
    val unselectedContent: Color,
    val shape: Shape,
    val textStyle: TextStyle,
    val selectedTextStyle: TextStyle,
)

@Composable
internal fun rememberMiuixTabColors(): MiuixTabColors? {
    if (!isMiuixComposition) return null
    val colors = MiuixTheme.colorScheme
    val body = MiuixTheme.textStyles.body1
    return MiuixTabColors(
        track = colors.surface,
        selectedBackground = colors.surfaceContainer,
        selectedContent = colors.onBackground,
        unselectedContent = colors.onSurfaceVariantSummary,
        shape = miuixShape(TabRowDefaults.TabRowCornerRadius),
        textStyle = body.copy(fontWeight = FontWeight.Normal),
        selectedTextStyle = body.copy(fontWeight = FontWeight.Bold),
    )
}

/**
 * Horizontally scrolling Miuix-style tab track. The selected tab's block slides between tabs
 * and the row keeps the selected tab in view, as Miuix TabRow does; the tab items themselves
 * (with their reorder gestures) are supplied by [content].
 */
@Composable
internal fun MiuixReorderableTabRow(
    selectedTabIndex: Int,
    tabs: MiuixTabColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // Left/width of every measured tab, in row coordinates, to place the sliding block.
    val bounds = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val target = bounds[selectedTabIndex]
    val left by animateIntAsState(target?.first ?: 0, tween(200, easing = LinearEasing), label = "tabLeft")
    val width by animateIntAsState(target?.second ?: 0, tween(200, easing = LinearEasing), label = "tabWidth")
    var viewport by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedTabIndex, target, viewport) {
        val t = target ?: return@LaunchedEffect
        if (viewport > 0) {
            val centered = (t.first + t.second / 2 - viewport / 2).coerceIn(0, scroll.maxValue)
            scroll.animateScrollTo(centered)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(TabRowDefaults.TabRowHeight)
            .clip(tabs.shape)
            .background(tabs.track)
            .onSizeChanged { viewport = it.width },
    ) {
        Box(Modifier.fillMaxHeight().horizontalScroll(scroll)) {
            if (target != null && width > 0) {
                Box(
                    Modifier
                        .offset { IntOffset(left, 0) }
                        .width(with(density) { width.toDp() })
                        .fillMaxHeight()
                        .clip(tabs.shape)
                        .background(tabs.selectedBackground),
                )
            }
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalMiuixTabBounds provides bounds) {
                    content()
                }
            }
        }
    }
}

/** Receives each tab's position inside [MiuixReorderableTabRow]. */
internal val LocalMiuixTabBounds = staticCompositionLocalOf<MutableMap<Int, Pair<Int, Int>>?> { null }

/** One Miuix tab: centred label, bold when selected; the block behind it is drawn by the row. */
@Composable
internal fun MiuixTab(
    title: String,
    selected: Boolean,
    dragging: Boolean,
    tabs: MiuixTabColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int,
) {
    val bounds = LocalMiuixTabBounds.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = TabRowDefaults.TabRowMinWidth)
            .onPlaced { coordinates ->
                bounds?.set(index, coordinates.positionInParent().x.toInt() to coordinates.size.width)
            }
            .clip(tabs.shape)
            .then(if (dragging) Modifier.background(tabs.selectedBackground) else Modifier)
            .semantics { role = Role.Tab; this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = if (selected) tabs.selectedTextStyle else tabs.textStyle,
            color = if (selected) tabs.selectedContent else tabs.unselectedContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
