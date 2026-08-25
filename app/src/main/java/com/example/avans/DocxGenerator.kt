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
        context.assets.open("template.docx").use { inputStream ->
            val doc = XWPFDocument(inputStream)

            replaceTextPlaceholders(doc, data)

            val expensesTable = findExpensesTable(doc)
            if (expensesTable != null) {
                fillExpensesTable(expensesTable, data)
            }

            FileOutputStream(outputFile).use { out ->
                doc.write(out)
            }
            doc.close()
        }
    }

    private fun findExpensesTable(doc: XWPFDocument): XWPFTable? {
        return doc.tables.find { table ->
            val tableText = table.text
            tableText.contains("производственные") || 
            tableText.contains("Сумма расхода") || 
            tableText.contains("принятая к учету")
        } ?: doc.tables.lastOrNull()
    }

    private fun replaceTextPlaceholders(doc: XWPFDocument, data: ReportData) {
        val replacements = mapOf(
            "{{report_date}}" to data.reportDate,
            "{{purpose}}" to data.purpose
        )

        for (paragraph in doc.paragraphs) {
            replaceInParagraph(paragraph, replacements)
        }

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
        val perDiemRowIndex = 4
        val firstExpenseRowIndex = 5

        val templateRow = table.getRow(firstExpenseRowIndex)
        val cleanRowXml = templateRow?.ctRow?.xmlText()

        // 1. Заполняем суточные (Строка №1)
        val row1 = table.getRow(perDiemRowIndex)
        if (row1 != null) {
            setCellText(row1, 0, "1")
            setCellText(row1, 1, "${data.startDate}\n${data.endDate}")
            setCellText(row1, 2, "-")
            setCellText(row1, 3, "Суточные")
            setCellText(row1, 4, String.format(Locale.US, "%.2f", data.perDiemSum))
        }

        var currentTotalSum = data.perDiemSum
        val expensesCount = data.expenses.size
        val minDataRows = 5 // Гарантированный минимум отображаемых строк под чеки

        val totalRowsNeeded = maxOf(minDataRows, expensesCount)

        for (i in 0 until totalRowsNeeded) {
            val rowIndex = firstExpenseRowIndex + i
            val expense = data.expenses.getOrNull(i)

            val currentRow = if (rowIndex < table.numberOfRows - 1) {
                table.getRow(rowIndex)
            } else {
                if (cleanRowXml != null) {
                    val clonedCTRow = CTRow.Factory.parse(cleanRowXml)
                    val newRow = XWPFTableRow(clonedCTRow, table)
                    table.addRow(newRow, rowIndex)
                    newRow
                } else null
            }

            if (currentRow != null) {
                if (expense != null) {
                    setCellText(currentRow, 0, (i + 2).toString())
                    setCellText(currentRow, 1, expense.date)
                    setCellText(currentRow, 2, expense.docNumber)
                    setCellText(currentRow, 3, expense.name)
                    setCellText(currentRow, 4, String.format(Locale.US, "%.2f", expense.sum))
                    currentTotalSum += expense.sum
                } else {
                    for (c in 0 until currentRow.tableCells.size) {
                        setCellText(currentRow, c, "")
                    }
                }
            }
        }

        // 3. Заполняем строку "Итого"
        val totalRow = table.getRow(table.numberOfRows - 1)
        if (totalRow != null) {
            setCellText(totalRow, 1, String.format(Locale.US, "%.2f", currentTotalSum))
        }
    }

    private fun setCellText(row: XWPFTableRow?, cellIndex: Int, text: String) {
        if (row == null) return
        val cell = row.getCell(cellIndex) ?: return
        
        while (cell.paragraphs.size > 1) {
            cell.removeParagraph(1)
        }
        val p = cell.paragraphs.firstOrNull() ?: cell.addParagraph()
        
        // Полностью очищаем все элементы текста ячейки
        while (p.runs.size > 0) {
            p.removeRun(0)
        }

        if (text.isEmpty()) return

        if (text.contains("\n")) {
            val lines = text.split("\n")
            val run1 = p.createRun()
            run1.setText(lines[0])
            for (i in 1 until lines.size) {
                val nextParagraph = cell.addParagraph()
                val nextRun = nextParagraph.createRun()
                nextRun.setText(lines[i])
            }
        } else {
            val run = p.createRun()
            run.setText(text)
        }
    }
}
