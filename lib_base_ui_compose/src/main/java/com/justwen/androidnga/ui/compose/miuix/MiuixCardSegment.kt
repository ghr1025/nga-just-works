package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Draws one segment of a card-grouped list: the Miuix card container color clipped so that only
 * the corners of the list's first/last row are rounded. Lazy lists cannot nest their items in a
 * single Card, so each row draws its segment of the shared card. Like Miuix Card, the clip uses
 * RoundedCornerShape because a G2 squircle clip breaks touch feedback.
 */
@Composable
internal fun Modifier.miuixCardSegment(top: Dp, bottom: Dp): Modifier {
    val shape = remember(top, bottom) {
        RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
    }
    return this
        .clip(shape)
        .background(MiuixTheme.colorScheme.surfaceContainer)
}

/** Provides the card content color for rows drawn on a card segment. */
@Composable
internal fun CardContentColor(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurfaceContainer) {
        content()
    }
}
