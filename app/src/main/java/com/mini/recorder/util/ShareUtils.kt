package com.mini.recorder.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mini.recorder.data.db.Recording
import java.io.File

object ShareUtils {
    fun share(context: Context, recording: Recording) {
        val file = File(recording.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = recording.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享錄音"))
    }
}
