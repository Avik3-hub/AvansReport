package com.example.avans

import android.content.Context
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
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

        // Словарь замен с поддержкой верхнего и нижнего регистра меток
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

        // 1. Замена меток в абзацах
        doc.paragraphs.forEach { replaceTextInParagraph(it, replacements) }

        // 2. Замена меток во всех таблицах
        doc.tables.forEach { replaceInTable(it, replacements) }

        // 3. Заполнение таблицы расходов
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
        val table = doc.tables.getOrNull(0) ?: return

        // Заполнение первой строки (суточные)
        if (table.rows.size > 1) {
            val row1 = table.getRow(1)
            row1.getCell(0)?.setText("1")
            row1.getCell(1)?.setText("${data.startDate} - ${data.endDate}")
            row1.getCell(2)?.setText("-")
            row1.getCell(3)?.setText("Суточные")
            row1.getCell(4)?.setText(String.format("%.2f", data.perDiemSum))
        }

        // Заполнение чеков
        var totalSum = data.perDiemSum
        data.expenses.forEachIndexed { index, item ->
            val row = table.createRow()
            row.getCell(0)?.setText("${index + 2}")
            row.getCell(1)?.setText(item.date)
            row.getCell(2)?.setText(item.docNumber)
            row.getCell(3)?.setText(item.name)
            row.getCell(4)?.setText(String.format("%.2f", item.sum))
            totalSum += item.sum
        }

        val totalRow = table.createRow()
        totalRow.getCell(3)?.setText("Итого израсходовано:")
        totalRow.getCell(4)?.setText(String.format("%.2f", totalSum))
    }
}
