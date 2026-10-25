package com.justwen.androidnga.module.message.compose

import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.justwen.androidnga.core.data.MessageArticlePageInfo
import com.justwen.androidnga.core.data.MessageThreadPageInfo
import com.justwen.androidnga.module.message.R
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.MiuixPagingList
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape

private const val SystemSender = "#SYSTEM#"

/** HyperOS-style list of private message threads: avatar, sender, subject, date and replies. */
@Composable
internal fun MiuixMessageList(
    page: MiuixPageContent,
    items: LazyPagingItems<MessageThreadPageInfo>,
    onOpen: (MessageThreadPageInfo) -> Unit,
) {
    MiuixPagingList(
        page = page,
        items = items,
        errorText = stringResource(com.justwen.androidnga.ui.compose.R.string.error_load_failed),
        retryText = "重试",
    ) { message ->
        MiuixMessageThreadRow(message, onOpen)
    }
}

@Composable
private fun MiuixMessageThreadRow(message: MessageThreadPageInfo, onOpen: (MessageThreadPageInfo) -> Unit) {
    val sender = message.from_username.takeUnless(TextUtils::isEmpty) ?: SystemSender
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    BasicComponent(
        onClick = { onOpen(message) },
        startAction = {
            Image(
                painter = painterResource(R.drawable.default_avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(miuixShape(20.dp)),
            )
        },
        endActions = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = message.time.orEmpty().substringBefore(' '),
                    style = MiuixTheme.textStyles.footnote1,
                    color = summary,
                )
                Text(
                    text = "${message.posts} 回复",
                    style = MiuixTheme.textStyles.footnote1,
                    color = summary,
                )
            }
        },
    ) {
        Text(
            text = sender,
            style = MiuixTheme.textStyles.headline1,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = message.subject.orEmpty(),
            modifier = Modifier.padding(top = 2.dp),
            style = MiuixTheme.textStyles.body2,
            color = summary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Messages of one thread; links inside a message open in the browser as in the classic UI. */
@Composable
internal fun MiuixMessageDetailList(
    page: MiuixPageContent,
    items: LazyPagingItems<MessageArticlePageInfo>,
    onOpenLink: (String) -> Unit,
) {
    MiuixPagingList(
        page = page,
        items = items,
        errorText = stringResource(com.justwen.androidnga.ui.compose.R.string.error_load_failed),
        retryText = "重试",
    ) { message ->
        MiuixMessageArticle(message, onOpenLink)
    }
}

@Composable
private fun MiuixMessageArticle(message: MessageArticlePageInfo, onOpenLink: (String) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val bodyStyle = MiuixTheme.textStyles.body1.copy(color = colors.onSurfaceContainer)
    val text = buildAnnotatedString {
        // android.util.Pair: first is the text, second marks a link.
        message.contentSections.forEach { section ->
            if (section.second != true) {
                append(section.first)
            } else {
                pushStringAnnotation(tag = "URL", annotation = section.first)
                withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Medium)) {
                    append(section.first)
                }
                pop()
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        if (!message.subject.isNullOrEmpty()) {
            Text(
                text = message.subject,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        @Suppress("DEPRECATION")
        ClickableText(
            text = text,
            style = bodyStyle,
            onClick = { offset ->
                text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { onOpenLink(it.item) }
            },
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = message.author.takeUnless(TextUtils::isEmpty) ?: SystemSender,
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
            Text(
                text = message.time,
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}
