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

        // 1. Замена меток в обычных параграфах
        doc.paragraphs.forEach { replaceTextInParagraph(it, replacements) }

        // 2. Замена меток во всех таблицах (шапка, ФИО, подписи)
        doc.tables.forEach { replaceInTable(it, replacements) }

        // 3. Заполнение таблицы чеков строго на оборотной стороне (стр. 2)
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
        // Ищем именно таблицу оборотной стороны (содержит заголовок "Наименование" или "Сумма расхода")
        val expenseTable = doc.tables.find { table ->
            table.rows.any { row ->
                row.tableCells.any { cell ->
                    cell.text.contains("Наименование документа", ignoreCase = true) ||
                    cell.text.contains("Сумма расхода", ignoreCase = true)
                }
            }
        } ?: return

        val allItems = mutableListOf<ExpenseItem>()
        // 1-я строка: Суточные
        allItems.add(
            ExpenseItem(
                date = "${data.startDate} - ${data.endDate}",
                docNumber = "-",
                name = "Суточные",
                sum = data.perDiemSum
            )
        )
        // Остальные строки: чеки и билеты
        allItems.addAll(data.expenses)

        // Индекс строк с данными начинается после шапки таблицы (обычно со строки №3)
        var currentRowIndex = 3
        var totalSum = 0.0

        allItems.forEachIndexed { index, item ->
            totalSum += item.sum

            val row = if (currentRowIndex < expenseTable.rows.size) {
                val existingRow = expenseTable.getRow(currentRowIndex)
                if (existingRow.tableCells.any { it.text.contains("Итого", ignoreCase = true) }) {
                    expenseTable.insertNewTableRow(currentRowIndex)
                } else {
                    existingRow
                }
            } else {
                expenseTable.createRow()
            }

            setCellText(row.getCell(0), "${index + 1}")
            setCellText(row.getCell(1), item.date)
            setCellText(row.getCell(2), item.docNumber)
            setCellText(row.getCell(3), item.name)
            setCellText(row.getCell(4), String.format("%.2f", item.sum))

            currentRowIndex++
        }

        // Заполняем итоговую сумму в строке "Итого"
        val totalRow = expenseTable.rows.find { row ->
            row.tableCells.any { it.text.contains("Итого", ignoreCase = true) }
        }
        if (totalRow != null) {
            setCellText(totalRow.getCell(4), String.format("%.2f", totalSum))
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
