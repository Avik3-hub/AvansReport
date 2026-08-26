package com.example.avans

import android.content.Context
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import java.io.File
import java.io.FileOutputStream

data class ExpenseItem(
    val date: String,
    val docNumber: String,
    val name: String,
    val sum: Double
)

data class EmployeeInfo(
    val name: String,
    val tabNumber: String,
    val position: String,
    val department: String
)

data class ReportData(
    val reportDate: String,
    val purpose: String,
    val startDate: String,
    val endDate: String,
    val perDiemSum: Double,
    val expenses: List<ExpenseItem>,
    val employee: EmployeeInfo
)

object DocxGenerator {

    fun generateReport(context: Context, data: ReportData, outputFile: File) {
        val inputStream = try {
            context.assets.open("template.docx")
        } catch (e: Exception) {
            throw Exception("Файл template.docx не найден в assets!")
        }

        val doc = XWPFDocument(inputStream)

        val replacements = mapOf(
            "{{REPORT_DATE}}" to data.reportDate,
            "{{report_date}}" to data.reportDate,
            "{{DEPARTMENT}}" to data.employee.department,
            "{{department}}" to data.employee.department,
            "{{EMPLOYEE_NAME}}" to data.employee.name,
            "{{employee_name}}" to data.employee.name,
            "{{TAB_NUMBER}}" to data.employee.tabNumber,
            "{{tab_number}}" to data.employee.tabNumber,
            "{{POSITION}}" to data.employee.position,
            "{{position}}" to data.employee.position,
            "{{PURPOSE}}" to data.purpose,
            "{{purpose}}" to data.purpose
        )

        // 1. Замена меток в параграфах
        doc.paragraphs.forEach { replaceTextInParagraph(it, replacements) }

        // 2. Замена меток в таблицах (шапка и подписи)
        doc.tables.forEach { replaceInTable(it, replacements) }

        // 3. Заполнение оборотной стороны
        fillExpenseTable(doc, data)

        FileOutputStream(outputFile).use { out ->
            doc.write(out)
        }
        doc.close()
        inputStream.close()
    }

    private fun replaceInTable(table: XWPFTable, replacements: Map<String, String>) {
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                cell.paragraphs.forEach { replaceTextInParagraph(it, replacements) }
                cell.tables.forEach { replaceInTable(it, replacements) }
            }
        }
    }

    private fun replaceTextInParagraph(paragraph: XWPFParagraph, replacements: Map<String, String>) {
        var text = paragraph.paragraphText
        var updated = false

        replacements.forEach { (key, value) ->
            if (text.contains(key)) {
                text = text.replace(key, value)
                updated = true
            }
        }

        if (updated) {
            for (i in paragraph.runs.size - 1 downTo 0) {
                paragraph.removeRun(i)
            }
            val newRun = paragraph.createRun()
            newRun.setText(text)
        }
    }

    private fun fillExpenseTable(doc: XWPFDocument, data: ReportData) {
        val expenseTable = doc.tables.find { table ->
            table.rows.any { row ->
                row.tableCells.any { cell ->
                    cell.text.contains("Наименование документа", ignoreCase = true) ||
                    cell.text.contains("Сумма расхода", ignoreCase = true)
                }
            }
        } ?: return

        // Поиск строки нумерации (содержит 6, 7, 8, 9)
        val numberingRowIndex = expenseTable.rows.indexOfFirst { row ->
            val texts = row.tableCells.map { it.text.trim() }
            texts.contains("6") && texts.contains("7") && texts.contains("8") && texts.contains("9")
        }

        // Данные начинаются строго ПОСЛЕ строки нумерации
        val startDataRowIndex = if (numberingRowIndex != -1) numberingRowIndex + 1 else 3

        val allItems = mutableListOf<ExpenseItem>()
        allItems.add(
            ExpenseItem(
                date = "${data.startDate} - ${data.endDate}",
                docNumber = "-",
                name = "Суточные",
                sum = data.perDiemSum
            )
        )
        allItems.addAll(data.expenses)

        // Гарантируем минимум 5 строк в таблице
        val minRows = 5
        val totalRowsToDisplay = maxOf(minRows, allItems.size)

        var totalSum = 0.0

        for (i in 0 until totalRowsToDisplay) {
            val targetRowIndex = startDataRowIndex + i

            // Проверяем, не уперлись ли в строку "Итого"
            val currentRow = expenseTable.rows.getOrNull(targetRowIndex)
            val isTotalRow = currentRow?.tableCells?.any { it.text.contains("Итого", ignoreCase = true) } == true

            val row = if (isTotalRow || targetRowIndex >= expenseTable.rows.size) {
                expenseTable.insertNewTableRow(targetRowIndex)
            } else {
                expenseTable.getRow(targetRowIndex)
            }

            while (row.tableCells.size < 9) {
                row.addNewTableCell()
            }

            if (i < allItems.size) {
                val item = allItems[i]
                totalSum += item.sum
                setCellText(row.getCell(0), "${i + 1}")
                setCellText(row.getCell(1), item.date)
                setCellText(row.getCell(2), item.docNumber)
                setCellText(row.getCell(3), item.name)
                setCellText(row.getCell(4), String.format("%.2f", item.sum))
            } else {
                // Заполнение пустых строк до 5 штук
                setCellText(row.getCell(0), "${i + 1}")
                setCellText(row.getCell(1), "")
                setCellText(row.getCell(2), "")
                setCellText(row.getCell(3), "")
                setCellText(row.getCell(4), "")
            }

            // Очистка колонок валюты/дебета (5..8)
            for (c in 5..8) {
                setCellText(row.getCell(c), "")
            }
        }

        // Выравнивание суммы "Итого"
        val totalRowIndex = expenseTable.rows.indexOfFirst { row ->
            row.tableCells.any { it.text.contains("Итого", ignoreCase = true) }
        }

        if (totalRowIndex != -1) {
            val totalRow = expenseTable.getRow(totalRowIndex)
            val sumStr = String.format("%.2f", totalSum)

            if (totalRow.tableCells.size < 9) {
                // Для объединенной строки "Итого": getCell(1) — это графа "в руб. коп."
                setCellText(totalRow.getCell(1), sumStr)
            } else {
                setCellText(totalRow.getCell(4), sumStr)
            }
        }
    }

    private fun setCellText(cell: XWPFTableCell?, text: String) {
        if (cell == null) return
        if (cell.paragraphs.isNotEmpty()) {
            val p = cell.paragraphs[0]
            for (i in p.runs.size - 1 downTo 0) {
                p.removeRun(i)
            }
            val run = p.createRun()
            run.fontSize = 8
            run.setText(text)
        } else {
            cell.setText(text)
        }
    }
}
