package gov.anzong.androidnga.ui.miuix

import sp.phone.mvp.model.entity.ThreadPageInfo

/**
 * What a classic topic-list fragment offers its Miuix page. Each method mirrors what the
 * fragment's classic views already trigger, so both appearances share one behaviour owner.
 */
interface MiuixTopicListHost {
    /** Whether pull-to-refresh applies right now (caches and local lists disable it). */
    fun isMiuixRefreshEnabled(): Boolean

    fun onMiuixRefresh()

    /** End of the list reached; load the next page when the fragment pages. */
    fun onMiuixLoadNextPage()

    fun onMiuixItemClick(info: ThreadPageInfo)

    /** Confirmation text for long-press delete, or null when rows cannot be deleted. */
    fun miuixDeleteConfirmText(info: ThreadPageInfo): String?

    fun onMiuixDelete(info: ThreadPageInfo)

    fun onMiuixTitleClick()

    /** Primary action button (board lists post a topic), or null for none. */
    fun miuixFabAction(): Runnable?
}
