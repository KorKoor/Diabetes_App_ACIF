package com.example.diabetes_app.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diabetes_app.R
import com.example.diabetes_app.data.DailyRecordData
import com.example.diabetes_app.data.MealRecordData
import com.example.diabetes_app.data.MealType
import com.example.diabetes_app.data.BloodPressureReading
import com.example.diabetes_app.data.GlucoseReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    navController: NavController,
    recordData: Any? // Puede ser DailyRecordData o MealRecordData (deserializado de JSON)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Registro", color = colorResource(id = R.color.headerText)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = colorResource(id = R.color.headerText))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.headerBackground))
            )
        },
        containerColor = colorResource(id = R.color.appBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (recordData) {
                is DailyRecordData -> DailyRecordDetailContent(recordData)
                is MealRecordData -> MealRecordDetailContent(recordData)
                else -> {
                    Text(
                        "No se pudo cargar el detalle del registro o el formato es incorrecto.",
                        fontSize = 18.sp,
                        color = colorResource(id = R.color.primaryText)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyRecordDetailContent(record: DailyRecordData) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val recordTime = if (record.date != null) timeFormatter.format(record.date) else "N/A"

    DetailCard(title = "Registro Diario de Vitals y Detalles") {
        DetailRow("Fecha:", formatter.format(record.date))
        DetailRow("Hora de la Toma:", recordTime)
        Spacer(modifier = Modifier.height(16.dp))

        Text("GLUCOSA Y PRESIÓN:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colorResource(id = R.color.primaryText))
        Spacer(modifier = Modifier.height(8.dp))

        // Mostrar lecturas de glucosa
        if (record.glucoseReadings.isNotEmpty()) {
            record.glucoseReadings.forEachIndexed { index, reading ->
                DetailRow("Glucosa ${index + 1}:", "${reading.value} mg/dL (${reading.moment})")
            }
        } else {
            Text("- No se registraron lecturas de glucosa.", fontSize = 14.sp, color = colorResource(id = R.color.textSecondary))
        }

        // Mostrar lectura de presión y pulso
        record.bloodPressureReadings.lastOrNull()?.let { pressure ->
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("Presión Arterial:", "${pressure.sistolica}/${pressure.diastolica} mmHg")
            DetailRow("Pulso:", "${pressure.pulso} LPM")
        }


        if (record.symptoms.isNotEmpty() || record.activities.isNotEmpty() || record.notes?.isNotBlank() == true || record.foodTypes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("DETALLES ADICIONALES DEL DÍA:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colorResource(id = R.color.primaryText))
            Spacer(modifier = Modifier.height(8.dp))

            if (record.symptoms.isNotEmpty()) {
                DetailRow("Síntomas Reportados:", record.symptoms.joinToString(", "))
            }
            if (record.activities.isNotEmpty()) {
                DetailRow("Actividad Física:", record.activities.joinToString())
                record.activityTime?.let { DetailRow("Duración:", it) }
            }
            if (record.foodTypes.isNotEmpty()) {
                DetailRow("Tipos de Comida:", record.foodTypes.joinToString(", "))
            }
            if (record.notes?.isNotBlank() == true) {
                // Para notas largas, usamos un Text simple en lugar de DetailRow para evitar desbordamiento
                Text("Notas:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colorResource(id = R.color.textSecondary))
                Text(record.notes, fontSize = 14.sp, color = colorResource(id = R.color.primaryText))
            }
        }
    }
}

@Composable
fun MealRecordDetailContent(mealRecord: MealRecordData) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    val mealName = when (mealRecord.mealType) {
        MealType.ALMUERZO -> "Comida"
        else -> mealRecord.mealType.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    DetailCard(title = "Detalle de Registro de Comida") {
        DetailRow("Tipo de Comida:", mealName.uppercase(Locale.getDefault()))
        DetailRow("Fecha:", dateFormatter.format(mealRecord.date))
        DetailRow("Hora Aproximada:", timeFormatter.format(mealRecord.date))
        DetailRow("Calorías Estimadas:", "${mealRecord.actualCalories} Cal")
        Spacer(modifier = Modifier.height(16.dp))

        Text("ITEMS CONSUMIDOS:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colorResource(id = R.color.primaryText))
        Spacer(modifier = Modifier.height(8.dp))

        if (mealRecord.selectedItemsWithQuantities.isNotEmpty()) {
            mealRecord.selectedItemsWithQuantities.forEach { item ->
                val quantity = item.quantityLevel.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                Text("- ${item.itemName} (Porción: $quantity)", fontSize = 14.sp, color = colorResource(id = R.color.primaryText), modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            Text("- No se especificaron los ítems.", fontSize = 14.sp, color = colorResource(id = R.color.textSecondary))
        }
    }
}

@Composable
fun DetailCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = colorResource(id = R.color.buttonPrimaryBackground))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colorResource(id = R.color.dividerColor))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colorResource(id = R.color.textSecondary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, fontSize = 14.sp, color = colorResource(id = R.color.primaryText))
    }
}