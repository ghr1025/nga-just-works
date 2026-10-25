package com.justwen.androidnga.module.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.justwen.androidnga.ui.compose.miuix.MiuixGroupCard
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import gov.anzong.androidnga.base.util.ToastUtils
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch

/** Miuix debug page; DebugManager keeps owning the debug switch and log files. */
@Composable
internal fun MiuixDebugPage(page: MiuixPageContent, onShare: () -> Unit) {
    var debugMode by remember { mutableStateOf(DebugManager.debugMode) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(page.scrollModifier),
    ) {
        Spacer(Modifier.height(page.padding.calculateTopPadding()))
        MiuixGroupCard {
            SuperSwitch(
                title = "调试模式",
                checked = debugMode,
                onCheckedChange = {
                    debugMode = it
                    DebugManager.debugMode = it
                    ToastUtils.show(if (it) "调试模式开启" else "调试模式关闭")
                },
            )
            SuperArrow(title = "分享日志", onClick = onShare)
            SuperArrow(
                title = "清除日志",
                onClick = {
                    if (DebugManager.debugMode) {
                        ToastUtils.show("请先关闭调试模式")
                    } else {
                        DebugManager.clearLogFiles()
                        ToastUtils.show("清除日志成功")
                    }
                },
            )
        }
    }
}
