package gov.anzong.androidnga.ui.miuix

import android.app.Activity
import androidx.activity.ComponentActivity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import com.justwen.androidnga.ui.compose.miuix.MiuixAddButton
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeOwner
import com.justwen.androidnga.ui.compose.miuix.MiuixDialogButtons
import com.justwen.androidnga.ui.compose.miuix.MiuixHostMenuActions
import com.justwen.androidnga.ui.compose.miuix.MiuixCardHorizontalMargin
import com.justwen.androidnga.ui.compose.miuix.MiuixLoading
import com.justwen.androidnga.ui.compose.miuix.MiuixMessage
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixPage
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.MiuixPullToRefresh
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixIcons
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import com.justwen.androidnga.ui.compose.miuix.rememberHostOptionsMenu
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * One row of a Miuix topic list. [title] keeps the NGA title styling (colour, bold, italic,
 * underline and the trailing type markers) produced by the classic TopicTitleHelper.
 */
@Immutable
class MiuixTopicRow(
    /** The source item; handed back to the fragment on clicks and deletes. */
    val source: Any,
    /**
     * LazyColumn key. Android requires keys to be Bundle-storable, so this is a String, unique
     * per row of one list.
     */
    val key: String,
    val title: CharSequence,
    val author: String?,
    val lastReply: String?,
    val replies: String?,
    /** Shown instead of author/last reply for reply search results. */
    val summary: String? = null,
) {
    /** Author and last replier as shown in the meta row, built once per row. */
    val people: String = listOfNotNull(author, lastReply?.let { "· $it" }).joinToString(" ")

    /** Cached styled title per colour pair; rows are recreated when the adapter data changes. */
    private var styledTitle: AnnotatedString? = null
    private var styledFor: Long = 0

    internal fun styledTitle(subtle: Color, accent: Color): AnnotatedString {
        val keyValue = subtle.value.toLong() * 31 + accent.value.toLong()
        val cached = styledTitle
        if (cached != null && styledFor == keyValue) return cached
        return title.toAnnotatedString(MiuixTitleColors(subtle, accent)).also {
            styledTitle = it
            styledFor = keyValue
        }
    }
}

/**
 * State that a legacy topic-list fragment pushes into its Miuix page. The fragment keeps
 * loading, paging and click handling; the page only renders and reports gestures back.
 */
class MiuixTopicListState {
    var rows by mutableStateOf<List<MiuixTopicRow>>(emptyList())

    /** Title size in sp from the existing topic-title-size preference. */
    var titleSizeSp by mutableFloatStateOf(17f)
    var refreshing by mutableStateOf(false)

    /** True until the first page arrived (or failed). */
    var initialLoading by mutableStateOf(true)
    var hasNextPage by mutableStateOf(true)
    var emptyText by mutableStateOf("暂无内容")

    /** Bumped by the fragment to scroll the list back to the top. */
    var scrollToTopRequest by mutableIntStateOf(0)
        private set

    fun requestScrollToTop() {
        scrollToTopRequest++
    }
}

/** Callbacks from a Miuix topic-list page to its fragment. */
class MiuixTopicListCallbacks(
    val onRefresh: (() -> Unit)?,
    val onLoadNextPage: () -> Unit,
    val onClick: (MiuixTopicRow) -> Unit,
    /** Returns the confirmation message for a long-press delete, or null if not deletable. */
    val deleteConfirmText: ((MiuixTopicRow) -> String?)? = null,
    val onDelete: ((MiuixTopicRow) -> Unit)? = null,
    val onTitleClick: (() -> Unit)? = null,
    val fab: (() -> Unit)? = null,
    val fabLongPressRepeat: (() -> Unit)? = null,
)

/**
 * Builds the Miuix page that replaces a legacy topic-list fragment view. The classic view is
 * kept (hidden) so ButterKnife bindings, presenters and observers keep working unchanged.
 */
