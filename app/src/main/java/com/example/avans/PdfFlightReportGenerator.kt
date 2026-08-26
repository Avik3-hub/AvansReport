package com.example.avans

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfFlightReportGenerator {

    fun generatePdf(context: Context, items: List<FlightData>, outputFile: File) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        var currentY = 40f
        canvas.drawText("СВЕДЕНИЯ О ВЫПОЛНЕНИИ ПОЛЕТНЫХ ЗАДАНИЙ И ПЕРЕМЕЩЕНИЯХ", (pageWidth / 2).toFloat(), currentY, titlePaint)
        currentY += 25f

        val colWidths = floatArrayOf(30f, 85f, 85f, 120f, 120f, 95f)
        val tableWidth = colWidths.sum()
        val startX = (pageWidth - tableWidth) / 2
        val rowHeight = 22f

        val headers = arrayOf("№", "Дата убытия", "Дата прибытия", "Откуда", "Куда", "Полетное задание")

        var currentX = startX
        for (i in headers.indices) {
            val rect = Rect(currentX.toInt(), currentY.toInt(), (currentX + colWidths[i]).toInt(), (currentY + rowHeight).toInt())
            canvas.drawRect(rect, borderPaint)
            val textY = currentY + (rowHeight / 2) - ((headerPaint.descent() + headerPaint.ascent()) / 2)
            canvas.drawText(headers[i], currentX + (colWidths[i] / 2), textY, headerPaint)
            currentX += colWidths[i]
        }

        currentY += rowHeight
        val minRows = 10
        val totalRows = maxOf(minRows, items.size)

        for (rowIndex in 0 until totalRows) {
            val flight = items.getOrNull(rowIndex)
            val rowData = arrayOf(
                "${rowIndex + 1}",
                flight?.departureDate ?: "",
                flight?.arrivalDate ?: "",
                flight?.fromLocation ?: "",
                flight?.toLocation ?: "",
                flight?.flightTask ?: ""
            )

            currentX = startX
            for (colIndex in rowData.indices) {
                val rect = Rect(currentX.toInt(), currentY.toInt(), (currentX + colWidths[colIndex]).toInt(), (currentY + rowHeight).toInt())
                canvas.drawRect(rect, borderPaint)
                val textY = currentY + (rowHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
                canvas.drawText(rowData[colIndex], currentX + (colWidths[colIndex] / 2), textY, textPaint)
                currentX += colWidths[colIndex]
            }
            currentY += rowHeight
        }

        pdfDocument.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
    }
}
