package gov.anzong.androidnga.ui.miuix

import sp.phone.common.PhoneConfiguration
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.param.TopicTitleHelper
import sp.phone.util.StringUtils

/**
 * Builds Miuix topic rows from the same ThreadPageInfo list and title formatting the classic
 * TopicListAdapter / ReplyListAdapter bind, so both appearances show identical data.
 */
object MiuixTopicRows {

    @JvmStatic
    fun topics(list: List<ThreadPageInfo>?): List<MiuixTopicRow> = keyed(list).map { (info, key) ->
        MiuixTopicRow(
            source = info,
            key = key,
            title = TopicTitleHelper.handleTitleFormat(info),
            author = info.author,
            lastReply = info.lastPoster,
            replies = info.replies.toString(),
        )
    }

    /** Reply search results: the reply text with its topic and time, as ReplyListAdapter shows. */
    @JvmStatic
    fun replies(list: List<ThreadPageInfo>?): List<MiuixTopicRow> = keyed(list).mapNotNull { (info, key) ->
        val reply = info.replyInfo ?: return@mapNotNull null
        MiuixTopicRow(
            source = info,
            key = key,
            title = reply.content.orEmpty(),
            author = null,
            lastReply = null,
            replies = null,
            summary = listOfNotNull(
                reply.subject?.takeIf(String::isNotEmpty),
                StringUtils.timeStamp2Date2(reply.postDate),
            ).joinToString(" · "),
        )
    }

    @JvmStatic
    fun titleSize(): Float = PhoneConfiguration.getInstance().topicTitleSize

    @JvmStatic
    fun info(row: MiuixTopicRow): ThreadPageInfo = row.source as ThreadPageInfo

    /**
     * Pairs each item with a String key derived from what ThreadPageInfo.equals compares
     * (tid, pid, cache entry). Repeats inside one list get an occurrence suffix, so keys stay
     * unique even if the adapter holds equal items.
     */
    internal fun keyed(list: List<ThreadPageInfo>?): List<Pair<ThreadPageInfo, String>> {
        val seen = HashMap<String, Int>()
        return list.orEmpty().map { info ->
            val base = "${info.tid}:${info.pid}:${info.cacheEntry?.hashCode() ?: 0}"
            val n = seen.merge(base, 1, Int::plus) ?: 1
            info to if (n == 1) base else "$base#$n"
        }
    }
}
