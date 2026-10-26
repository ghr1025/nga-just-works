package gov.anzong.androidnga.ui.miuix

import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.ui.adapter.BaseAppendableAdapter

/**
 * Connects a classic topic-list fragment ([host]) to its Miuix page.
 *
 * The fragment's adapter stays the single owner of the list data (paging, appends, removals,
 * de-duplication); this bridge observes it and republishes a snapshot to the Miuix page, and
 * forwards Miuix gestures to the fragment methods the classic views call.
 */
class MiuixTopicListBridge(
    private val fragment: Fragment,
    private val host: MiuixTopicListHost,
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
            onRefresh = { if (host.isMiuixRefreshEnabled()) host.onMiuixRefresh() else setRefreshing(false) },
            onLoadNextPage = host::onMiuixLoadNextPage,
            onClick = { host.onMiuixItemClick(MiuixTopicRows.info(it)) },
            deleteConfirmText = { host.miuixDeleteConfirmText(MiuixTopicRows.info(it)) },
            onDelete = { host.onMiuixDelete(MiuixTopicRows.info(it)) },
            onTitleClick = host::onMiuixTitleClick,
            fab = host.miuixFabAction()?.let { action -> { action.run() } },
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
        // Rows did not change; only the paging flag may have.
        adapter?.let { state.hasNextPage = it.hasNextPage() }
    }

    fun scrollToTop() = state.requestScrollToTop()

    /** For local lists that are complete on attach (no network load to wait for). */
    fun markLoaded() {
        state.initialLoading = false
        state.refreshing = false
        publish()
    }

    /** Rows already built for adapter items, so unchanged rows keep their instance. */
    private val rowCache = MiuixTopicRows.RowCache()

    private fun publish() {
        val adapter = adapter ?: return
        @Suppress("UNCHECKED_CAST")
        val items = (0 until adapter.itemCount).map { adapter.getItem(it) as ThreadPageInfo }
        val rows = rowCache.rows(items, replyResults)
        // Same row instances in the same order: nothing to recompose.
        if (rows.size != state.rows.size || rows.indices.any { rows[it] !== state.rows[it] }) {
            state.rows = rows
        }
        state.hasNextPage = adapter.hasNextPage()
    }
}
