package gov.anzong.androidnga.ui.miuix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sp.phone.mvp.model.entity.ThreadPageInfo

/**
 * LazyColumn keys must be storable in a Bundle on Android; a ThreadPageInfo key crashed every
 * Miuix topic list with "Type of the key ... is not supported". Keys must also be unique.
 */
class MiuixTopicRowsKeyTest {

    private fun info(tid: Int, pid: Int = 0) = ThreadPageInfo().apply {
        setTid(tid)
        setPid(pid)
    }

    @Test
    fun keysAreStringsUniqueEvenForEqualItems() {
        val items = listOf(info(1), info(2), info(1), info(2, 5))
        val keys = MiuixTopicRows.keyed(items).map { it.second }
        keys.forEach { assertTrue("key must be a String: $it", it is String) }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(listOf("1:0:0", "2:0:0", "1:0:0#2", "2:5:0"), keys)
    }

    @Test
    fun keyedKeepsTheSourceItems() {
        val items = listOf(info(7), info(8))
        assertEquals(items, MiuixTopicRows.keyed(items).map { it.first })
    }

    @Test
    fun replyRowsDropQuotedPostAndBbCode() {
        val raw = "[quote][pid=883163400,47598627,15]Reply[/pid] [b]Post by [uid=1]x[/uid] (2026-10-01):[/b]引用内容[/quote]带新，留ID"
        assertEquals("带新，留ID", MiuixTopicRows.readableReply(raw))
        assertEquals("50就够了", MiuixTopicRows.readableReply("[b]50就够了[/b]"))
        assertEquals("还真是，不会真是", MiuixTopicRows.readableReply("<br/>还真是，不会真是"))
    }
}
