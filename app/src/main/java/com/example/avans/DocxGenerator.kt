package com.example.avans

import android.content.Context
import org.apache.poi.xwpf.usermodel.*
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
        val doc = XWPFDocument()

        // 1. Шапка документа (справа)
        val pHeader = doc.createParagraph()
        pHeader.alignment = ParagraphAlignment.RIGHT
        val rHeader = pHeader.createRun()
        rHeader.fontSize = 9
        rHeader.setText("Унифицированная форма № АО-1\nУтверждена постановлением Госкомстата России\nот 01.08.2001 № 55")

        // 2. Блок УТВЕРЖДАЮ и ОКУД/ОКПО
        val tableTop = doc.createTable(1, 2)
        tableTop.width = 10000
        
        val cellLeft = tableTop.getRow(0).getCell(0)
        val pUtv = cellLeft.paragraphs[0]
        pUtv.alignment = ParagraphAlignment.CENTER
        var r = pUtv.createRun()
        r.isBold = true
        r.setText("УТВЕРЖДАЮ")
        pUtv.createRun().addBreak()

        val pSum = cellLeft.createParagraph()
        r = pSum.createRun()
        r.setText("Отчет в сумме ______________________________________")
        pSum.createRun().addBreak()
        
        r = pSum.createRun()
        r.setText("______________________ руб. _____ коп.")
        pSum.createRun().addBreak()

        val pRuk = cellLeft.createParagraph()
        r = pRuk.createRun()
        r.setText("Руководитель _____________________________________")
        pRuk.createRun().addBreak()
        
        r = pRuk.createRun()
        r.fontSize = 8
        r.setText("                                         должность")
        pRuk.createRun().addBreak()

        val pSig = cellLeft.createParagraph()
        r = pSig.createRun()
        r.setText("_________________   ________________________")
        pSig.createRun().addBreak()
        r = pSig.createRun()
        r.fontSize = 8
        r.setText("         подпись                          расшифровка")

        val cellRight = tableTop.getRow(0).getCell(1)
        val pOkud = cellRight.paragraphs[0]
        pOkud.alignment = ParagraphAlignment.RIGHT
        r = pOkud.createRun()
        r.setText("Форма по ОКУД 0302001\nпо ОКПО 01")

        // 3. Заголовок
        val pTitle = doc.createParagraph()
        pTitle.alignment = ParagraphAlignment.CENTER
        r = pTitle.createRun()
        r.isBold = true
        r.fontSize = 14
        r.setText("АВАНСОВЫЙ ОТЧЕТ № _____ от ${data.reportDate} г.")

        // 4. Позиции и подотчетное лицо (С динамическими данными сотрудника в жирном курсиве)
        val pInfo1 = doc.createParagraph()
        pInfo1.createRun().setText("Структурное подразделение  ")
        addValueRun(pInfo1, data.employee.department)

        val pInfo2 = doc.createParagraph()
        pInfo2.createRun().setText("Подотчетное лицо  ")
        addValueRun(pInfo2, data.employee.name)
        pInfo2.createRun().setText("   Табельный номер  ")
        addValueRun(pInfo2, data.employee.tabNumber)

        val pInfo3 = doc.createParagraph()
        pInfo3.createRun().setText("Профессия (должность)  ")
        addValueRun(pInfo3, data.employee.position)
        pInfo3.createRun().setText("   Назначение аванса  ")
        addValueRun(pInfo3, data.purpose)

        // 5. Таблица расходов (1 страница)
        val table = doc.createTable()
        val headerRow = table.getRow(0)
        
        val headers = listOf("№", "Дата", "№ документа", "Наименование расхода", "Сумма (руб.)")
        for (i in headers.indices) {
            val cell = if (i == 0) headerRow.getCell(0) else headerRow.addNewTableCell()
            val p = cell.paragraphs[0]
            p.alignment = ParagraphAlignment.CENTER
            val run = p.createRun()
            run.isBold = true
            run.setText(headers[i])
        }

        // Ввод суточных (Строка 1)
        val row1 = table.createRow()
        row1.getCell(0).setText("1")
        row1.getCell(1).setText("${data.startDate} - ${data.endDate}")
        row1.getCell(2).setText("-")
        row1.getCell(3).setText("Суточные")
        row1.getCell(4).setText(String.format("%.2f", data.perDiemSum))

        // Ввод чеков и билетов (Строки 2+)
        var totalSum = data.perDiemSum
        data.expenses.forEachIndexed { index, item ->
            val row = table.createRow()
            row.getCell(0).setText("${index + 2}")
            row.getCell(1).setText(item.date)
            row.getCell(2).setText(item.docNumber)
            row.getCell(3).setText(item.name)
            row.getCell(4).setText(String.format("%.2f", item.sum))
            totalSum += item.sum
        }

        // Итоговая строка
        val totalRow = table.createRow()
        totalRow.getCell(0).setText("")
        totalRow.getCell(1).setText("")
        totalRow.getCell(2).setText("")
        val pTotalLabel = totalRow.getCell(3).paragraphs[0].createRun()
        pTotalLabel.isBold = true
        pTotalLabel.setText("Итого израсходовано:")
        
        val pTotalVal = totalRow.getCell(4).paragraphs[0].createRun()
        pTotalVal.isBold = true
        pTotalVal.setText(String.format("%.2f", totalSum))

        // 6. Подписи на 2-й странице (с подстановкой ФИО подотчетного лица)
        val pPageBreak = doc.createParagraph()
        pPageBreak.isPageBreak = true

        val pBottomSig = doc.createParagraph()
        pBottomSig.createRun().setText("Подотчетное лицо  ")
        val rSigName = pBottomSig.createRun()
        rSigName.isBold = true
        rSigName.isItalic = true
        rSigName.setText(data.employee.name)
        
        pBottomSig.createRun().setText("   _______________   (подпись)")

        // Сохранение файла
        FileOutputStream(outputFile).use { out ->
            doc.write(out)
        }
        doc.close()
    }

    private fun addValueRun(paragraph: XWPFParagraph, text: String) {
        val run = paragraph.createRun()
        run.isBold = true
        run.isItalic = true
        run.setText(text)
    }
}
