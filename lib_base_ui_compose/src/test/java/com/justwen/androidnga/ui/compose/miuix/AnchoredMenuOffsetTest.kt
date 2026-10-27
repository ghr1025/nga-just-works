package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class AnchoredMenuOffsetTest {

    // 1080x2400 window, status bar 100px, navigation bar 60px; 20px side margins like Miuix.
    private val window = IntRect(0, 100, 1080, 2340)
    private val margin = IntRect(52, 0, 52, 0)
    private val menu = IntSize(560, 900)

    private fun anchorAt(top: Int) = IntRect(950, top, 1050, top + 100)

    @Test
    fun opensBelowTheAnchorWhenItFits() {
        val anchor = anchorAt(500)
        assertEquals(IntOffset(1050 - 560 - 52, 600), anchoredMenuOffset(anchor, window, menu, margin))
    }

    @Test
    fun opensAboveTheAnchorNearTheBottom() {
        // The floor's "more" button low on the screen: the menu ends at the button's top edge.
        val anchor = anchorAt(1900)
        assertEquals(IntOffset(1050 - 560 - 52, 1900 - 900), anchoredMenuOffset(anchor, window, menu, margin))
    }

    @Test
    fun staysInsideTheWindowWhenNeitherSideFits() {
        val tall = IntSize(560, 2000)
        val offset = anchoredMenuOffset(anchorAt(1200), window, tall, margin)
        assertEquals(2340 - 2000, offset.y)
    }

    @Test
    fun keepsTheMenuInsideTheSideMargins() {
        val leftAnchor = IntRect(10, 500, 60, 560)
        assertEquals(52, anchoredMenuOffset(leftAnchor, window, menu, margin).x)
    }
}
