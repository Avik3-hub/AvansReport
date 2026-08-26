package com.example.avans

import android.content.Context
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
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

        // Поиск строки нумерации (1 2 3 4 5 6 7 8 9)
        val numberingRowIndex = expenseTable.rows.indexOfFirst { row ->
            val texts = row.tableCells.map { it.text.trim() }
            texts.contains("6") && texts.contains("7") && texts.contains("8") && texts.contains("9")
        }

        val startDataRowIndex = if (numberingRowIndex != -1) numberingRowIndex + 1 else 3

        // Эталонная высота строки (берётся из первой строки данных шаблона или задается в ~380 twips)
        val sampleRow = expenseTable.rows.getOrNull(startDataRowIndex)
        val sampleHeight = if (sampleRow != null && sampleRow.height > 0) sampleRow.height else 380

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

        // Гарантируем минимум 5 строк
        val minRows = 5
        val totalRowsToDisplay = maxOf(minRows, allItems.size)

        var totalSum = 0.0

        for (i in 0 until totalRowsToDisplay) {
            val targetRowIndex = startDataRowIndex + i

            val currentRow = expenseTable.rows.getOrNull(targetRowIndex)
            val isTotalRow = currentRow?.tableCells?.any { it.text.contains("Итого", ignoreCase = true) } == true

            val row = if (isTotalRow || targetRowIndex >= expenseTable.rows.size) {
                expenseTable.insertNewTableRow(targetRowIndex)
            } else {
                expenseTable.getRow(targetRowIndex)
            }

            // Устанавливаем единую высоту строки
            row.height = sampleHeight

            while (row.tableCells.size < 9) {
                row.addNewTableCell()
            }

            if (i < allItems.size) {
                val item = allItems[i]
                totalSum += item.sum
                setCellText(row.getCell(0), "${i + 1}", ParagraphAlignment.CENTER)
                setCellText(row.getCell(1), item.date, ParagraphAlignment.CENTER)
                setCellText(row.getCell(2), item.docNumber, ParagraphAlignment.CENTER)
                setCellText(row.getCell(3), item.name, ParagraphAlignment.LEFT)
                setCellText(row.getCell(4), String.format("%.2f", item.sum), ParagraphAlignment.RIGHT)
            } else {
                // Пустые строки для выравнивания бланка
                setCellText(row.getCell(0), "${i + 1}", ParagraphAlignment.CENTER)
                setCellText(row.getCell(1), "", ParagraphAlignment.CENTER)
                setCellText(row.getCell(2), "", ParagraphAlignment.CENTER)
                setCellText(row.getCell(3), "", ParagraphAlignment.LEFT)
                setCellText(row.getCell(4), "", ParagraphAlignment.RIGHT)
            }

            // Очистка и центрирование неиспользуемых колонок валюты/дебета (5..8)
            for (c in 5..8) {
                setCellText(row.getCell(c), "", ParagraphAlignment.CENTER)
            }
        }

        // Заполнение строки "Итого"
        val totalRowIndex = expenseTable.rows.indexOfFirst { row ->
            row.tableCells.any { it.text.contains("Итого", ignoreCase = true) }
        }

        if (totalRowIndex != -1) {
            val totalRow = expenseTable.getRow(totalRowIndex)
            totalRow.height = sampleHeight
            val sumStr = String.format("%.2f", totalSum)

            if (totalRow.tableCells.size < 9) {
                setCellText(totalRow.getCell(1), sumStr, ParagraphAlignment.RIGHT, isBold = true)
            } else {
                setCellText(totalRow.getCell(4), sumStr, ParagraphAlignment.RIGHT, isBold = true)
            }
        }
    }

    private fun setCellText(
        cell: XWPFTableCell?,
        text: String,
        alignment: ParagraphAlignment = ParagraphAlignment.CENTER,
        isBold: Boolean = false
    ) {
        if (cell == null) return

        // Выравнивание по вертикали (строго по центру ячейки)
        cell.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER

        val p = if (cell.paragraphs.isNotEmpty()) cell.paragraphs[0] else cell.addParagraph()

        for (i in p.runs.size - 1 downTo 0) {
            p.removeRun(i)
        }

        // Выравнивание по горизонтали и обнуление лишних отступов
        p.alignment = alignment
        p.spacingBefore = 0
        p.spacingAfter = 0

        val run = p.createRun()
        run.fontSize = 8
        run.isBold = isBold
        run.setText(text)
    }
}
