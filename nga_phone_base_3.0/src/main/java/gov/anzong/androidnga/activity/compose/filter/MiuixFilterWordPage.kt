package gov.anzong.androidnga.activity.compose.filter

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.justwen.androidnga.ui.compose.miuix.MiuixAddButton
import com.justwen.androidnga.ui.compose.miuix.MiuixListCardItem
import com.justwen.androidnga.ui.compose.miuix.MiuixMessage
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import kotlinx.coroutines.launch
import sp.phone.common.User
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private sealed interface FilterDialog {
    data class Add(val state: FilterState<*>) : FilterDialog
    data class Delete(val label: String, val onConfirm: () -> Unit) : FilterDialog
}

/**
 * Miuix filter-rule page: contour tabs per rule type, card list per tab, an add button and
 * Miuix dialogs for adding and deleting. Rules are still read and written by the view model.
 */
@Composable
internal fun MiuixFilterWordPage(page: MiuixPageContent, viewModel: FilterWordViewModel) {
    val states = viewModel.filterStateList
    val pagerState = rememberPagerState(pageCount = { states.size })
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<FilterDialog?>(null) }
    val current = states.getOrNull(pagerState.currentPage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = page.padding.calculateTopPadding()),
    ) {
        TabRowWithContour(
            tabs = states.map { it.title },
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            FilterTab(
                page = page,
                state = states[index],
                onDelete = { label, confirm -> dialog = FilterDialog.Delete(label, confirm) },
            )
        }
    }
    if (current?.addAction != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = miuixListPadding(page.padding).calculateBottomPadding()),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            MiuixAddButton(onClick = { dialog = FilterDialog.Add(current) }, contentDescription = "新增")
        }
    }
    FilterDialogs(dialog, onDismiss = { dialog = null })
}

@Composable
private fun FilterTab(
    page: MiuixPageContent,
    state: FilterState<*>,
    onDelete: (String, () -> Unit) -> Unit,
) {
    val data by state.filterData.observeAsState()
    val items = data.orEmpty().filterNotNull()
    if (items.isEmpty()) {
        MiuixMessage(text = state.tips ?: "暂无内容")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(page.scrollModifier),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = miuixListPadding(page.padding, extraBottom = 96.dp).calculateBottomPadding(),
        ),
    ) {
        if (!state.tips.isNullOrEmpty()) {
            item(key = "tips") {
                Text(
                    text = state.tips!!,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                )
            }
        }
        itemsIndexed(items) { index, item ->
            val label = filterLabel(item)
            MiuixListCardItem(index = index, count = items.size) {
                // BasicComponent's own onClick would compete with long press, so both gestures
                // live on one combinedClickable.
                BasicComponent(
                    title = label,
                    summary = if (state.removeAction != null) "长按删除" else null,
                    modifier = Modifier.combinedClickable(
                        onClick = { state.showAction?.invoke(item) },
                        onLongClick = state.removeAction?.let { remove ->
                            { onDelete(label) { remove(item) } }
                        },
                    ),
                )
            }
        }
    }
}

private fun filterLabel(item: Any): String = when (item) {
    is User -> item.userId + "/" + item.nickName
    is FilterKeyword -> item.keyword
    else -> item.toString()
}

@Composable
private fun FilterDialogs(dialog: FilterDialog?, onDismiss: () -> Unit) {
    var input by remember(dialog) { mutableStateOf("") }
    SuperDialog(
        show = dialog is FilterDialog.Add,
        title = "新增屏蔽项",
        summary = "用户请输入\"用户ID/用户名\"，关键词直接输入",
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        DialogButtons(
            confirmText = "确定",
            onDismiss = onDismiss,
            onConfirm = {
                (dialog as? FilterDialog.Add)?.state?.addAction?.invoke(input)
                onDismiss()
            },
        )
    }
    SuperDialog(
        show = dialog is FilterDialog.Delete,
        title = "删除屏蔽项",
        summary = (dialog as? FilterDialog.Delete)?.let { "确定要删除屏蔽项 “${it.label}” 吗？" },
        onDismissRequest = onDismiss,
    ) {
        DialogButtons(
            confirmText = "删除",
            onDismiss = onDismiss,
            onConfirm = {
                (dialog as? FilterDialog.Delete)?.onConfirm?.invoke()
                onDismiss()
            },
        )
    }
}

@Composable
internal fun DialogButtons(confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row {
        TextButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(20.dp))
        TextButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
