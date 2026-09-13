package com.example.avans

import android.content.Context
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream

object PdfNativeGenerator {

    fun generateFlightPdf(context: Context, data: ReportData, outputFile: File) {
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, FileOutputStream(outputFile))
        document.open()

        val fontPath = "assets/arial.ttf"
        val baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        
        val fontTitle = Font(baseFont, 12f, Font.BOLD)
        val fontBold = Font(baseFont, 9f, Font.BOLD)
        val fontRegular = Font(baseFont, 9f, Font.NORMAL)
        val fontSmall = Font(baseFont, 7f, Font.NORMAL)

        // 1. Шапка документа (Строго по АО-1)
        val orgHeader = Paragraph("Ухтинский филиал ООО Авиапредприятие «Газпром авиа»", fontRegular)
        document.add(orgHeader)
        
        val docTitle = Paragraph("АВАНСОВЫЙ ОТЧЕТ № _____ от ${data.reportDate}", fontTitle)
        docTitle.setAlignment(Element.ALIGN_CENTER)
        document.add(docTitle)
        document.add(Paragraph(" ", fontSmall))

        // Данные подотчетного лица
        document.add(Paragraph("Подотчетное лицо: ${data.employee.name} (Таб. №: ${data.employee.tabNumber})", fontRegular))
        document.add(Paragraph("Профессия (должность): ${data.employee.position}", fontRegular))
        document.add(Paragraph("Структурное подразделение: ${data.employee.department}", fontRegular))
        document.add(Paragraph("Назначение аванса: ${data.purpose}", fontRegular))
        document.add(Paragraph(" ", fontSmall))

        // 2. Таблица расходов
        val table = PdfPTable(floatArrayOf(1f, 3f, 3f, 8f, 3f))
        table.widthPercentage = 100f

        val headers = arrayOf(
            "№\nпп", 
            "Дата документа", 
            "Номер документа", 
            "Наименование документа (расхода)", 
            "Сумма расхода\nпо отчету (руб.)"
        )

        for (headerText in headers) {
            val cell = PdfPCell(Phrase(headerText, fontBold)).apply {
                setHorizontalAlignment(Element.ALIGN_CENTER)
                setVerticalAlignment(Element.ALIGN_MIDDLE)
                setPadding(4f)
                setBorderWidth(0.5f)
            }
            table.addCell(cell)
        }

        // --- 1-я строка: Суточные ---
        table.addCell(createCell("1", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("${data.startDate}\n${data.endDate}", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("-", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("Суточные", fontRegular, Element.ALIGN_LEFT))
        table.addCell(createCell(String.format("%.2f", data.perDiemSum), fontRegular, Element.ALIGN_RIGHT))

        var totalSum = data.perDiemSum

        // --- Чеки и билеты ---
        data.expenses.forEachIndexed { index, expense ->
            val rowNum = (index + 2).toString()
            table.addCell(createCell(rowNum, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.date, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.docNumber, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.name, fontRegular, Element.ALIGN_LEFT))
            table.addCell(createCell(String.format("%.2f", expense.sum), fontRegular, Element.ALIGN_RIGHT))
            totalSum += expense.sum
        }

        // --- Пустые строки до минимума (5) ---
        val currentRows = data.expenses.size + 1
        if (currentRows < 5) {
            for (i in (currentRows + 1)..5) {
                table.addCell(createCell(i.toString(), fontRegular, Element.ALIGN_CENTER))
                table.addCell(createCell("", fontRegular, Element.ALIGN_CENTER))
                table.addCell(createCell("", fontRegular, Element.ALIGN_CENTER))
                table.addCell(createCell("", fontRegular, Element.ALIGN_LEFT))
                table.addCell(createCell("", fontRegular, Element.ALIGN_RIGHT))
            }
        }

        // --- Строка Итого ---
        val cellTotalLabel = PdfPCell(Phrase("Итого израсходовано:", fontBold)).apply {
            setColspan(4)
            setHorizontalAlignment(Element.ALIGN_RIGHT)
            setPadding(4f)
            setBorderWidth(0.5f)
        }
        table.addCell(cellTotalLabel)

        val cellTotalVal = createCell(String.format("%.2f", totalSum), fontBold, Element.ALIGN_RIGHT)
        table.addCell(cellTotalVal)

        document.add(table)
        document.add(Paragraph(" ", fontRegular))

        // 3. Подписи
        val signParagraph = Paragraph("Подотчетное лицо: ____________________ / ${data.employee.name} /", fontRegular)
        signParagraph.setAlignment(Element.ALIGN_RIGHT)
        document.add(signParagraph)

        document.close()
    }

    private fun createCell(text: String, font: Font, align: Int): PdfPCell {
        return PdfPCell(Phrase(text, font)).apply {
            setHorizontalAlignment(align)
            setVerticalAlignment(Element.ALIGN_MIDDLE)
            setPadding(4f)
            setBorderWidth(0.5f)
        }
    }
}
