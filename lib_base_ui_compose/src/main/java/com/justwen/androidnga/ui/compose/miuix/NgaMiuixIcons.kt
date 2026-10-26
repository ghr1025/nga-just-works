package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Application-owned outline icons for affordances the Miuix 0.8.8 icon set does not include.
 * Drawn with HyperOS proportions (24dp box, 1.7dp rounded strokes) so they sit next to
 * `MiuixIcons` without looking foreign. Tinted by the caller like every Miuix icon.
 */
object NgaMiuixIcons {

    val Menu: ImageVector by lazy {
        strokeIcon("NgaMenu") {
            moveTo(4.5f, 7f); lineTo(19.5f, 7f)
            moveTo(4.5f, 12f); lineTo(19.5f, 12f)
            moveTo(4.5f, 17f); lineTo(19.5f, 17f)
        }
    }

    val Person: ImageVector by lazy {
        strokeIcon("NgaPerson") {
            moveTo(12f, 12f)
            arcTo(3.6f, 3.6f, 0f, true, true, 12.001f, 12f)
            moveTo(5f, 19.5f)
            curveTo(5.8f, 16.2f, 8.6f, 14.5f, 12f, 14.5f)
            curveTo(15.4f, 14.5f, 18.2f, 16.2f, 19f, 19.5f)
        }
    }

    val Reply: ImageVector by lazy {
        strokeIcon("NgaReplyCount") {
            moveTo(6.5f, 5.5f); lineTo(17.5f, 5.5f)
            curveTo(18.6f, 5.5f, 19.5f, 6.4f, 19.5f, 7.5f)
            lineTo(19.5f, 14.5f)
            curveTo(19.5f, 15.6f, 18.6f, 16.5f, 17.5f, 16.5f)
            lineTo(10.5f, 16.5f); lineTo(6.5f, 19.5f); lineTo(6.5f, 16.5f)
            curveTo(5.4f, 16.5f, 4.5f, 15.6f, 4.5f, 14.5f)
            lineTo(4.5f, 7.5f)
            curveTo(4.5f, 6.4f, 5.4f, 5.5f, 6.5f, 5.5f)
            close()
        }
    }

    private fun strokeIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            pathBuilder = block,
        ).build()
}
