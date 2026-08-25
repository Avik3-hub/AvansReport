package com.example.avans

import android.content.Context
import org.apache.poi.xwpf.usermodel.XWPFDocument
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
        // Открываем исходный шаблон из assets
        val inputStream = context.assets.open("ao1_template.docx")
        val doc = XWPFDocument(inputStream)

        // Карта замены меток в шаблоне
        val replacements = mapOf(
            "{{REPORT_DATE}}" to data.reportDate,
            "{{DEPARTMENT}}" to data.employee.department,
            "{{EMPLOYEE_NAME}}" to data.employee.name,
            "{{TAB_NUMBER}}" to data.employee.tabNumber,
            "{{POSITION}}" to data.employee.position,
            "{{PURPOSE}}" to data.purpose
        )

        // 1. Замена меток в обычных параграфах
        doc.paragraphs.forEach { replaceTextInParagraph(it, replacements) }

        // 2. Замена меток внутри таблиц (шапка, блоки УТВЕРЖДАЮ и подписи)
        doc.tables.forEach { table ->
            replaceInTable(table, replacements)
        }

        // 3. Заполнение основной таблицы расходов
        fillExpenseTable(doc, data)

        // Сохранение итогового файла
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

    private fun replaceTextInParagraph(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph, replacements: Map<String, String>) {
        val text = paragraph.paragraphText
        var updatedText = text
        var needUpdate = false

        replacements.forEach { (key, value) ->
            if (updatedText.contains(key)) {
                updatedText = updatedText.replace(key, value)
                needUpdate = true
            }
        }

        if (needUpdate) {
            // Очищаем существующие runs и записываем обновленный текст с сохранением базовых свойств
            val runsCount = paragraph.runs.size
            for (i in (runsCount - 1) downTo 0) {
                paragraph.removeRun(i)
            }
            val newRun = paragraph.createRun()
            newRun.isBold = true
            newRun.isItalic = true
            newRun.setText(updatedText)
        }
    }

    private fun fillExpenseTable(doc: XWPFDocument, data: ReportData) {
        // Находим таблицу расходов (обычно 2-я или 3-я таблица в бланке АО-1)
        val table = doc.tables.getOrNull(1) ?: return

        // Суточные (Строка 1)
        if (table.rows.size > 1) {
            val row1 = table.getRow(1)
            row1.getCell(0)?.setText("1")
            row1.getCell(1)?.setText("${data.startDate} - ${data.endDate}")
            row1.getCell(2)?.setText("-")
            row1.getCell(3)?.setText("Суточные")
            row1.getCell(4)?.setText(String.format("%.2f", data.perDiemSum))
        }

        // Чеки и билеты
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

        // Итоговая строка
        val totalRow = table.createRow()
        totalRow.getCell(3)?.setText("Итого израсходовано:")
        totalRow.getCell(4)?.setText(String.format("%.2f", totalSum))
    }
}
