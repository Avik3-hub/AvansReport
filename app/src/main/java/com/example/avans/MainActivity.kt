package com.example.avans

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.core.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AvansReportScreen()
                }
            }
        }
    }
}

enum class Region(val title: String) {
    SOUTH("Юг"),
    NORTH("Север")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvansReportScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    // Ставки суточных (сохраняются в настройках)
    var southRate by remember { mutableDoubleStateOf(prefs.getFloat("south_rate", 500f).toDouble()) }
    var northRate by remember { mutableDoubleStateOf(prefs.getFloat("north_rate", 700f).toDouble()) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Основные поля отчета
    var reportDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))) }
    var purpose by remember { mutableStateOf("Командировка") }
    
    // Суточные
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf(Region.SOUTH) }
    
    // Авторасчет суточных
    val currentRate = if (selectedRegion == Region.SOUTH) southRate else northRate
    val daysCount = remember(startDate, endDate) { calculateDays(startDate, endDate) }
    val perDiemSum = daysCount * currentRate

    // Список расходов
    var expenses by remember { mutableStateOf(listOf<ExpenseItem>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Авансовый отчет АО-1") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки суточных")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Общая информация
            item {
                Text("1. Основные данные", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                DatePickerField(
                    label = "Дата составления отчета",
                    value = reportDate,
                    onDateSelected = { reportDate = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Назначение аванса") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Суточные
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. Суточные (1-я строка таблицы)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Выбор региона:", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            FilterChip(
                                selected = selectedRegion == Region.SOUTH,
                                onClick = { selectedRegion = Region.SOUTH },
                                label = { Text("Юг (${southRate.toInt()} ₽/день)") }
                            )
                            FilterChip(
                                selected = selectedRegion == Region.NORTH,
                                onClick = { selectedRegion = Region.NORTH },
                                label = { Text("Север (${northRate.toInt()} ₽/день)") }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DatePickerField(
                                label = "Дата начала",
                                value = startDate,
                                onDateSelected = { startDate = it },
                                modifier = Modifier.weight(1f)
                            )
                            DatePickerField(
                                label = "Дата конца",
                                value = endDate,
                                onDateSelected = { endDate = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Дней: $daysCount | Сумма: ${String.format(Locale.US, "%.2f", perDiemSum)} ₽",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. Расходы (Чеки и билеты)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Чеки и билеты", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = {
                        expenses = expenses + ExpenseItem("", "", "", 0.0)
                    }) {
                        Text("+ Добавить чек")
                    }
                }
            }

            itemsIndexed(expenses) { index, expense ->
                ExpenseCard(
                    index = index + 2,
                    expense = expense,
                    onUpdate = { updated ->
                        val newList = expenses.toMutableList()
                        newList[index] = updated
                        expenses = newList
                    },
                    onDelete = {
                        val newList = expenses.toMutableList()
                        newList.removeAt(index)
                        expenses = newList
                    }
                )
            }

            // Кнопка генерации
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val data = ReportData(
                            reportDate = reportDate,
                            purpose = purpose,
                            startDate = startDate,
                            endDate = endDate,
                            perDiemSum = perDiemSum,
                            expenses = expenses
                        )
                        generateAndOpenReport(context, data)
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

    // Диалог настроек ставок
    if (showSettingsDialog) {
        SettingsDialog(
            currentSouth = southRate,
            currentNorth = northRate,
            onDismiss = { showSettingsDialog = false },
            onSave = { newSouth, newNorth ->
                southRate = newSouth
                northRate = newNorth
                prefs.edit()
                    .putFloat("south_rate", newSouth.toFloat())
                    .putFloat("north_rate", newNorth.toFloat())
                    .apply()
                showSettingsDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Календарь") },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ExpenseCard(
    index: Int,
    expense: ExpenseItem,
    onUpdate: (ExpenseItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Строка №$index", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = "Дата",
                    value = expense.date,
                    onDateSelected = { onDateSelected -> onUpdate(expense.copy(date = onDateSelected)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = expense.docNumber,
                    onValueChange = { onUpdate(expense.copy(docNumber = it)) },
                    label = { Text("№ чека/билета") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = expense.name,
                onValueChange = { onUpdate(expense.copy(name = it)) },
                label = { Text("Наименование расхода") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = if (expense.sum == 0.0) "" else expense.sum.toString(),
                onValueChange = { onUpdate(expense.copy(sum = it.toDoubleOrNull() ?: 0.0)) },
                label = { Text("Сумма (руб.)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsDialog(
    currentSouth: Double,
    currentNorth: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double) -> Unit
) {
    var southInput by remember { mutableStateOf(currentSouth.toInt().toString()) }
    var northInput by remember { mutableStateOf(currentNorth.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки ставок суточных") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = southInput,
                    onValueChange = { southInput = it },
                    label = { Text("Ставка Юг (руб/день)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = northInput,
                    onValueChange = { northInput = it },
                    label = { Text("Ставка Север (руб/день)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = southInput.toDoubleOrNull() ?: currentSouth
                val n = northInput.toDoubleOrNull() ?: currentNorth
                onSave(s, n)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

fun calculateDays(startDateStr: String, endDateStr: String): Long {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val start = LocalDate.parse(startDateStr, formatter)
        val end = LocalDate.parse(endDateStr, formatter)
        if (!end.isBefore(start)) {
            ChronoUnit.DAYS.between(start, end) + 1
        } else 0L
    } catch (e: Exception) {
        0L
    }
}

private fun generateAndOpenReport(context: Context, data: ReportData) {
    try {
        val outFile = File(context.cacheDir, "avans_report.docx")
        DocxGenerator.generateReport(context, data, outFile)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            outFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Открыть или распечатать отчет"))
    } catch (e: Exception) {
        e.printStackTrace()
        val errorDetails = e.localizedMessage ?: e.javaClass.simpleName
        Toast.makeText(context, "Ошибка: $errorDetails", Toast.LENGTH_LONG).show()
    }
}
