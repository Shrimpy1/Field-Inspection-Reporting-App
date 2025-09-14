package com.tiramisu.groupapplication.util.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun extensionForMime(mime: String) = when (mime.lowercase()) {
    "image/png" -> ".png"
    "image/webp" -> ".webp"
    "image/heic", "image/heif" -> ".heic"
    else -> ".jpg"
}

// CameraUris.kt
fun createTempImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File.createTempFile("capture_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}