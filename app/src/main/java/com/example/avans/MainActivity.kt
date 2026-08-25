package com.example.avans

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AvansReportScreen(
                        onGenerateReport = { reportData ->
                            generateAndOpenReport(reportData)
                        }
                    )
                }
            }
        }
    }

    private fun generateAndOpenReport(data: ReportData) {
    try {
        val outFile = File(cacheDir, "avans_report.docx")
        DocxGenerator.generateReport(this, data, outFile)

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            outFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Открыть или распечатать отчет"))
    } catch (e: Exception) {
        e.printStackTrace()
        val errorDetails = e.localizedMessage ?: e.javaClass.simpleName
        Toast.makeText(this, "Ошибка: $errorDetails", Toast.LENGTH_LONG).show()
    }
}


    data class ExpenseInputState(
        val date: String = "",
        val docNumber: String = "",
        val name: String = "",
        val sum: String = ""
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AvansReportScreen(onGenerateReport: (ReportData) -> Unit) {
        var reportDate by remember { mutableStateOf("12.08.2026") }
        var purpose by remember { mutableStateOf("Командировка в Варандей") }
        
        var startDate by remember { mutableStateOf("28.07.2026") }
        var endDate by remember { mutableStateOf("11.08.2026") }
        var perDiemSum by remember { mutableStateOf("10500.00") }

        val expensesList = remember { mutableStateListOf<ExpenseInputState>() }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Авансовый отчет АО-1") }) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("1. Лицевая сторона (Шапка)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = reportDate,
                        onValueChange = { reportDate = it },
                        label = { Text("Дата отчета") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Назначение аванса") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("2. Суточные (1-я строка таблицы)", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Дата начала") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("Дата конца") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = perDiemSum,
                        onValueChange = { perDiemSum = it },
                        label = { Text("Сумма суточных (руб.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3. Чеки и билеты", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { expensesList.add(ExpenseInputState()) }) {
                            Text("+ Добавить чек")
                        }
                    }
                }

                itemsIndexed(expensesList) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Строка №${index + 2}", style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = { expensesList.removeAt(index) }) {
                                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = item.date,
                                    onValueChange = { expensesList[index] = item.copy(date = it) },
                                    label = { Text("Дата") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = item.docNumber,
                                    onValueChange = { expensesList[index] = item.copy(docNumber = it) },
                                    label = { Text("№ чека/билета") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { expensesList[index] = item.copy(name = it) },
                                label = { Text("Наименование расхода") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = item.sum,
                                onValueChange = { expensesList[index] = item.copy(sum = it) },
                                label = { Text("Сумма (руб.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val parsedPerDiem = perDiemSum.toDoubleOrNull() ?: 0.0
                            val parsedExpenses = expensesList.map {
                                ExpenseItem(
                                    date = it.date,
                                    docNumber = it.docNumber,
                                    name = it.name,
                                    sum = it.sum.toDoubleOrNull() ?: 0.0
                                )
                            }
                            val data = ReportData(
                                reportDate = reportDate,
                                purpose = purpose,
                                startDate = startDate,
                                endDate = endDate,
                                perDiemSum = parsedPerDiem,
                                expenses = parsedExpenses
                            )
                            onGenerateReport(data)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Сформировать и открыть .docx")
                    }
                }
            }
        }
    }
}
