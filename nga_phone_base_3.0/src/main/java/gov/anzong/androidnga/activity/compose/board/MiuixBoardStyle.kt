package gov.anzong.androidnga.activity.compose.board

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A board tile drawn as a small HyperOS card. Like Miuix Card itself, the tile uses a plain
 * rounded rectangle: HWUI clips it in hardware, while the smooth G2 shape is a path clip that is
 * re-rasterised for every one of the many tiles on the board grid.
 */
private val BoardTileShape = RoundedCornerShape(16.dp)

@Composable
internal fun Modifier.miuixBoardTile(): Modifier = this
    .clip(BoardTileShape)
    .background(MiuixTheme.colorScheme.surfaceContainer)

@Composable
internal fun miuixBoardTextColor(): Color = MiuixTheme.colorScheme.onSurfaceContainer

@Composable
internal fun miuixBoardHintColor(): Color = MiuixTheme.colorScheme.onSurfaceVariantSummary
