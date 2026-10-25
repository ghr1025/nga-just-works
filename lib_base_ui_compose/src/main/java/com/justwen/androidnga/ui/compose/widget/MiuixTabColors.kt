package com.justwen.androidnga.ui.compose.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.justwen.androidnga.ui.compose.miuix.isMiuixComposition
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix tokens for the shared reorderable tab row; null outside the Miuix appearance. */
@Immutable
internal class MiuixTabColors(
    val background: Color,
    val selectedContent: Color,
    val unselectedContent: Color,
    val textStyle: TextStyle,
)

@Composable
internal fun rememberMiuixTabColors(): MiuixTabColors? {
    if (!isMiuixComposition) return null
    val colors = MiuixTheme.colorScheme
    return MiuixTabColors(
        background = colors.surface,
        selectedContent = colors.primary,
        unselectedContent = colors.onSurfaceVariantSummary,
        textStyle = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
    )
}

/** Short capsule under the selected tab, as in HyperOS tab bars. */
@Composable
internal fun MiuixTabIndicator(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomCenter)
            .padding(bottom = 6.dp)
            .width(20.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}
