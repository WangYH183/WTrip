package com.baguetteui.wtrip

import android.net.Uri
import java.io.File

object ImageCompressor {
    fun deleteIfLocalFile(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        runCatching { File(path).delete() }
    }
}