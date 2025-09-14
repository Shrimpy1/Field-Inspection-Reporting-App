package com.tiramisu.groupapplication.util.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.get
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class QualityResult(
    val tooDark: Boolean,
    val tooBlurry: Boolean,
    val brightness: Int,   // 0..255 approx
    val sharpness: Int     // arbitrary units
)

suspend fun evaluateImageQuality(context: Context, uri: Uri): QualityResult =
    withContext(Dispatchers.Default) {
        val bmp = decodeDownscaled(context, uri, 256) ?: return@withContext QualityResult(
            tooDark = false,
            tooBlurry = false,
            brightness = 255,
            sharpness = 999
        )
        val (brightness, tooDark) = brightnessCheck(bmp)
        val (sharpness, tooBlurry) = blurCheck(bmp)
        QualityResult(tooDark, tooBlurry, brightness, sharpness)
    }

private fun decodeDownscaled(context: Context, uri: Uri, maxSize: Int): Bitmap? {
    return if (Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = min(maxSize.toFloat() / w, maxSize.toFloat() / h)
            if (scale < 1f) {
                decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }

        if (bmp.config == Bitmap.Config.HARDWARE) {
            bmp.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bmp
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)?.let { bmp ->
            val scale = min(maxSize.toFloat() / bmp.width, maxSize.toFloat() / bmp.height)
            if (scale < 1f) {
                bmp.scale((bmp.width * scale).toInt(), (bmp.height * scale).toInt())
            } else bmp
        }
    }
}

private fun brightnessCheck(bmp: Bitmap): Pair<Int, Boolean> {
    var sum = 0L;
    var count = 0
    val w = bmp.width;
    val h = bmp.height
    for (y in 0 until h step 2) for (x in 0 until w step 2) {
        val p = bmp[x, y]
        val l = (0.2126 * Color.red(p) + 0.7152 * Color.green(p) + 0.0722 * Color.blue(p)).toInt()
        sum += l; count++
    }
    val mean = (sum / max(1, count)).toInt()
    val tooDark = mean < 50              // tweak threshold
    return mean to tooDark
}

private fun blurCheck(bmp: Bitmap): Pair<Int, Boolean> {
    var gradSum = 0L;
    var count = 0
    val w = bmp.width;
    val h = bmp.height
    fun g(px: Int) =
        (0.299 * Color.red(px) + 0.587 * Color.green(px) + 0.114 * Color.blue(px)).toInt()
    for (y in 1 until h - 1 step 2) for (x in 1 until w - 1 step 2) {
        val gx = abs(g(bmp[x, y]) - g(bmp[x + 1, y]))
        val gy = abs(g(bmp[x, y]) - g(bmp[x, y + 1]))
        gradSum += (gx + gy); count++
    }
    val sharpness = (gradSum / max(1, count)).toInt()
    val tooBlurry = sharpness < 8        // tweak threshold
    return sharpness to tooBlurry
}