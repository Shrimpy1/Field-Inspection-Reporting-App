package com.tiramisu.groupapplication.util

fun extensionForMime(mime: String) = when (mime.lowercase()) {
    "image/png" -> ".png"
    "image/webp" -> ".webp"
    "image/heic", "image/heif" -> ".heic"
    else -> ".jpg"
}