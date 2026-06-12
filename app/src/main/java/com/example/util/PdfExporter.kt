package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CollectionLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndShareEmiReport(
        context: Context,
        collectionHistory: List<CollectionLog>,
        totalOutlay: Double,
        totalRecovered: Double,
        outstandingActive: Double,
        formatPkr: (Double) -> String,
        formatDate: (Long) -> String
    ) {
        val pdfDocument = PdfDocument()

        // Page info for A4 size: 595 x 842 points
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }

        var yPosition = 50f

        // Draw header helper
        fun drawPageHeader(canvas: Canvas, pageNum: Int) {
            paint.style = Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(0, 100, 80) // Emerald tone
            paint.strokeWidth = 3f
            
            // Header Top Bar Accent
            paint.style = Paint.Style.FILL
            canvas.drawRect(30f, 30f, 565f, 40f, paint)

            // Header Title
            textPaint.textSize = 18f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = android.graphics.Color.rgb(0, 80, 60)
            canvas.drawText("EMI PAYMENT & AUDIT RECOVERY REPORT", 30f, 65f, textPaint)

            // Generated date info & Page number
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = android.graphics.Color.GRAY
            val simpleDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateStr = simpleDateFormat.format(Date())
            canvas.drawText("Generated on: $dateStr", 30f, 82f, textPaint)
            canvas.drawText("Page: $pageNum", 500f, 82f, textPaint)

            // Horizontal rules
            paint.color = android.graphics.Color.LTGRAY
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(30f, 95f, 565f, 95f, paint)
        }

        // Draw page headers on first page
        drawPageHeader(canvas, pageNumber)
        yPosition = 120f

        // --- SECTION 1: CAPITAL OUTSTANDING SUMMARY ---
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(240, 245, 242) // clean off-white card bg
        canvas.drawRoundRect(30f, yPosition, 565f, yPosition + 120f, 8f, 8f, paint)

        // Title of block
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.rgb(0, 100, 80)
        canvas.drawText("CAPITAL OUTSTANDING SUMMARY", 45f, yPosition + 25f, textPaint)

        // Draw dividing line
        paint.style = Paint.Style.STROKE
        paint.color = android.graphics.Color.rgb(215, 225, 220)
        paint.strokeWidth = 1f
        canvas.drawLine(45f, yPosition + 35f, 550f, yPosition + 35f, paint)

        // Info entries
        textPaint.textSize = 10f
        textPaint.color = android.graphics.Color.DKGRAY

        // Rows inside summary block
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Total Outlay Portfolio:", 45f, yPosition + 55f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.BLACK
        canvas.drawText(formatPkr(totalOutlay), 250f, yPosition + 55f, textPaint)

        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Total Volume Recovered (Received):", 45f, yPosition + 73f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.rgb(0, 120, 70) // green label for recoveries
        canvas.drawText(formatPkr(totalRecovered), 250f, yPosition + 73f, textPaint)

        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Outstanding Active Credit (Remaining):", 45f, yPosition + 91f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.rgb(180, 50, 50) // Red/orange alert color for remaining balance
        canvas.drawText(formatPkr(outstandingActive), 250f, yPosition + 91f, textPaint)

        // Recovery ratio/efficiency
        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Collection Efficiency Ratio:", 45f, yPosition + 107f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.BLACK
        val collectionEfficiency = if (totalOutlay > 0) (totalRecovered / totalOutlay * 100) else 0.0
        val ratioStr = String.format(Locale.getDefault(), "%.1f", collectionEfficiency) + "%"
        canvas.drawText(ratioStr, 250f, yPosition + 107f, textPaint)

        yPosition += 150f

        // --- SECTION 2: RECENT TRANSACTIONS LOG TABLE ---
        textPaint.textSize = 12f
        textPaint.color = android.graphics.Color.rgb(0, 80, 60)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECENT TRANSACTIONS LOG", 30f, yPosition, textPaint)

        yPosition += 10f

        // Table Header draw
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.rgb(0, 80, 60) // dark emerald background
        canvas.drawRect(30f, yPosition, 565f, yPosition + 25f, paint)

        textPaint.textSize = 9f
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        // Headers text
        canvas.drawText("Date", 35f, yPosition + 16f, textPaint)
        canvas.drawText("Customer Name", 115f, yPosition + 16f, textPaint)
        canvas.drawText("Item / Cycle", 245f, yPosition + 16f, textPaint)
        canvas.drawText("Payment Method", 375f, yPosition + 16f, textPaint)
        canvas.drawText("Amount (PKR)", 475f, yPosition + 16f, textPaint)

        yPosition += 25f

        // Draw Table items with multi-page support
        for (item in collectionHistory) {
            // Check if we need to start a new page
            // Keep some margins at the bottom: 842 - 50 = 792
            if (yPosition > 780f) {
                // Draw bottom footer for current page
                textPaint.color = android.graphics.Color.LTGRAY
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("Confidential - For Internal Use Only", 30f, 810f, textPaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Re-draw header on new page
                drawPageHeader(canvas, pageNumber)

                // Draw Table Header again
                yPosition = 120f
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(0, 80, 60)
                canvas.drawRect(30f, yPosition, 565f, yPosition + 25f, paint)

                textPaint.textSize = 9f
                textPaint.color = android.graphics.Color.WHITE
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Date", 35f, yPosition + 16f, textPaint)
                canvas.drawText("Customer Name", 115f, yPosition + 16f, textPaint)
                canvas.drawText("Item / Cycle", 245f, yPosition + 16f, textPaint)
                canvas.drawText("Payment Method", 375f, yPosition + 16f, textPaint)
                canvas.drawText("Amount (PKR)", 475f, yPosition + 16f, textPaint)

                yPosition += 25f
            }

            // Draw alternating background row shading
            paint.style = Paint.Style.FILL
            if (collectionHistory.indexOf(item) % 2 == 0) {
                paint.color = android.graphics.Color.rgb(250, 252, 250)
            } else {
                paint.color = android.graphics.Color.rgb(238, 244, 241)
            }
            canvas.drawRect(30f, yPosition, 565f, yPosition + 26f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(220, 230, 225)
            paint.strokeWidth = 0.5f
            canvas.drawRect(30f, yPosition, 565f, yPosition + 26f, paint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8.5f

            // Data values
            canvas.drawText(formatDate(item.receiptDate), 35f, yPosition + 17f, textPaint)
            
            // Handle ellipsis for names that are too long to fit
            var nameStr = item.customerName
            if (nameStr.length > 20) {
                nameStr = nameStr.substring(0, 18) + "..."
            }
            canvas.drawText(nameStr, 115f, yPosition + 17f, textPaint)

            var descStr = "${item.itemName} (Cycle #${item.installmentNumber})"
            if (descStr.length > 22) {
                descStr = descStr.substring(0, 20) + "..."
            }
            canvas.drawText(descStr, 245f, yPosition + 17f, textPaint)
            
            canvas.drawText(item.paymentMethod, 375f, yPosition + 17f, textPaint)
            
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatPkr(item.amountReceived), 475f, yPosition + 17f, textPaint)

            yPosition += 26f
        }

        // Draw final page bottom footer
        textPaint.color = android.graphics.Color.LTGRAY
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Confidential - For Internal Use Only - End of Report", 30f, 810f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache directory
        val fileName = "EMI_Payment_Report_${System.currentTimeMillis()}.pdf"
        val outputFile = File(context.cacheDir, fileName)

        try {
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(context, "PDF Report exported successfully!", Toast.LENGTH_SHORT).show()
            sharePdfFile(context, outputFile)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to write PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun sharePdfFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        try {
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "EMI Payment & Recovery Audit Report")
                putExtra(Intent.EXTRA_TEXT, "Kindly review the attached PDF copy of the generated EMI Payment and Quality Recovery Report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Report via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
