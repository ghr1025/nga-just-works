package gov.anzong.androidnga.activity.compose.drawer

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountLabelTest {

    @Test
    fun shortNamesStayWhole() {
        assertEquals("FluidCrystal", ellipsizeColumns("FluidCrystal", 20))
        assertEquals("测试账号二号", ellipsizeColumns("测试账号二号", 20))
    }

    @Test
    fun cjkCountsDoubleAndEndsWithEllipsis() {
        // 13 CJK characters = 26 columns > 20: keep 9 characters (18 columns) plus the ellipsis.
        assertEquals("一个名字特别长的测…", ellipsizeColumns("一个名字特别长的测试账号三号", 20))
    }

    @Test
    fun latinCountsSingle() {
        assertEquals("abcdefghijklmnopqrs…", ellipsizeColumns("abcdefghijklmnopqrstuvwxyz", 20))
    }
}
