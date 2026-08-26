package com.example.avans

import android.content.Context
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream

data class FlightLeg(
    val from: String = "",
    val to: String = "",
    val depDay: String = "",
    val depMonth: String = "",
    val depYear: String = "",
    val arrDay: String = "",
    val arrMonth: String = "",
    val arrYear: String = "",
    val taskNumber: String = ""
)

data class MemoData(
    val employeeName: String,
    val position: String,
    val department: String,
    val tabNum: String,
    val legs: List<FlightLeg>
)

object XlsxGenerator {

    fun generateMemo(context: Context, data: MemoData, outputFile: File) {
        context.assets.open("template_memo.xlsx").use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            // Словарь замен для шапки
            val replacements = mutableMapOf(
                "{{EMPLOYEE_NAME}}" to data.employeeName,
                "{{POSITION}}" to data.position,
                "{{DEPARTMENT}}" to data.department,
                "{{TAB_NUMBER}}" to data.tabNum
            )

            // Заполнение 7 блоков перелетов
            for (i in 1..7) {
                val leg = data.legs.getOrNull(i - 1)
                replacements["{{from_$i}}"] = leg?.from ?: ""
                replacements["{{to_$i}}"] = leg?.to ?: ""
                replacements["{{dep_d_$i}}"] = leg?.depDay ?: ""
                replacements["{{dep_m_$i}}"] = leg?.depMonth ?: ""
                replacements["{{dep_y_$i}}"] = leg?.depYear ?: ""
                replacements["{{arr_d_$i}}"] = leg?.arrDay ?: ""
                replacements["{{arr_m_$i}}"] = leg?.arrMonth ?: ""
                replacements["{{arr_y_$i}}"] = leg?.arrYear ?: ""
                replacements["{{task_$i}}"] = leg?.taskNumber ?: ""
            }

            // Обход всех ячеек листа и замена совпадений
            for (row in sheet) {
                for (cell in row) {
                    if (cell.cellType == CellType.STRING) {
                        var cellValue = cell.stringCellValue
                        var modified = false

                        for ((key, value) in replacements) {
                            if (cellValue.contains(key)) {
                                cellValue = cellValue.replace(key, value)
                                modified = true
                            }
                        }

                        if (modified) {
                            cell.setCellValue(cellValue)
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { out ->
                workbook.write(out)
            }
            workbook.close()
        }
    }
}
