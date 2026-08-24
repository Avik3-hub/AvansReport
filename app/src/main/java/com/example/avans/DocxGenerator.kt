package com.example.avans

import android.content.Context
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class ExpenseItem(
    val date: String,
    val docNumber: String,
    val name: String,
    val sum: Double
)

data class ReportData(
    val reportDate: String,
    val purpose: String,
    val startDate: String,
    val endDate: String,
    val perDiemSum: Double,
    val expenses: List<ExpenseItem>
)

object DocxGenerator {

    fun generateReport(context: Context, data: ReportData, outputFile: File) {
        // 1. Открываем шаблон из assets
        context.assets.open("template.docx").use { inputStream ->
            val doc = XWPFDocument(inputStream)

            // 2. Заполняем текстовые метки на 1-й странице
            replaceTextPlaceholders(doc, data)

            // 3. Заполняем таблицу на 2-й странице
            if (doc.tables.size > 1) {
                val table = doc.tables[1] // Вторая таблица (оборотная сторона)
                fillExpensesTable(table, data)
            }

            // 4. Сохраняем готовый документ
            FileOutputStream(outputFile).use { out ->
                doc.write(out)
            }
            doc.close()
        }
    }

    private fun replaceTextPlaceholders(doc: XWPFDocument, data: ReportData) {
        val replacements = mapOf(
            "{{report_date}}" to data.reportDate,
            "{{purpose}}" to data.purpose
        )

        // 1. Поиск и замена в обычных абзацах
        for (paragraph in doc.paragraphs) {
            replaceInParagraph(paragraph, replacements)
        }

        // 2. Поиск и замена во всех таблицах (включая таблицы 1-й страницы!)
        for (table in doc.tables) {
            for (row in table.rows) {
                for (cell in row.tableCells) {
                    for (paragraph in cell.paragraphs) {
                        replaceInParagraph(paragraph, replacements)
                    }
                }
            }
        }
    }

    private fun replaceInParagraph(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph, replacements: Map<String, String>) {
        for ((target, replacement) in replacements) {
            if (paragraph.text.contains(target)) {
                for (run in paragraph.runs) {
                    val text = run.getText(0)
                    if (text != null && text.contains(target)) {
                        run.setText(text.replace(target, replacement), 0)
                    }
                }
            }
        }
    }

    private fun fillExpensesTable(table: XWPFTable, data: ReportData) {
        val perDiemRowIndex = 3
        val templateRowIndex = 4

        // 1. Заполняем Строку 1 (Суточные)
        val row1 = table.getRow(perDiemRowIndex)
        setCellText(row1, 0, "1")
        setCellText(row1, 1, "${data.startDate}\n${data.endDate}")
        setCellText(row1, 2, "-")
        setCellText(row1, 3, "Суточные")
        setCellText(row1, 4, String.format(Locale.US, "%.2f", data.perDiemSum))

        // 2. Вычисляем количество обычных строк (минимум 4, чтобы всего было >= 5)
        val minRegularRows = 4
        val totalRegularRows = maxOf(minRegularRows, data.expenses.size)
        val templateRow = table.getRow(templateRowIndex)

        var currentTotalSum = data.perDiemSum

        for (i in 0 until totalRegularRows) {
            val expense = data.expenses.getOrNull(i)
            val rowNum = (i + 2).toString()

            // Клонируем эталонную строку со всеми стилями и рамками
            val currentRow = if (i == 0) {
                templateRow
            } else {
                val clonedCTRow = CTRow.Factory.parse(templateRow.ctRow.xmlText())
                val newRow = XWPFTableRow(clonedCTRow, table)
                table.addRow(newRow, templateRowIndex + i)
                newRow
            }

            if (expense != null) {
                setCellText(currentRow, 0, rowNum)
                setCellText(currentRow, 1, expense.date)
                setCellText(currentRow, 2, expense.docNumber)
                setCellText(currentRow, 3, expense.name)
                setCellText(currentRow, 4, String.format(Locale.US, "%.2f", expense.sum))
                currentTotalSum += expense.sum
            } else {
                setCellText(currentRow, 0, rowNum)
                setCellText(currentRow, 1, "")
                setCellText(currentRow, 2, "")
                setCellText(currentRow, 3, "")
                setCellText(currentRow, 4, "")
            }
        }

        // 3. Обновляем строку "Итого"
        val totalRow = table.getRow(table.numberOfRows - 1)
        val totalCellIndex = 4
        setCellText(totalRow, totalCellIndex, String.format(Locale.US, "%.2f", currentTotalSum))
    }

    private fun setCellText(row: XWPFTableRow, cellIndex: Int, text: String) {
        val cell = row.getCell(cellIndex) ?: return
        while (cell.paragraphs.size > 1) {
            cell.removeParagraph(1)
        }
        val p = cell.paragraphs[0]
        p.runs.forEach { it.setText("", 0) }
        
        val run = if (p.runs.isNotEmpty()) p.runs[0] else p.createRun()
        
        if (text.contains("\n")) {
            val lines = text.split("\n")
            run.setText(lines[0], 0)
            for (i in 1 until lines.size) {
                val nextParagraph = cell.addParagraph()
                val nextRun = nextParagraph.createRun()
                nextRun.setText(lines[i])
            }
        } else {
            run.setText(text, 0)
        }
    }
}