fun miuixTopicListView(
    classicView: View?,
    activity: Activity,
    state: MiuixTopicListState,
    callbacks: MiuixTopicListCallbacks,
): View {
    val compose = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NgaMiuixTheme {
                MiuixTopicListPage(activity, state, callbacks)
            }
        }
    }
    if (classicView == null) return compose
    classicView.visibility = View.GONE
    return android.widget.FrameLayout(activity).apply {
        addView(classicView, ViewGroup.LayoutParams(0, 0))
        addView(compose, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}

@Composable
private fun MiuixTopicListPage(
    activity: Activity,
    state: MiuixTopicListState,
    callbacks: MiuixTopicListCallbacks,
) {
    val chrome = (activity as? MiuixChromeOwner)?.miuixChrome
    val menu = rememberHostOptionsMenu(activity)
    @OptIn(ExperimentalFoundationApi::class)
    val listState = rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(ahead = 480.dp, behind = 240.dp))
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Pair<MiuixTopicRow, String>?>(null) }
    LaunchedEffect(state.scrollToTopRequest) {
        if (state.scrollToTopRequest > 0) listState.scrollToItem(0)
    }
    MiuixPage(
        title = chrome?.title.orEmpty(),
        navIcon = MiuixNavIcon.Back,
        onNavigationClick = { (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() ?: activity.finish() },
        actions = { MiuixHostMenuActions(activity, menu) },
        floatingActionButton = {
            val fab = callbacks.fab
            if (fab != null) {
                MiuixAddButton(onClick = fab, contentDescription = "发帖")
            }
        },
        onTitleClick = callbacks.onTitleClick?.let { titleClick ->
            {
                scope.launch { listState.scrollToItem(0) }
                titleClick()
            }
        },
    ) { page ->
        TopicListBody(page, state, callbacks, listState) { row, message -> pendingDelete = row to message }
    }
    val delete = pendingDelete
    SuperDialog(
        show = delete != null,
        title = "删除",
        summary = delete?.second,
        onDismissRequest = { pendingDelete = null },
    ) {
        MiuixDialogButtons(
            confirmText = "删除",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                delete?.first?.let { callbacks.onDelete?.invoke(it) }
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun TopicListBody(
    page: MiuixPageContent,
    state: MiuixTopicListState,
    callbacks: MiuixTopicListCallbacks,
    listState: LazyListState,
    onRequestDelete: (MiuixTopicRow, String) -> Unit,
) {
    val rows = state.rows
    val list: @Composable () -> Unit = {
        when {
            rows.isEmpty() && state.initialLoading ->
                MiuixLoading(Modifier.padding(top = page.padding.calculateTopPadding()))

            rows.isEmpty() ->
                MiuixMessage(
                    text = state.emptyText,
                    modifier = Modifier.padding(top = page.padding.calculateTopPadding()),
                    actionText = if (callbacks.onRefresh != null) "重试" else null,
                    onAction = callbacks.onRefresh,
                )

            else -> TopicLazyList(page, state, callbacks, listState, onRequestDelete)
        }
    }
    val refresh = callbacks.onRefresh
    if (refresh == null) {
        list()
    } else {
        MiuixPullToRefresh(
            page = page,
            isRefreshing = state.refreshing && !state.initialLoading,
            onRefresh = refresh,
        ) {
            list()
        }
    }
}

@Composable
private fun TopicLazyList(
    page: MiuixPageContent,
    state: MiuixTopicListState,
    callbacks: MiuixTopicListCallbacks,
    listState: LazyListState,
    onRequestDelete: (MiuixTopicRow, String) -> Unit,
) {
    val rows = state.rows
    // Same trigger as RecyclerViewEx: reaching the last item while a next page exists.
    LaunchedEffect(listState, rows.size, state.hasNextPage) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { last -> state.hasNextPage && !state.refreshing && rows.isNotEmpty() && last >= rows.size }
            .collect { callbacks.onLoadNextPage() }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .then(page.scrollModifier),
        contentPadding = miuixListPadding(page.padding, extraBottom = if (callbacks.fab != null) 96.dp else 12.dp),
    ) {
        item(key = "top-gap") { Box(Modifier.padding(top = 12.dp)) }
        itemsIndexed(rows, key = { _, row -> row.key }, contentType = { _, _ -> "topic" }) { _, row ->
            // One card per topic, like HyperOS list cards; cards are separated by spacing.
            Card(
                modifier = Modifier
                    .padding(horizontal = MiuixCardHorizontalMargin)
                    .padding(bottom = 10.dp),
            ) {
                val onClick = remember(row, callbacks) { { callbacks.onClick(row) } }
                val onLongClick: (() -> Unit)? = remember(row, callbacks) {
                    callbacks.deleteConfirmText?.let { confirm ->
                        {
                            confirm(row)?.let { onRequestDelete(row, it) }
                            Unit
                        }
                    }
                }
                TopicRowContent(
                    row = row,
                    titleSizeSp = state.titleSizeSp,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            }
        }
        item(key = "footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.hasNextPage && state.refreshing) {
                    InfiniteProgressIndicator(color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}

@Composable
private fun TopicRowContent(
    row: MiuixTopicRow,
    titleSizeSp: Float,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val colors = MiuixTheme.colorScheme
    val summaryColor = colors.onSurfaceVariantSummary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = row.styledTitle(colors.onSurfaceVariantSummary, colors.primary),
            style = MiuixTheme.textStyles.body1.copy(fontSize = titleSizeSp.sp),
            color = colors.onSurfaceContainer,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (row.summary != null) {
            Text(
                text = row.summary,
                style = MiuixTheme.textStyles.body2,
                color = summaryColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Author and last replier share the start; the reply count is pinned to the end so
            // counts line up across rows regardless of name lengths.
            if (row.author != null) {
                Icon(NgaMiuixIcons.Person, contentDescription = null, tint = summaryColor, modifier = Modifier.size(15.dp))
            }
            MetaText(
                text = row.people,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 12.dp),
            )
            if (row.replies != null) {
                // Fixed-width end column: icon and count line up at the right edge in every row.
                Row(
                    modifier = Modifier.widthIn(min = 56.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(NgaMiuixIcons.Reply, contentDescription = null, tint = summaryColor, modifier = Modifier.size(15.dp))
                    MetaText(row.replies, Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Miuix replacements for the classic marker colours of TopicTitleHelper: the board suffix
 * (text_color_disabled, a beige meant for the cream classic background) and the attachment
 * marker (title_orange) become Miuix summary and primary colours. Author-chosen title colours
 * (red, blue, green, silver) keep their meaning and stay as they are.
 */
private class MiuixTitleColors(val subtle: Color, val accent: Color)

private val ClassicSubtleColors = setOf(0xFFC4BEAE.toInt(), 0xFF6F6F6F.toInt())
private val ClassicAccentColors = setOf(0xFFA06700.toInt())

/** Converts the classic Spanned title into Compose styles, keeping the NGA title colours. */
private fun CharSequence.toAnnotatedString(miuix: MiuixTitleColors): AnnotatedString {
    val spanned = this as? Spanned ?: return AnnotatedString(toString())
    return buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start < 0 || end <= start) return@forEach
            val style = when (span) {
                is ForegroundColorSpan -> SpanStyle(
                    color = when (span.foregroundColor) {
                        in ClassicSubtleColors -> miuix.subtle
                        in ClassicAccentColors -> miuix.accent
                        else -> Color(span.foregroundColor)
                    },
                )
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    else -> null
                }
                is UnderlineSpan -> SpanStyle(textDecoration = TextDecoration.Underline)
                else -> null
            }
            if (style != null) addStyle(style, start, end)
        }
    }
}
