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
    fun topics(list: List<ThreadPageInfo>?): List<MiuixTopicRow> = list.orEmpty().map { info ->
        MiuixTopicRow(
            key = info,
            title = TopicTitleHelper.handleTitleFormat(info),
            author = info.author,
            lastReply = info.lastPoster,
            replies = info.replies.toString(),
        )
    }

    /** Reply search results: the reply text with its topic and time, as ReplyListAdapter shows. */
    @JvmStatic
    fun replies(list: List<ThreadPageInfo>?): List<MiuixTopicRow> = list.orEmpty().mapNotNull { info ->
        val reply = info.replyInfo ?: return@mapNotNull null
        MiuixTopicRow(
            key = info,
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
    fun info(row: MiuixTopicRow): ThreadPageInfo = row.key as ThreadPageInfo
}
