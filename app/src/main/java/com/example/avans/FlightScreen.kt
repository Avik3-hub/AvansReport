package com.example.avans

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.util.Calendar

@Composable
fun FlightDetailsBlock() {
    val context = LocalContext.current
    val historyManager = remember { LocationHistoryManager(context) }

    // Список блоков перелетов (по умолчанию 1)
    var flightList by remember { mutableStateOf(listOf(FlightData())) }
    val savedLocations = remember { mutableStateListOf(*historyManager.getSavedLocations().toTypedArray()) }

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
                Text(
                    text = "Сведения о перелетах",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = {
                    flightList = flightList + FlightData()
                }) {
                    Text("+ Добавить перелет")
                }
            }
        }

        itemsIndexed(flightList) { index, flight ->
            FlightItemCard(
                index = index + 1,
                flight = flight,
                savedLocations = savedLocations,
                canDelete = flightList.size > 1,
                onUpdate = { updated ->
                    val newList = flightList.toMutableList()
                    newList[index] = updated
                    flightList = newList
                },
                onDelete = {
                    val newList = flightList.toMutableList()
                    newList.removeAt(index)
                    flightList = newList
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // Сохраняем все введенные города в историю
                    flightList.forEach { item ->
                        if (item.fromLocation.isNotBlank()) {
                            historyManager.saveLocation(item.fromLocation)
                        }
                        if (item.toLocation.isNotBlank()) {
                            historyManager.saveLocation(item.toLocation)
                        }
                    }
                    
                    // Обновляем список подсказок
                    savedLocations.clear()
                    savedLocations.addAll(historyManager.getSavedLocations())

                    // Генерируем и сразу открываем PDF
                    generateAndOpenPdf(context, flightList)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сформировать и открыть PDF")
            }
        }
    }
}

@Composable
fun FlightItemCard(
    index: Int,
    flight: FlightData,
    savedLocations: List<String>,
    canDelete: Boolean,
    onUpdate: (FlightData) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val departureDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val formatted = String.format("%02d.%02d.%d", d, m + 1, y)
            onUpdate(flight.copy(departureDate = formatted))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val arrivalDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val formatted = String.format("%02d.%02d.%d", d, m + 1, y)
            onUpdate(flight.copy(arrivalDate = formatted))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Перелет №$index", style = MaterialTheme.typography.titleMedium)
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить перелет",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { departureDatePicker.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (flight.departureDate.isEmpty()) "Дата убытия"
                        else "Убытие: ${flight.departureDate}",
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { arrivalDatePicker.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (flight.arrivalDate.isEmpty()) "Дата прибытия"
                        else "Прибытие: ${flight.arrivalDate}",
                        maxLines = 1
                    )
                }
            }

            AutoCompleteField(
                label = "Откуда",
                value = flight.fromLocation,
                onValueChange = { onUpdate(flight.copy(fromLocation = it)) },
                suggestions = savedLocations
            )

            AutoCompleteField(
                label = "Куда",
                value = flight.toLocation,
                onValueChange = { onUpdate(flight.copy(toLocation = it)) },
                suggestions = savedLocations
            )

            OutlinedTextField(
                value = flight.flightTask,
                onValueChange = { onUpdate(flight.copy(flightTask = it)) },
                label = { Text("№ Полетного задания") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = suggestions.filter { it.contains(value, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filtered.forEach { selection ->
                DropdownMenuItem(
                    text = { Text(selection) },
                    onClick = {
                        onValueChange(selection)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun generateAndOpenPdf(context: Context, items: List<FlightData>) {
    try {
        val outFile = File(context.cacheDir, "Убытие_прибытие.pdf")
        PdfFlightReportGenerator.generatePdf(context, items, outFile)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            outFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Открыть PDF отчет"))
    } catch (e: Exception) {
        e.printStackTrace()
        val errorDetails = e.localizedMessage ?: e.javaClass.simpleName
        Toast.makeText(context, "Ошибка при открытии PDF: $errorDetails", Toast.LENGTH_LONG).show()
    }
}
