package com.example.avans

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> {
                // Основной авансовый отчет
            }
            1 -> {
                FlightDetailsBlock()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsBlock() {
    val context = LocalContext.current
    val historyManager = remember { LocationHistoryManager(context) }

    var departureDate by remember { mutableStateOf("") }
    var arrivalDate by remember { mutableStateOf("") }
    var fromLocation by remember { mutableStateOf("") }
    var toLocation by remember { mutableStateOf("") }
    var flightTask by remember { mutableStateOf("") }

    val savedLocations = remember { mutableStateListOf(*historyManager.getSavedLocations().toTypedArray()) }
    val calendar = Calendar.getInstance()

    val departureDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> departureDate = String.format("%02d.%02d.%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val arrivalDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> arrivalDate = String.format("%02d.%02d.%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Сведения о перелетах", style = MaterialTheme.typography.titleLarge)

        OutlinedButton(onClick = { departureDatePicker.show() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (departureDate.isEmpty()) "Выберите дату убытия" else "Убытие: $departureDate")
        }

        OutlinedButton(onClick = { arrivalDatePicker.show() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (arrivalDate.isEmpty()) "Выберите дату прибытия" else "Прибытие: $arrivalDate")
        }

        AutoCompleteField("Откуда", fromLocation, { fromLocation = it }, savedLocations)
        AutoCompleteField("Куда", toLocation, { toLocation = it }, savedLocations)

        OutlinedTextField(
            value = flightTask,
            onValueChange = { flightTask = it },
            label = { Text("№ Полетного задания") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (fromLocation.isNotBlank()) historyManager.saveLocation(fromLocation)
                if (toLocation.isNotBlank()) historyManager.saveLocation(toLocation)

                val flight = FlightData(departureDate, arrivalDate, fromLocation, toLocation, flightTask)
                val pdfFile = File(context.cacheDir, "Убытие_прибытие.pdf")
                PdfFlightReportGenerator.generatePdf(context, listOf(flight), pdfFile)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сформировать PDF")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteField(label: String, value: String, onValueChange: (String) -> Unit, suggestions: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = suggestions.filter { it.contains(value, ignoreCase = true) }

    ExposedDropdownMenuBox(expanded = expanded && filtered.isNotEmpty(), onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && filtered.isNotEmpty(), onDismissRequest = { expanded = false }) {
            filtered.forEach { selection ->
                DropdownMenuItem(text = { Text(selection) }, onClick = { onValueChange(selection); expanded = false })
            }
        }
    }
}
