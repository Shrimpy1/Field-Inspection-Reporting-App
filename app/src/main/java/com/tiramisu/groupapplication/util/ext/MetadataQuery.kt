package com.tiramisu.groupapplication.util.ext

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

// ---- Query basic metadata ----
fun Context.querySize(uri: Uri): Long? {
    // 1) Try OpenableColumns.SIZE
    contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        ?.use { c: Cursor ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx != -1 && c.moveToFirst()) {
                val v = c.getLong(idx)
                if (v > 0) return v
            }
        }
    // 2) Try DocumentFile (needs androidx.documentfile)
    DocumentFile.fromSingleUri(this, uri)?.length()?.let { if (it >= 0) return it }
    // 3) Try file descriptor (stat size may be -1 if unknown)
    runCatching {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            if (afd.length >= 0) return afd.length
        }
    }
    return null
}

fun Context.queryDisplayName(uri: Uri): String? =
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }

fun Context.queryMime(uri: Uri): String? {
    // 1) Ask provider
    contentResolver.getType(uri)?.let { return it }
    // 2) Guess from extension
    val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    return if (!ext.isNullOrBlank())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
    else null
}

fun Context.copyToAppFiles(
    src: Uri,
    subdir: String = "attachments",
    fileName: String? = null
): Uri {
    val name = fileName ?: (queryDisplayName(src) ?: "${System.currentTimeMillis()}")
    val dir = File(filesDir, subdir).apply { mkdirs() }
    val dst = File(dir, name)
    contentResolver.openInputStream(src).use { input ->
        FileOutputStream(dst).use { out -> input!!.copyTo(out) }
    }
    return dst.toUri() // file://...; Coil can load this offline
}