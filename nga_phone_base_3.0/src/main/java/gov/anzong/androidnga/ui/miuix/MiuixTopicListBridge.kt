package gov.anzong.androidnga.ui.miuix

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.ui.adapter.BaseAppendableAdapter
import sp.phone.ui.fragment.TopicSearchFragment

/**
 * Connects a classic [TopicSearchFragment] to its Miuix page.
 *
 * The fragment's adapter stays the single owner of the list data (paging, appends, removals,
 * de-duplication); this bridge observes it and republishes a snapshot to the Miuix page, and
 * forwards Miuix gestures to the fragment methods the classic views call.
 */
class MiuixTopicListBridge(
    private val fragment: TopicSearchFragment,
    private val replyResults: Boolean,
) {
    private val state = MiuixTopicListState().apply {
        titleSizeSp = MiuixTopicRows.titleSize()
        emptyText = "暂无内容"
    }

    private var adapter: BaseAppendableAdapter<*, *>? = null

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = publish()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = publish()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = publish()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = publish()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = publish()
    }

    fun wrap(classicView: View): View = miuixTopicListView(
        classicView = classicView,
        activity = fragment.requireActivity(),
        state = state,
        callbacks = MiuixTopicListCallbacks(
            onRefresh = { if (fragment.isMiuixRefreshEnabled) fragment.onMiuixRefresh() },
            onLoadNextPage = fragment::onMiuixLoadNextPage,
            onClick = { fragment.onMiuixItemClick(MiuixTopicRows.info(it)) },
            deleteConfirmText = { fragment.miuixDeleteConfirmText(MiuixTopicRows.info(it)) },
            onDelete = { fragment.onMiuixDelete(MiuixTopicRows.info(it)) },
            onTitleClick = fragment::onMiuixTitleClick,
            fab = fragment.miuixFabAction()?.let { action -> { action.run() } },
        ),
    )

    fun attach(adapter: BaseAppendableAdapter<*, *>) {
        this.adapter?.unregisterAdapterDataObserver(observer)
        this.adapter = adapter
        adapter.registerAdapterDataObserver(observer)
        publish()
    }

    fun setRefreshing(refreshing: Boolean) {
        state.refreshing = refreshing
        if (!refreshing) {
            state.initialLoading = false
        }
        publish()
    }

    fun scrollToTop() = state.requestScrollToTop()

    private fun publish() {
        val adapter = adapter ?: return
        @Suppress("UNCHECKED_CAST")
        val items = (0 until adapter.itemCount).map { adapter.getItem(it) as ThreadPageInfo }
        state.rows = if (replyResults) MiuixTopicRows.replies(items) else MiuixTopicRows.topics(items)
        state.hasNextPage = adapter.hasNextPage()
    }
}
