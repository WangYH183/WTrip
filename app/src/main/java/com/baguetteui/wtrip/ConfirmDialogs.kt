package com.baguetteui.wtrip

import android.content.Context
import androidx.appcompat.app.AlertDialog

object ConfirmDialogs {
    fun confirmDelete(
        context: Context,
        what: String,
        title: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("删除$what")
            .setMessage("确定删除「$title」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> onConfirm() }
            .show()
    }
}