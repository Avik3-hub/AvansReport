package com.example.avans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import org.json.JSONArray
import org.json.JSONObject
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
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", false)) }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = if (isDarkMode) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = if (isDarkMode) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !isDarkMode
                    insetsController.isAppearanceLightNavigationBars = !isDarkMode
                }
            }
            val oledDarkColorScheme = darkColorScheme(
                primary = Color(0xFF90CAF9),
                secondary = Color(0xFF64B5F6),
                background = Color(0xFF000000),
                surface = Color(0xFF000000),
                surfaceVariant = Color(0xFF121212),
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5),
                onSurfaceVariant = Color(0xFFCAC4D0)
            )
            val lightColorScheme = lightColorScheme(
                primary = Color(0xFF1976D2),
                secondary = Color(0xFF0288D1)
            )
            val colorScheme = if (isDarkMode) oledDarkColorScheme else lightColorScheme
            MaterialTheme(colorScheme = colorScheme) {
                MainAppPager(
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        isDarkMode = !isDarkMode
                        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAppPager(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (page) {
                    0 -> FlightDetailsBlock()
                    1 -> AvansReportScreen(isDarkMode = isDarkMode, onThemeToggle = onThemeToggle)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pages = listOf("Перелеты (1/2)", "Авансовый отчет (2/2)")
            pages.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class FlightLegInput(
    val depDate: String = "",
    val arrDate: String = "",
    val from: String = "",
    val to: String = "",
    val taskNumber: String = ""
)

@Composable
fun FlightDetailsBlock() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var legs by remember { mutableStateOf(loadDraftLegs(prefs)) }

    LaunchedEffect(legs) {
        saveDraftLegs(prefs, legs)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Перелеты", style = MaterialTheme.typography.titleLarge)
                Button(onClick = {
                    if (legs.size < 7) {
                        legs = legs + FlightLegInput()
                    } else {
                        Toast.makeText(context, "Максимум 7 перелетов", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("+ Перелет")
                }
            }
        }

        if (legs.isEmpty()) {
            item {
                Text(
                    text = "Список перелетов пуст. Нажмите «+ Перелет» для добавления.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(legs) { index, leg ->
            FlightLegCard(
                index = index + 1,
                leg = leg,
                onUpdate = { updated ->
                    val newList = legs.toMutableList()
                    newList[index] = updated
                    legs = newList
                },
                onDelete = {
                    val newList = legs.toMutableList()
                    newList.removeAt(index)
                    legs = newList
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { generateAndOpenMemo(context, legs) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сформировать и открыть .xlsx")
            }
        }
    }
}

@Composable
fun FlightLegCard(
    index: Int,
    leg: FlightLegInput,
    onUpdate: (FlightLegInput) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Перелет No$index", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = "Убытие:",
                    value = leg.depDate,
                    onDateSelected = { onUpdate(leg.copy(depDate = it)) },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "Прибытие:",
                    value = leg.arrDate,
                    onDateSelected = { onUpdate(leg.copy(arrDate = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = leg.from,
                onValueChange = { onUpdate(leg.copy(from = it)) },
                label = { Text("Откуда") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = leg.to,
                onValueChange = { onUpdate(leg.copy(to = it)) },
                label = { Text("Куда") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = leg.taskNumber,
                onValueChange = { onUpdate(leg.copy(taskNumber = it)) },
                label = { Text("No Полетного задания") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

enum class Region { SOUTH, NORTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvansReportScreen(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var southRate by remember { mutableDoubleStateOf(prefs.getFloat("south_rate", 500f).toDouble()) }
    var northRate by remember { mutableDoubleStateOf(prefs.getFloat("north_rate", 700f).toDouble()) }
    var employeeName by remember { mutableStateOf(prefs.getString("emp_name", "Нагибин Сергей Викторович") ?: "Нагибин Сергей Викторович") }
    var tabNumber by remember { mutableStateOf(prefs.getString("emp_tab_number", "8701") ?: "8701") }
    var position by remember { mutableStateOf(prefs.getString("emp_position", "техник АиРЭО") ?: "техник АиРЭО") }
    var department by remember { mutableStateOf(prefs.getString("emp_department", "участок ТО вертолетов") ?: "участок ТО вертолетов") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showEmployeeDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var destinationHistory by remember { mutableStateOf(loadHistory(prefs, "history_destinations")) }
    var expenseNameHistory by remember { mutableStateOf(loadHistory(prefs, "history_expense_names")) }
    var reportDate by remember {
        mutableStateOf(prefs.getString("draft_report_date", null) ?: LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    }
    var destinationCity by remember {
        mutableStateOf(prefs.getString("draft_destination_city", "Вологду") ?: "Вологду")
    }
    var startDate by remember {
        mutableStateOf(prefs.getString("draft_start_date", "") ?: "")
    }
    var endDate by remember {
        mutableStateOf(prefs.getString("draft_end_date", "") ?: "")
    }
    var selectedRegion by remember {
        val regionStr = prefs.getString("draft_region", Region.SOUTH.name)
        mutableStateOf(if (regionStr == Region.NORTH.name) Region.NORTH else Region.SOUTH)
    }
    var expenses by remember {
        mutableStateOf(loadDraftExpenses(prefs))
    }
    LaunchedEffect(reportDate, destinationCity, startDate, endDate, selectedRegion, expenses) {
        saveDraft(prefs, reportDate, destinationCity, startDate, endDate, selectedRegion, expenses)
    }
    val currentRate = if (selectedRegion == Region.SOUTH) southRate else northRate
    val daysCount = remember(startDate, endDate) { calculateDays(startDate, endDate) }
    val perDiemSum = daysCount * currentRate
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Авансовый отчет АО-1") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Данные сотрудника") },
                            onClick = {
                                showMenu = false
                                showEmployeeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Настройки суточных") },
                            onClick = {
                                showMenu = false
                                showSettingsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(if (isDarkMode) "Тема оформления: светлая" else "Тема оформления: темная") 
                            },
                            onClick = {
                                showMenu = false
                                onThemeToggle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сброс заполнения") },
                            onClick = {
                                showMenu = false
                                reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                destinationCity = ""
                                startDate = ""
                                endDate = ""
                                selectedRegion = Region.SOUTH
                                expenses = emptyList()
                                Toast.makeText(context, "Черновик очищен", Toast.LENGTH_SHORT).show()
                            }
                        )
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
            item {
                Text("1. Основные данные", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                DatePickerField(
                    label = "Дата составления отчета",
                    value = reportDate,
                    onDateSelected = { reportDate = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                AutoCompleteTextField(
                    value = destinationCity,
                    onValueChange = { destinationCity = it },
                    label = "Место назначения",
                    prefixText = "Командировка в ",
                    history = destinationHistory
                )
            }
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
                    nameHistory = expenseNameHistory,
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
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val fullPurpose = "Командировка в $destinationCity".trim()
                        
                        destinationHistory = saveHistoryItem(prefs, "history_destinations", destinationCity)
                        expenses.map { it.name }.filter { it.isNotBlank() }.forEach { name ->
                            expenseNameHistory = saveHistoryItem(prefs, "history_expense_names", name)
                        }
                        val data = ReportData(
                            reportDate = reportDate,
                            purpose = fullPurpose,
                            startDate = startDate,
                            endDate = endDate,
                            perDiemSum = perDiemSum,
                            expenses = expenses,
                            employee = EmployeeInfo(
                                name = employeeName.toShortName(),
                                tabNumber = tabNumber,
                                position = position,
                                department = department
                            )
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
    if (showEmployeeDialog) {
        EmployeeDialog(
            currentName = employeeName,
            currentTabNumber = tabNumber,
            currentPosition = position,
            currentDepartment = department,
            onDismiss = { showEmployeeDialog = false },
            onSave = { name, tab, pos, dept ->
                employeeName = name
                tabNumber = tab
                position = pos
                department = dept
                prefs.edit()
                    .putString("emp_name", name)
                    .putString("emp_tab_number", tab)
                    .putString("emp_position", pos)
                    .putString("emp_department", dept)
                    .apply()
                showEmployeeDialog = false
            }
        )
    }
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

@Composable
fun EmployeeDialog(
    currentName: String,
    currentTabNumber: String,
    currentPosition: String,
    currentDepartment: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var tabNumber by remember { mutableStateOf(currentTabNumber) }
    var position by remember { mutableStateOf(currentPosition) }
    var department by remember { mutableStateOf(currentDepartment) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Данные сотрудника") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Подотчетное лицо (ФИО полностью)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = tabNumber,
                    onValueChange = { tabNumber = it },
                    label = { Text("Табельный номер") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Профессия (должность)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Структурное подразделение") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, tabNumber, position, department) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefixText: String? = null,
    history: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredHistory = remember(value, history) {
        if (value.isBlank()) history else history.filter { it.contains(value, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        expanded = expanded && filteredHistory.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            prefix = prefixText?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true
        )
        if (filteredHistory.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredHistory.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onValueChange(item)
                            expanded = false
                        }
                    )
                }
            }
        }
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
    nameHistory: List<String>,
    onUpdate: (ExpenseItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
            
            AutoCompleteTextField(
                value = expense.name,
                onValueChange = { name -> onUpdate(expense.copy(name = name)) },
                label = "Наименование расхода",
                history = nameHistory
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

fun String.toShortName(): String {
    val parts = this.trim().split("\\s+".toRegex())
    if (parts.isEmpty()) return ""
    val lastName = parts[0]
    val firstNameInitial = parts.getOrNull(1)?.firstOrNull()?.let { "$it." } ?: ""
    val patronymicInitial = parts.getOrNull(2)?.firstOrNull()?.let { "$it." } ?: ""
    return "$lastName $firstNameInitial$patronymicInitial".trim()
}

fun parseDateToComponents(dateStr: String): Triple<String, String, String> {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val date = LocalDate.parse(dateStr, formatter)
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        Triple(
            date.dayOfMonth.toString(),
            months[date.monthValue - 1],
            date.year.toString()
        )
    } catch (e: Exception) {
        Triple("", "", "")
    }
}

fun saveDraftLegs(prefs: android.content.SharedPreferences, legs: List<FlightLegInput>) {
    val jsonArray = JSONArray()
    legs.forEach { item ->
        val obj = JSONObject()
        obj.put("depDate", item.depDate)
        obj.put("arrDate", item.arrDate)
        obj.put("from", item.from)
        obj.put("to", item.to)
        obj.put("taskNumber", item.taskNumber)
        jsonArray.put(obj)
    }
    prefs.edit().putString("draft_flight_legs", jsonArray.toString()).apply()
}

fun loadDraftLegs(prefs: android.content.SharedPreferences): List<FlightLegInput> {
    val jsonStr = prefs.getString("draft_flight_legs", null) ?: return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<FlightLegInput>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                FlightLegInput(
                    depDate = obj.optString("depDate", ""),
                    arrDate = obj.optString("arrDate", ""),
                    from = obj.optString("from", ""),
                    to = obj.optString("to", ""),
                    taskNumber = obj.optString("taskNumber", "")
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveDraft(
    prefs: android.content.SharedPreferences,
    reportDate: String,
    destinationCity: String,
    startDate: String,
    endDate: String,
    region: Region,
    expenses: List<ExpenseItem>
) {
    val jsonArray = JSONArray()
    expenses.forEach { item ->
        val obj = JSONObject()
        obj.put("date", item.date)
        obj.put("docNumber", item.docNumber)
        obj.put("name", item.name)
        obj.put("sum", item.sum)
        jsonArray.put(obj)
    }
    prefs.edit()
        .putString("draft_report_date", reportDate)
        .putString("draft_destination_city", destinationCity)
        .putString("draft_start_date", startDate)
        .putString("draft_end_date", endDate)
        .putString("draft_region", region.name)
        .putString("draft_expenses", jsonArray.toString())
        .apply()
}

fun loadDraftExpenses(prefs: android.content.SharedPreferences): List<ExpenseItem> {
    val jsonStr = prefs.getString("draft_expenses", null) ?: return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<ExpenseItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ExpenseItem(
                    date = obj.optString("date", ""),
                    docNumber = obj.optString("docNumber", ""),
                    name = obj.optString("name", ""),
                    sum = obj.optDouble("sum", 0.0)
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun loadHistory(prefs: android.content.SharedPreferences, key: String): List<String> {
    val set = prefs.getStringSet(key, emptySet()) ?: emptySet()
    return set.toList()
}

fun saveHistoryItem(prefs: android.content.SharedPreferences, key: String, item: String): List<String> {
    if (item.isBlank()) return loadHistory(prefs, key)
    val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
    current.add(item.trim())
    prefs.edit().putStringSet(key, current).apply()
    return current.toList()
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
        val safePurpose = data.purpose
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifEmpty { "Авансовый_отчет" }
        val fileName = "$safePurpose.docx"
        val outFile = File(context.cacheDir, fileName)
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

private fun generateAndOpenMemo(context: Context, legsInput: List<FlightLegInput>) {
    try {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val fullName = prefs.getString("emp_name", "Нагибин Сергей Викторович") ?: "Нагибин Сергей Викторович"
        val tabNumber = prefs.getString("emp_tab_number", "8701") ?: "8701"
        val position = prefs.getString("emp_position", "техник АиРЭО") ?: "техник АиРЭО"
        val department = prefs.getString("emp_department", "участок ТО вертолетов") ?: "участок ТО вертолетов"

        val memoLegs = legsInput.map { leg ->
            val (depD, depM, depY) = parseDateToComponents(leg.depDate)
            val (arrD, arrM, arrY) = parseDateToComponents(leg.arrDate)
            FlightLeg(
                from = leg.from,
                to = leg.to,
                depDay = depD,
                depMonth = depM,
                depYear = depY,
                arrDay = arrD,
                arrMonth = arrM,
                arrYear = arrY,
                taskNumber = leg.taskNumber
            )
        }

        val memoData = MemoData(
            employeeName = fullName,
            position = position,
            department = department,
            tabNum = tabNumber,
            legs = memoLegs
        )

        val fileName = "Убытие_прибытие.xlsx"
        val outFile = File(context.cacheDir, fileName)
        XlsxGenerator.generateMemo(context, memoData, outFile)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            outFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Открыть или распечатать служебную записку"))
    } catch (e: Exception) {
        e.printStackTrace()
        val errorDetails = e.localizedMessage ?: e.javaClass.simpleName
        Toast.makeText(context, "Ошибка: $errorDetails", Toast.LENGTH_LONG).show()
    }
}
