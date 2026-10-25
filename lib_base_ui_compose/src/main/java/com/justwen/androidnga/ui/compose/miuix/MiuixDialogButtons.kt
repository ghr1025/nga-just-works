package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton

/** Cancel + primary confirm row used at the bottom of Miuix dialogs. */
@Composable
fun MiuixDialogButtons(
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dismissText: String = "取消",
    confirmEnabled: Boolean = true,
) {
    Row {
        TextButton(text = dismissText, onClick = onDismiss, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(20.dp))
        TextButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
