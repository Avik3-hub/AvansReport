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
        // Устанавливаем альбомную или портретную ориентацию A4 со стандартными полями
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, FileOutputStream(outputFile))
        document.open()

        // Подключаем шрифт из assets для кириллицы (стандартный Times/Arial look)
        val fontPath = "assets/arialmt.ttf"
        val baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        
        val fontTitle = Font(baseFont, 12f, Font.BOLD)
        val fontBold = Font(baseFont, 9f, Font.BOLD)
        val fontRegular = Font(baseFont, 9f, Font.NORMAL)
        val fontSmall = Font(baseFont, 7f, Font.NORMAL)

        // 1. Шапка документа (Строго как в форме АО-1)
        val orgHeader = Paragraph("Ухтинский филиал ООО Авиапредприятие «Газпром авиа»", fontRegular)
        document.add(orgHeader)
        
        val docTitle = Paragraph("АВАНСОВЫЙ ОТЧЕТ № _____ от ${data.reportDate}", fontTitle)
        docTitle.alignment = Element.ALIGN_CENTER
        document.add(docTitle)
        document.add(Paragraph(" ", fontSmall))

        // Данные подотчетного лица
        document.add(Paragraph("Подотчетное лицо: ${data.employee.name} (Таб. №: ${data.employee.tabNumber})", fontRegular))
        document.add(Paragraph("Профессия (должность): ${data.employee.position}", fontRegular))
        document.add(Paragraph("Структурное подразделение: ${data.employee.department}", fontRegular))
        document.add(Paragraph("Назначение аванса: ${data.purpose}", fontRegular))
        document.add(Paragraph(" ", fontSmall))

        // 2. Таблица расходов (Оборотная сторона формы № АО-1: 5 основных колонок)[span_3](start_span)[span_3](end_span)
        val table = PdfPTable(floatArrayOf(1f, 3f, 3f, 8f, 3f))
        table.widthPercentage = 100f

        // Заголовки колонок (Строгий стиль, без заливки цвета)[span_4](start_span)[span_4](end_span)
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
                borderWidth = 0.5f // Тонкая стандартная рамка таблицы
            }
            table.addCell(cell)
        }

        // --- 1-я строка: Суточные (Две даты в графе "Дата")[span_5](start_span)[span_5](end_span) ---
        table.addCell(createCell("1", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("${data.startDate}\n${data.endDate}", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("-", fontRegular, Element.ALIGN_CENTER))
        table.addCell(createCell("Суточные", fontRegular, Element.ALIGN_LEFT))
        table.addCell(createCell(String.format("%.2f", data.perDiemSum), fontRegular, Element.ALIGN_RIGHT))

        var totalSum = data.perDiemSum

        // --- Остальные чеки и билеты[span_6](start_span)[span_6](end_span) ---
        data.expenses.forEachIndexed { index, expense ->
            val rowNum = (index + 2).toString()
            table.addCell(createCell(rowNum, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.date, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.docNumber, fontRegular, Element.ALIGN_CENTER))
            table.addCell(createCell(expense.name, fontRegular, Element.ALIGN_LEFT))
            table.addCell(createCell(String.format("%.2f", expense.sum), fontRegular, Element.ALIGN_RIGHT))
            totalSum += expense.sum
        }

        // --- Добор пустых строк до минимума (чтобы бланк смотрелся полноразмерным) ---
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

        // --- Строка Итого[span_7](start_span)[span_7](end_span) ---
        val cellTotalLabel = PdfPCell(Phrase("Итогоизрасходовано:", fontBold)).apply {
            colspan = 4
            horizontalAlignment = Element.ALIGN_RIGHT
            padding = 4f
            borderWidth = 0.5f
        }
        table.addCell(cellTotalLabel)

        val cellTotalVal = createCell(String.format("%.2f", totalSum), fontBold, Element.ALIGN_RIGHT)
        table.addCell(cellTotalVal)

        document.add(table)
        document.add(Paragraph(" ", fontRegular))

        // 3. Подписи внизу бланка[span_8](start_span)[span_8](end_span)
        val signParagraph = Paragraph("Подотчетное лицо: ____________________ / ${data.employee.name} /", fontRegular)
        signParagraph.alignment = Element.ALIGN_RIGHT
        document.add(signParagraph)

        document.close()
    }

    // Вспомогательная функция для создания ячеек без цветного фона
    private fun createCell(text: String, font: Font, align: Int): PdfPCell {
        return PdfPCell(Phrase(text, font)).apply {
            horizontalAlignment = align
            verticalAlignment = Element.ALIGN_MIDDLE
            padding = 4f
            borderWidth = 0.5f // Обычные тонкие черные границы
        }
    }
}
