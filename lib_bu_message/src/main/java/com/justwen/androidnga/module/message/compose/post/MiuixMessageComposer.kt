package com.justwen.androidnga.module.message.compose.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField

/**
 * Miuix composer for a private message. Field state is only a draft mirror: every edit is
 * written to the existing post model, which keeps validation and sending.
 */
@Composable
internal fun MiuixMessageComposer(
    page: MiuixPageContent,
    newPostMode: Boolean,
    initialRecipient: String,
    initialSubject: String,
    onRecipientChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var subject by remember { mutableStateOf(initialSubject) }
    var content by remember { mutableStateOf("") }
    val padding = miuixListPadding(page.padding)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .then(page.scrollModifier)
            .verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (newPostMode) {
            TextField(
                value = recipient,
                onValueChange = {
                    recipient = it
                    onRecipientChange(it)
                },
                label = "输入ID/用户名",
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
        }
        TextField(
            value = subject,
            onValueChange = {
                subject = it
                onSubjectChange(it)
            },
            label = "标题",
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
        TextField(
            value = content,
            onValueChange = {
                content = it
                onContentChange(it)
            },
            label = "内容",
            minLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp),
        )
        TextButton(
            text = "发送消息",
            onClick = onSend,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )
    }
}
