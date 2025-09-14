package com.tiramisu.groupapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {
    data class ReportData(
        val id: String,
        val title: String?,
        val type: String?,
        val score: String?,
        val notes: String?,
        val latitude: Double?,
        val longitude: Double?,
        val inspectorName: String?,
        val createdAtMillis: Long = System.currentTimeMillis()
    )

    suspend fun exportInspectionPdf(
        report: ReportData,
        // Each item = model for Coil (Uri/String), plus optional caption under the image
        images: List<Pair<Any, String?>> = emptyList()
    ): Uri {
        val doc = PdfDocument()

        // A4 @ 72dpi (PdfDocument uses "points")
        val pageW = 595
        val pageH = 842
        val margin = 32
        val contentW = pageW - margin * 2

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = Color.BLACK
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.DKGRAY
        }
        val rulePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        var pageNum = 1
        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
            val page = doc.startPage(info)
            return page to page.canvas
        }

        fun finishPage(page: PdfDocument.Page, canvas: Canvas) {
            // footer
            val footer = "Page $pageNum"
            val w = smallPaint.measureText(footer)
            canvas.drawText(footer, pageW - margin - w, pageH - margin / 2f, smallPaint)
            doc.finishPage(page)
            pageNum++
        }

        var (page, canvas) = newPage()
        var y = margin

        // Header
        canvas.drawText(
            "Inspection Report",
            margin.toFloat(),
            y + 6f + headerPaint.textSize,
            headerPaint
        )
        y += (headerPaint.textSize + 16).toInt()
        canvas.drawLine(
            margin.toFloat(),
            y.toFloat(),
            (pageW - margin).toFloat(),
            y.toFloat(),
            rulePaint
        )
        y += 12

        // Stamp block
        fun drawLine(label: String, value: String?) {
            val t = "$label: ${value ?: "—"}"
            y += (bodyPaint.textSize + 6).toInt()
            canvas.drawText(t, margin.toFloat(), y.toFloat(), bodyPaint)
        }

        drawLine("Title", report.title)
        drawLine("Type", report.type)
        drawLine("Score", report.score)
        drawLine("Notes", report.notes?.take(300)) // trim; keep it tidy
        drawLine(
            "GPS",
            report.latitude?.let { String.format("%.6f, %.6f", it, report.longitude ?: 0.0) })
        drawLine("Inspector", report.inspectorName ?: "—")
        drawLine("Date", dateFmt.format(Date(report.createdAtMillis)))

        y += 8
        canvas.drawLine(
            margin.toFloat(),
            y.toFloat(),
            (pageW - margin).toFloat(),
            y.toFloat(),
            rulePaint
        )
        y += 12

        // Images
        // Reserve max image width, maintain aspect ratio, wrap to new page as needed
        val imgMaxW = contentW
        val captionTopPad = 4
        val imgGapY = 12

        suspend fun loadBitmap(model: Any, targetW: Int): Bitmap? {
            val req = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)     // we need to read pixels when scaling
                .size(targetW)            // let Coil bound the width
                .build()
            val res = imageLoader.execute(req)
            val d = res.drawable ?: return null
            // convert Drawable → Bitmap
            return createBitmap(
                d.intrinsicWidth.coerceAtLeast(1),
                d.intrinsicHeight.coerceAtLeast(1)
            ).also { bmp ->
                val c = Canvas(bmp)
                d.setBounds(0, 0, bmp.width, bmp.height)
                d.draw(c)
            }
        }

        for ((model, caption) in images) {
            // Load and scale bitmap to fit width
            val src = loadBitmap(model, imgMaxW) ?: continue
            val scale = min(imgMaxW.toFloat() / src.width, 1f)
            val drawW = (src.width * scale).toInt()
            val drawH = (src.height * scale).toInt()

            // New page if not enough space
            val need =
                drawH + (if (!caption.isNullOrBlank()) (captionTopPad + bodyPaint.textSize).toInt() else 0) + imgGapY
            if (y + need > pageH - margin) {
                finishPage(page, canvas)
                val np = newPage()
                page = np.first; canvas = np.second
                y = margin
            }

            val left = margin
            val top = y
            val dest = Rect(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(src, null, dest, null)
            y += drawH

            if (!caption.isNullOrBlank()) {
                y += captionTopPad
                // wrap caption if too long
                drawMultilineText(canvas, caption, left, y, contentW, bodyPaint, lineSpacing = 4)
                y += (bodyPaint.textSize + 4).toInt()
            }
            y += imgGapY
        }

        // finish last page
        finishPage(page, canvas)

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(outDir, "inspection_${report.id}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { os -> doc.writeTo(os) }
        doc.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        left: Int,
        top: Int,
        maxWidth: Int,
        paint: Paint,
        lineSpacing: Int = 2
    ) {
        val words = text.split(" ")
        val fm = paint.fontMetrics
        var y = top.toFloat()
        var line = StringBuilder()
        for (w in words) {
            val test = if (line.isEmpty()) w else "${line} $w"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(line.toString(), left.toFloat(), y - fm.ascent, paint)
                y += paint.textSize + lineSpacing
                line = StringBuilder(w)
            } else line = StringBuilder(test)
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), left.toFloat(), y - fm.ascent, paint)
        }
    }
}