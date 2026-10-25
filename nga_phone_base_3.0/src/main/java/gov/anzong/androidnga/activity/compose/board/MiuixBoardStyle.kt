package gov.anzong.androidnga.activity.compose.board

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.SmallTitle as MiuixSmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape

/** Board group header in the Miuix appearance. */
@Composable
internal fun SmallTitle(text: String) = MiuixSmallTitle(text = text)

/** A board tile drawn as a small HyperOS card. */
@Composable
internal fun Modifier.miuixBoardTile(): Modifier = this
    .clip(miuixShape(16.dp))
    .background(MiuixTheme.colorScheme.surfaceContainer)

@Composable
internal fun miuixBoardTextColor(): Color = MiuixTheme.colorScheme.onSurfaceContainer

@Composable
internal fun miuixBoardHintColor(): Color = MiuixTheme.colorScheme.onSurfaceVariantSummary
