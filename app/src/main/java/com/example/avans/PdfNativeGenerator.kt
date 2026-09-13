package com.example.avans

import android.content.Context
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfNativeGenerator {

    fun generateFlightPdf(context: Context, data: ReportData, outputFile: File) {
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, FileOutputStream(outputFile))
        document.open()

        // Безопасное получение пути к шрифту arialmt.ttf из assets
        val fontPath = getFontPath(context)
        val baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)

        val fontTitle = Font(baseFont, 12f, Font.BOLD)
        val fontBold = Font(baseFont, 9f, Font.BOLD)
        val fontRegular = Font(baseFont, 9f, Font.NORMAL)
        val fontSmall = Font(baseFont, 7f, Font.NORMAL)

        // 1. Шапка документа
        val orgHeader = Paragraph("Ухтинский филиал ООО Авиапредприятие «Газпром авиа»", fontRegular)
        document.add(orgHeader)

        val docTitle = Paragraph("АВАНСОВЫЙ ОТЧЕТ № _____ от ${data.reportDate}", fontTitle).apply {
            alignment = Element.ALIGN_CENTER
        }
        document.add(docTitle)
        document.add(Paragraph(" ", fontSmall))

        // Данные подотчетного лица
        document.add(Paragraph("Подотчетное лицо: ${data.employee.name} (Таб. №: ${data.employee.tabNumber})", fontRegular))
        document.add(Paragraph("Профессия (должность): ${data.employee.position}", fontRegular))
        document.add(Paragraph("Структурное подразделение: ${data.employee.department}", fontRegular))
        document.add(Paragraph("Назначение аванса: ${data.purpose}", fontRegular))
        document.add(Paragraph(" ", fontSmall))

        // 2. Таблица расходов
        val table = PdfPTable(floatArrayOf(1f, 2.5f, 2.5f, 8f, 3f)).apply {
            widthPercentage = 100f
        }

        val headers = arrayOf(
            "№\nпп",
            "Дата документа",
            "Номер документа",
            "Наименование документа (расхода)",
            "Сумма расхода\nпо отчету (руб.)"
        )

        for (headerText in headers) {
            val cell = PdfPCell(Phrase(headerText, fontBold)).apply {
                horizontalAlignment = Element.ALIGN_CENTER
                verticalAlignment = Element.ALIGN_MIDDLE
                padding = 4f
            }
            table.addCell(cell)
        }

        // --- 1-я строка: Суточные ---
        val datesText = if (data.startDate == data.endDate) data.startDate else "${data.startDate}\n${data.endDate}"
        table.addCell(createCell("1", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell(datesText, fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("-", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("Суточные", fontRegular, Element.ALIGN_LEFT))
        table.addCell(createCell(formatMoney(data.perDiemSum), fontRegular, Element.ALIGN_RIGHT))

        var totalSum = data.perDiemSum

        // --- Чеки и билеты ---
        data.expenses.forEachIndexed { index, expense ->
            val rowNum = (index + 2).toString()
            table.addCell(createCell(rowNum, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.date, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.docNumber, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.name, fontRegular, Element.ALIGN_LEFT))
            table.addCell(createCell(formatMoney(expense.sum), fontRegular, Element.ALIGN_RIGHT))
            totalSum += expense.sum
        }

        // --- Добор пустых строк до 6 ---
        val currentRows = data.expenses.size + 1
        val minRows = 6
        if (currentRows < minRows) {
            for (i in (currentRows + 1)..minRows) {
                table.addCell(createCell(i.toString(), fontRegular, Element.ALIGN_CENTER))
                table.addCell(createCell("", fontRegular, Element.ALIGN_CENTER))
                table.addCell(createCell("", fontRegular, Element.ALIGN_CENTER))
                table.addCell(createCell("", fontRegular, Element.ALIGN_LEFT))
                table.addCell(createCell("", fontRegular, Element.ALIGN_RIGHT))
            }
        }

        // --- Строка Итого ---
        val cellTotalLabel = PdfPCell(Phrase("Итого израсходовано:", fontBold)).apply {
            colspan = 4
            horizontalAlignment = Element.ALIGN_RIGHT
            padding = 4f
        }
        table.addCell(cellTotalLabel)

        val cellTotalVal = createCell(formatMoney(totalSum), fontBold, Element.ALIGN_RIGHT)
        table.addCell(cellTotalVal)

        document.add(table)
        document.add(Paragraph(" ", fontRegular))

        // 3. Подпись
        val signParagraph = Paragraph("Подотчетное лицо: ____________________ / ${data.employee.name} /", fontRegular).apply {
            alignment = Element.ALIGN_RIGHT
        }
        document.add(signParagraph)

        document.close()
    }

    private fun createCell(text: String, font: Font, align: Int): PdfPCell {
        return PdfPCell(Phrase(text, font)).apply {
            horizontalAlignment = align
            verticalAlignment = Element.ALIGN_MIDDLE
            padding = 4f
        }
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }

    // Копирование arialmt.ttf из assets во внутренний кэш
    private fun getFontPath(context: Context): String {
        val fontFile = File(context.cacheDir, "arialmt.ttf")
        if (!fontFile.exists() || fontFile.length() == 0L) {
            context.assets.open("arialmt.ttf").use { input ->
                FileOutputStream(fontFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return fontFile.absolutePath
    }
}
