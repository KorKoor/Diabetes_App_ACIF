package com.example.diabetes_app.ui.analysis

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.diabetes_app.R
import com.example.diabetes_app.data.UserProfile
import com.example.diabetes_app.data.MedicationData
import com.example.diabetes_app.data.DailyRecordData
import com.example.diabetes_app.data.MealRecordData
import com.example.diabetes_app.data.MealType
import com.example.diabetes_app.data.DosageTakenRecord
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.example.diabetes_app.GlucoseStats
import com.example.diabetes_app.ActivitySummary
import com.example.diabetes_app.DietaryHabitsSummary
import com.example.diabetes_app.MedicationAdherenceSummary
import com.example.diabetes_app.data.GlucoseReading
import com.example.diabetes_app.ui.analysis.AnalysisViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.tasks.await
import java.util.Date
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.nestedScroll

// --- INICIO DE CLASES DE DATOS Y CONSTANTES (Mantén estas definiciones UNA SOLA VEZ) ---
data class GlucoseStats(
    val average: Float,
    val min: Float,
    val max: Float,
    val inRangeCount: Int,
    val highCount: Int,
    val lowCount: Int,
    val totalReadings: Int,
    val stdDev: Float,
    val tir: Float,
    val tar: Float,
    val tbr: Float,
    val cv: Float,
    val severeLowCount: Int,
    val severeHighCount: Int
)

data class ActivitySummary(
    val totalMinutes: Int,
    val uniqueActivities: String
)

data class DietaryHabitsSummary(
    val foodTypeFrequency: String,
    val mealTypeCounts: Map<MealType, Int>,
    val totalCaloriesRecorded: Int
)

data class MedicationAdherenceSummary(
    val adherencePercentage: String,
    val note: String,
    val medications: List<MedicationData>
)

val PrimaryBlue = Color(0xFF42A5F5)
val DarkBlue = Color(0xFF1976D2)
val LightGrayBackground = Color(0xFFF0F2F5)
val CardBackground = Color.White
val TextDark = Color(0xFF212121)
val TextLight = Color(0xFF757575)
val AccentRed = Color(0xFFEF5350)
val GlucoseHighColor = Color(0xFFEF5350)
val GlucoseNormalColor = Color(0xFF66BB6A)
val GlucoseLowColor = Color(0xFFFFA726)

const val GLUCOSE_NORMAL_MIN_AN = 70f
const val GLUCOSE_NORMAL_MAX_AN = 130f
const val GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN = 54f
const val GLUCOSE_HYPERGLYCEMIA_THRESHOLD_AN = 180f
const val GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN = 250f

enum class AnalysisTabType {
    PATIENT, // Nueva pestaña
    GLUCOSE,
    ACTIVITY,
    SYMPTOMS,
    DIET
}
// --- FIN DE CLASES DE DATOS Y CONSTANTES ---


// --- INICIO DEL COMPOSABLE PRINCIPAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: AnalysisViewModel = viewModel()
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val selectedPeriodDays by viewModel.selectedPeriodDays.collectAsState()
    var selectedAnalysisTab by remember { mutableStateOf(AnalysisTabType.GLUCOSE) }

    // Estado para las Observaciones Clínicas (persistente en recomposiciones y recreaciones)
    var clinicalNotes by rememberSaveable { mutableStateOf("") }

    val firestoreDb = remember { FirebaseFirestore.getInstance() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

// Para capturar siempre el valor más reciente dentro de callbacks
    val currentNotesState = rememberUpdatedState(clinicalNotes)


    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect

        // Cargar datos iniciales del ViewModel
        viewModel.loadData(userId)

        // Cargar notas clínicas persistentes
        runCatching {
            val notesDocRef = firestoreDb.collection("users")
                .document(userId)
                .collection("dailyRecords")
                .document("CLINICAL_NOTES")

            val document = notesDocRef.get().await()
            if (document.exists()) {
                clinicalNotes = document.getString("notes") ?: ""
            }
        }.onFailure { e ->
            android.util.Log.e("AnalysisScreen", "Error loading clinical notes", e)
        }
    }

// Lógica para guardar las notas clínicas
    val onSaveClinicalNotes: () -> Unit = {
        if (userId == null) {
            Toast.makeText(context, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        } else {
            val notesText = currentNotesState.value.trim()
            if (notesText.isEmpty()) {
                Toast.makeText(context, "Las notas están vacías, no se guardaron.", Toast.LENGTH_SHORT).show()
            } else {
                val notesDocRef = firestoreDb.collection("users")
                    .document(userId)
                    .collection("dailyRecords")
                    .document("CLINICAL_NOTES")

                val data = mapOf(
                    "notes" to notesText,
                    "lastUpdated" to Date()
                )

                notesDocRef.set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Observaciones clínicas guardadas.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error al guardar notas: ${e.message}", Toast.LENGTH_LONG).show()
                        android.util.Log.e("AnalysisScreen", "Error saving clinical notes", e)
                    }
            }
        }
    }

    val analysisPeriods = remember {
        listOf(
            "Últimos 7 días" to 7,
            "Últimos 14 días" to 14,
            "Últimos 30 días" to 30,
            "Últimos 3 Meses" to 90,
            "Últimos 6 Meses" to 180,
            "Todo el Historial" to Int.MAX_VALUE
        )
    }

    var selectedPeriodText by remember(selectedPeriodDays) {
        mutableStateOf(analysisPeriods.find { it.second == selectedPeriodDays }?.first ?: "Últimos 30 días")
    }
    var periodDropdownExpanded by remember { mutableStateOf(false) }

    val filteredDailyRecords: List<DailyRecordData> by viewModel.filteredDailyRecords.collectAsState()
    val filteredMealRecords: List<MealRecordData> by viewModel.filteredMealRecords.collectAsState()
    val filteredDosageRecords: List<DosageTakenRecord> by viewModel.filteredDosageRecords.collectAsState()

    // Bloques de cálculo de resúmenes
    val recentNotes = remember(filteredDailyRecords) {
        filteredDailyRecords
            .mapNotNull { record: DailyRecordData ->
                record.notes?.takeIf { note -> note.isNotBlank() }?.let { note ->
                    val date = record.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM"))
                    val time = record.date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    "$date ($time): $note"
                }
            }
            .takeLast(5)
            .joinToString(separator = "\n- ")
            .let { result -> if (result.isNotBlank()) "- $result" else "Ninguna nota reciente registrada en este período." }
    }

    val symptomFrequency = remember(filteredDailyRecords) {
        val allSymptoms: List<String> = filteredDailyRecords
            .flatMap { record: DailyRecordData -> record.symptoms }

        val grouped: Map<String, Int> = allSymptoms
            .groupingBy { symptom: String -> symptom }
            .eachCount()

        val topSymptoms: List<String> = grouped.entries
            .sortedByDescending { entry: Map.Entry<String, Int> -> entry.value }
            .take(5)
            .map { entry: Map.Entry<String, Int> -> "${entry.key} (${entry.value} veces)" }

        if (topSymptoms.isNotEmpty()) "- ${topSymptoms.joinToString("\n- ")}"
        else "Ningún síntoma registrado en este período."
    }

    val glucoseStatsForPeriod: GlucoseStats = remember(filteredDailyRecords) {
        val glucoseValues: List<Float> = filteredDailyRecords
            .flatMap { record: DailyRecordData -> record.glucoseReadings }
            .map { reading: GlucoseReading -> reading.value.toFloat() }

        if (glucoseValues.isNotEmpty()) {
            val average: Float = glucoseValues.average().toFloat()
            val minVal: Float = glucoseValues.minOrNull() ?: 0f
            val maxVal: Float = glucoseValues.maxOrNull() ?: 0f
            val inRangeCount: Int = glucoseValues.count { value: Float -> value in GLUCOSE_NORMAL_MIN_AN..GLUCOSE_NORMAL_MAX_AN }
            val highCount: Int = glucoseValues.count { value: Float -> value > GLUCOSE_NORMAL_MAX_AN }
            val lowCount: Int = glucoseValues.count { value: Float -> value < GLUCOSE_NORMAL_MIN_AN }
            val severeLowCount: Int = glucoseValues.count { value: Float -> value < GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN }
            val severeHighCount: Int = glucoseValues.count { value: Float -> value > GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN }
            val totalReadings: Int = glucoseValues.size
            val tir: Float = (inRangeCount * 100f / totalReadings)
            val tar: Float = (highCount * 100f / totalReadings)
            val tbr: Float = (lowCount * 100f / totalReadings)
            val sumOfSquares: Float = glucoseValues
                .map { value: Float -> (value - average).let { diff -> diff * diff } }
                .sum()
            val stdDev: Float = if (totalReadings > 1) kotlin.math.sqrt(sumOfSquares / (totalReadings - 1)) else 0f
            val cv: Float = if (average > 0f) (stdDev / average) * 100f else 0f

            GlucoseStats(
                average, minVal, maxVal, inRangeCount, highCount, lowCount, totalReadings,
                stdDev, tir, tar, tbr, cv, severeLowCount, severeHighCount
            )
        } else {
            GlucoseStats(0f, 0f, 0f, 0, 0, 0, 0, 0f, 0f, 0f, 0f, 0f, 0, 0)
        }
    }

    val activitySummary = remember(filteredDailyRecords) {
        val totalActivityMinutes: Int = filteredDailyRecords
            .mapNotNull { record: DailyRecordData ->
                record.activityTime?.let { timeStr: String ->
                    var minutes = 0
                    Regex("(\\d+)h").find(timeStr)?.groupValues?.get(1)?.toIntOrNull()?.let { h -> minutes += h * 60 }
                    Regex("(\\d+)m").find(timeStr)?.groupValues?.get(1)?.toIntOrNull()?.let { m -> minutes += m }
                    minutes
                }
            }.sum()

        val uniqueActivities: String = filteredDailyRecords
            .flatMap { record: DailyRecordData -> record.activities }
            .distinct()
            .joinToString()

        ActivitySummary(totalActivityMinutes, uniqueActivities)
    }

    val dietaryHabitsSummary = remember(filteredDailyRecords, filteredMealRecords) {
        val foodTypeFrequency: String = filteredDailyRecords
            .flatMap { record: DailyRecordData -> record.foodTypes }
            .groupingBy { foodType: String -> foodType }
            .eachCount()
            .entries
            .sortedByDescending { entry -> entry.value }
            .take(3)
            .map { entry -> "${entry.key} (${entry.value} veces)" }
            .joinToString(separator = "\n- ")
            .let { result -> if (result.isNotBlank()) "- $result" else "Ningún tipo de alimento registrado en este período." }

        val mealTypeCounts: Map<MealType, Int> = filteredMealRecords
            .groupingBy { meal: MealRecordData -> meal.mealType }
            .eachCount()

        val totalCaloriesRecorded: Int = filteredMealRecords
            .sumOf { meal: MealRecordData -> meal.actualCalories }

        DietaryHabitsSummary(foodTypeFrequency, mealTypeCounts, totalCaloriesRecorded)
    }

    val medicationAdherenceSummary = remember(medications, filteredDosageRecords, selectedPeriodDays) {
        val dosesTakenCount: Int = filteredDosageRecords.size

        val totalExpectedDoses: Int = medications.map { medication: MedicationData ->
            val expectedTimes: List<Pair<String, String>> = calculateExpectedDoseTimes(medication)

            val totalDays: Int = if (selectedPeriodDays == Int.MAX_VALUE) {
                val firstRecordDate: Long? = filteredDailyRecords.minOfOrNull { record: DailyRecordData -> record.date.time }
                if (firstRecordDate != null) {
                    val firstDate = Instant.ofEpochMilli(firstRecordDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    (ChronoUnit.DAYS.between(firstDate, LocalDate.now()) + 1).toInt()
                } else 0
            } else selectedPeriodDays

            expectedTimes.size * totalDays
        }.sum()


        val adherencePercentage: String = if (totalExpectedDoses > 0) {
            String.format(Locale.US, "%.1f", (dosesTakenCount.toFloat() * 100f / totalExpectedDoses.toFloat())) + " %"
        } else if (medications.isNotEmpty()) {
            "0.0 %"
        } else {
            "N/A"
        }

        val note: String = if (medications.isEmpty()) {
            "No hay medicamentos registrados en tu perfil. No se puede calcular la adherencia."
        } else {
            "El porcentaje de adherencia es una estimación basada en $dosesTakenCount dosis registradas de $totalExpectedDoses esperadas."
        }

        MedicationAdherenceSummary(adherencePercentage, note, medications)
    }

    val createAndSharePdf: () -> Unit = {
        try {
            // Se usa la clase PdfGenerator definida más abajo
            val pdfGenerator = PdfGenerator(context)
            val fileUri = pdfGenerator.createPdf(
                userProfile,
                medications,
                glucoseStatsForPeriod,
                filteredDailyRecords,
                activitySummary,
                dietaryHabitsSummary,
                medicationAdherenceSummary,
                selectedPeriodText,
                selectedPeriodDays,
                clinicalNotes // notas clínicas persistentes
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "Informe Médico de Diabetes")
                putExtra(Intent.EXTRA_TEXT, "Adjunto el informe médico de diabetes generado en la aplicación.")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir informe médico"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar el PDF. Asegúrate de que PdfGenerator esté implementado y las clases de datos sean accesibles.", Toast.LENGTH_LONG).show()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Análisis Clínico",
                        color = colorResource(id = R.color.primaryText),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colorResource(id = R.color.primaryText)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = colorResource(id = R.color.primaryText),
                    titleContentColor = colorResource(id = R.color.primaryText),
                    actionIconContentColor = colorResource(id = R.color.primaryText)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // altura estándar pero ligera
                windowInsets = WindowInsets.statusBars,
            )
        },
        containerColor = colorResource(id = R.color.appBackground),
    ){ innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightGrayBackground)
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Pestañas de Análisis
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalysisTab(title = "Paciente", isSelected = selectedAnalysisTab == AnalysisTabType.PATIENT) { selectedAnalysisTab = AnalysisTabType.PATIENT }
                AnalysisTab(title = "Glucometría", isSelected = selectedAnalysisTab == AnalysisTabType.GLUCOSE) { selectedAnalysisTab = AnalysisTabType.GLUCOSE }
                AnalysisTab(title = "Actividad", isSelected = selectedAnalysisTab == AnalysisTabType.ACTIVITY) { selectedAnalysisTab = AnalysisTabType.ACTIVITY }
                AnalysisTab(title = "Dieta", isSelected = selectedAnalysisTab == AnalysisTabType.DIET) { selectedAnalysisTab = AnalysisTabType.DIET }
                AnalysisTab(title = "Síntomas", isSelected = selectedAnalysisTab == AnalysisTabType.SYMPTOMS) { selectedAnalysisTab = AnalysisTabType.SYMPTOMS }
            }

            // Selector de Período
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = periodDropdownExpanded,
                    onExpandedChange = { periodDropdownExpanded = !periodDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().menuAnchor().clickable { periodDropdownExpanded = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Período de Análisis: $selectedPeriodText", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodDropdownExpanded)
                    }

                    ExposedDropdownMenu(
                        expanded = periodDropdownExpanded,
                        onDismissRequest = { periodDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        analysisPeriods.forEach { (periodText, days) ->
                            DropdownMenuItem(
                                text = { Text(periodText) },
                                onClick = {
                                    selectedPeriodText = periodText
                                    viewModel.updateSelectedPeriodDays(days)
                                    periodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Crossfade(targetState = selectedAnalysisTab, label = "AnalysisTabTransition") { tab ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when (tab) {
                        AnalysisTabType.PATIENT -> {
                            ReportCard(title = "Datos del Paciente y Tratamiento", icon = Icons.Filled.Person) {
                                PatientInfoSection(userProfile = userProfile, medications = medications)
                                Spacer(modifier = Modifier.height(16.dp))
                                MedicationAdherenceSummarySection(medicationAdherenceSummary = medicationAdherenceSummary)
                            }
                        }
                        AnalysisTabType.GLUCOSE -> {
                            ReportCard(title = "Métricas de Glucosa ($selectedPeriodText)", icon = Icons.Filled.MonitorHeart) {
                                GlucoseMetricsOverview(glucoseStatsForPeriod = glucoseStatsForPeriod)
                                Spacer(modifier = Modifier.height(16.dp))
                                GlucoseAverageSection(filteredDailyRecords = filteredDailyRecords, selectedPeriodText = selectedPeriodText)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Gráfico de Tendencia de Glucosa - Promedio Diario", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.padding(bottom = 8.dp))
                                GlucoseLineChart(records = filteredDailyRecords, daysInPeriod = selectedPeriodDays)
                                Spacer(modifier = Modifier.height(16.dp))
                                GlucoseSummaryTable(recordsForPeriod = filteredDailyRecords)
                            }
                        }
                        AnalysisTabType.ACTIVITY -> {
                            ReportCard(title = "Resumen de Actividad Física", icon = Icons.Filled.DirectionsRun) {
                                ActivitySummarySection(activitySummary = activitySummary)
                            }
                        }
                        AnalysisTabType.DIET -> {
                            ReportCard(title = "Resumen de Hábitos Alimenticios", icon = Icons.Filled.RestaurantMenu) {
                                DietaryHabitsSummarySection(dietaryHabitsSummary = dietaryHabitsSummary)
                            }
                        }
                        AnalysisTabType.SYMPTOMS -> {
                            ReportCard(title = "Notas y Síntomas", icon = Icons.Filled.Sick) {
                                NotesAndSymptomsSection(recentNotes = recentNotes, symptomFrequency = symptomFrequency)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nueva sección de Observaciones Clínicas (Editable) - MEJORA CLAVE
            ClinicalNotesCard(
                notes = clinicalNotes,
                onNotesChange = { clinicalNotes = it },
                onSaveNotes = onSaveClinicalNotes
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Exportar
            Button(
                onClick = createAndSharePdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Description, "Exportar", tint = CardBackground, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Informe Médico", color = CardBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// NUEVO COMPONENTE: Tarjeta para ingresar y guardar observaciones clínicas
@Composable
fun ClinicalNotesCard(notes: String, onNotesChange: (String) -> Unit, onSaveNotes: () -> Unit) {
    ReportCard(title = "Observaciones Clínicas", icon = Icons.Filled.FormatAlignLeft) {
        Text(
            "Este campo permite al usuario o profesional de la salud agregar notas y análisis persistentes para el informe.",
            fontSize = 14.sp,
            color = TextLight,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Escriba sus observaciones clínicas...", color = TextLight) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            maxLines = 8,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = TextLight.copy(alpha = 0.5f),
                focusedLabelColor = PrimaryBlue,
                unfocusedLabelColor = TextLight,
                focusedTextColor = TextDark,
                unfocusedTextColor = TextDark,
                cursorColor = PrimaryBlue
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = onSaveNotes,
                modifier = Modifier
                    .width(160.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Guardar Notas", color = CardBackground, fontSize = 14.sp)
            }
        }
    }
}
// FUNCIONES AUXILIARES
@Composable
fun AnalysisTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = title,
            color = if (isSelected) PrimaryBlue else TextLight,
            fontSize = 14.sp, // Tamaño ajustado para 5 pestañas
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(PrimaryBlue, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun ReportCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

fun calculateExpectedDoseTimes(medication: MedicationData): List<Pair<String, String>> {
    val parsedTime = try { LocalTime.parse(medication.time, DateTimeFormatter.ofPattern("HH:mm")) } catch (e: Exception) { LocalTime.MIN }
    return when (medication.frequency) {
        "Una vez al día" -> listOf(medication.time to "Dosis única")
        "Dos veces al día" -> listOf(
            medication.time to "Mañana",
            parsedTime.plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm")) to "Noche"
        )
        "Tres veces al día" -> listOf(
            parsedTime.format(DateTimeFormatter.ofPattern("HH:mm")) to "Mañana",
            parsedTime.plusHours(8).format(DateTimeFormatter.ofPattern("HH:mm")) to "Tarde",
            parsedTime.plusHours(16).format(DateTimeFormatter.ofPattern("HH:mm")) to "Noche"
        )
        "Cada 6 hr" -> listOf(
            parsedTime.format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 1",
            parsedTime.plusHours(6).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 2",
            parsedTime.plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 3",
            parsedTime.plusHours(18).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 4"
        )
        "Cada 8 hr" -> listOf(
            parsedTime.format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 1",
            parsedTime.plusHours(8).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 2",
            parsedTime.plusHours(16).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 3"
        )
        "Cada 12 hr" -> listOf(
            parsedTime.format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 1",
            parsedTime.plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 2"
        )
        "Cada 24 hr" -> listOf(medication.time to "Dosis única")
        else -> emptyList()
    }
}

@Composable
fun PatientInfoSection(userProfile: UserProfile, medications: List<MedicationData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = "Datos Demográficos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue, modifier = Modifier.padding(bottom = 8.dp))
        TextInfoRow("Paciente:", userProfile.name)
        TextInfoRow("Edad:", "${userProfile.age} años")
        TextInfoRow("Sexo:", userProfile.gender)
        TextInfoRow("IMC:", "${String.format(Locale.US, "%.2f", userProfile.bmi)} (${userProfile.bmiCategory})")

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Información Clínica", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue, modifier = Modifier.padding(bottom = 8.dp))
        TextInfoRow("Diagnóstico Principal:", userProfile.condition)
        val diabetesType = userProfile.diabetesType
        if (userProfile.condition == "Diabetes" && diabetesType != null) {
            TextInfoRow("Tipo de Diabetes:", diabetesType)
        }
        userProfile.diagnosisDate?.let {
            TextInfoRow("Fecha Diagnóstico:", it.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun TextInfoRow(label: String, value: String, valueColor: Color = TextDark, fontWeight: FontWeight = FontWeight.Normal) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextDark,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = fontWeight,
            modifier = Modifier.weight(0.6f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun NotesAndSymptomsSection(recentNotes: String, symptomFrequency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Síntomas más frecuentes:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = symptomFrequency,
            fontSize = 14.sp,
            color = TextDark,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
        )

        Text(
            text = "Notas y observaciones recientes (últimas 5):",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = recentNotes,
            fontSize = 14.sp,
            color = TextDark,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun GlucoseMetricsOverview(glucoseStatsForPeriod: GlucoseStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Métricas Clave de Glucosa:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (glucoseStatsForPeriod.totalReadings == 0) {
            Text("No hay lecturas de glucosa para el período seleccionado.", fontSize = 14.sp, color = TextLight)
            return
        }

        val decimalFormat = DecimalFormat("#.##")
        val average = decimalFormat.format(glucoseStatsForPeriod.average)
        val sd = decimalFormat.format(glucoseStatsForPeriod.stdDev)
        val cv = decimalFormat.format(glucoseStatsForPeriod.cv)

        MetricRow("Promedio (mg/dL):", average, TextDark, FontWeight.Bold)
        MetricRow("Desviación Estándar (SD):", sd, TextDark)
        MetricRow("Coeficiente de Variación (CV):", "$cv %", when {
            glucoseStatsForPeriod.cv < 36f -> GlucoseNormalColor
            glucoseStatsForPeriod.cv < 50f -> GlucoseLowColor
            else -> GlucoseHighColor
        }, FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tiempo en Rango (%):",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        MetricRow("TIR (${GLUCOSE_NORMAL_MIN_AN}-${GLUCOSE_NORMAL_MAX_AN}):", "${decimalFormat.format(glucoseStatsForPeriod.tir)} %", GlucoseNormalColor, FontWeight.Bold)
        MetricRow("TAR (>${GLUCOSE_NORMAL_MAX_AN}):", "${decimalFormat.format(glucoseStatsForPeriod.tar)} %", GlucoseHighColor)
        MetricRow("TBR (<${GLUCOSE_NORMAL_MIN_AN}):", "${decimalFormat.format(glucoseStatsForPeriod.tbr)} %", GlucoseLowColor)

        Spacer(modifier = Modifier.height(8.dp))

        MetricRow("Hipoglucemia Severa (<${GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN}):", glucoseStatsForPeriod.severeLowCount.toString(), GlucoseLowColor, FontWeight.Bold)
        MetricRow("Hiperglucemia Severa (>${GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN}):", glucoseStatsForPeriod.severeHighCount.toString(), GlucoseHighColor, FontWeight.Bold)
    }
}

@Composable
fun MetricRow(label: String, value: String, valueColor: Color = TextDark, fontWeight: FontWeight = FontWeight.Normal) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 14.sp, color = TextDark)
        Text(text = value, fontSize = 14.sp, color = valueColor, fontWeight = fontWeight)
    }
}

@Composable
fun GlucoseAverageSection(filteredDailyRecords: List<DailyRecordData>, selectedPeriodText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Promedios de Glucosa por Período:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val allGlucoseValues = filteredDailyRecords
            .flatMap { it.glucoseReadings }
            .map { it.value.toFloat() }
            .filter { it > 0 }

        if (allGlucoseValues.isEmpty()) {
            Text("No hay lecturas de glucosa válidas.", fontSize = 14.sp, color = TextLight)
            return
        }
        val decimalFormat = DecimalFormat("#.##")
        val today = LocalDate.now()

        fun filterReadings(records: List<DailyRecordData>, startDate: LocalDate): List<Float> {
            return records
                .filter { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() >= startDate }
                .flatMap { it.glucoseReadings }
                .map { it.value.toFloat() }
        }

        val weeklyRecords = filterReadings(filteredDailyRecords, today.minusWeeks(1).plusDays(1))
        if (weeklyRecords.isNotEmpty()) {
            TextInfoRow(label = "Promedio Semanal (Últimos 7 días):", value = "${decimalFormat.format(weeklyRecords.average())} mg/dL")
        }

        val monthlyRecords = filterReadings(filteredDailyRecords, today.withDayOfMonth(1))
        if (monthlyRecords.isNotEmpty()) {
            TextInfoRow(label = "Promedio Mensual (Mes Actual):", value = "${decimalFormat.format(monthlyRecords.average())} mg/dL")
        }

        // Promedio General del Período Seleccionado
        TextInfoRow(
            label = "Promedio General ($selectedPeriodText):",
            value = "${decimalFormat.format(allGlucoseValues.average())} mg/dL"
        )
    }
}

@Composable
fun ActivitySummarySection(activitySummary: ActivitySummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Resumen de Minutos de Ejercicio:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (activitySummary.totalMinutes == 0) {
            Text("No hay actividad registrada en este período.", fontSize = 14.sp, color = TextLight)
            return
        }

        val hours = activitySummary.totalMinutes / 60
        val minutes = activitySummary.totalMinutes % 60
        val totalTime = StringBuilder().apply {
            if (hours > 0) android.util.Log.e("ActivitySummary", "${hours}h ")
            append("${minutes}m")
        }.toString()

        TextInfoRow(label = "Total de Minutos Activos:", value = totalTime)
        TextInfoRow(label = "Tipos de Actividad Registrados:", value = activitySummary.uniqueActivities.ifEmpty { "Ninguno" })
    }
}

@Composable
fun DietaryHabitsSummarySection(dietaryHabitsSummary: DietaryHabitsSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Resumen de Ingreso de Alimentos:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextInfoRow(label = "Calorías Totales Registradas:", value = "${dietaryHabitsSummary.totalCaloriesRecorded} Cal")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tipos de alimento frecuentes:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextDark
        )
        Text(
            text = dietaryHabitsSummary.foodTypeFrequency.ifEmpty { "- Ninguno" },
            fontSize = 14.sp,
            color = TextDark,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Text(
            text = "Distribución de Comidas:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextDark
        )
        if (dietaryHabitsSummary.mealTypeCounts.isEmpty()) {
            Text("- Ningún registro de comidas principales.", fontSize = 14.sp, color = TextLight, modifier = Modifier.padding(start = 8.dp))
        } else {
            dietaryHabitsSummary.mealTypeCounts.forEach { (mealType, count) ->
                val mealName = mealType.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                Text(
                    text = "- ${mealName}: $count veces",
                    fontSize = 14.sp,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MedicationAdherenceSummarySection(medicationAdherenceSummary: MedicationAdherenceSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Adherencia a la Medicación:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = PrimaryBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextInfoRow(
            label = "Porcentaje de Adherencia (Estimado):",
            value = medicationAdherenceSummary.adherencePercentage,
            valueColor = PrimaryBlue,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = medicationAdherenceSummary.note,
            fontSize = 12.sp,
            color = TextLight,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (medicationAdherenceSummary.medications.isNotEmpty()) {
            Text(
                text = "Medicamentos Registrados en Perfil:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            medicationAdherenceSummary.medications.forEach { medication ->
                Text(
                    text = "- ${medication.name} (${medication.dose} ${medication.unit})",
                    fontSize = 13.sp,
                    color = TextDark,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                )
            }
        } else {
            Text(
                text = "No hay medicamentos registrados en tu perfil para analizar.",
                fontSize = 13.sp,
                color = TextLight,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun GlucoseSummaryTable(recordsForPeriod: List<DailyRecordData>) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.dp, LightGrayBackground, RoundedCornerShape(8.dp))) {
        Text(
            text = "Detalle Diario de Glucosa (Máximo Registrado)",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark,
            modifier = Modifier.padding(12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue.copy(alpha = 0.1f))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("Fecha", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue, modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)
            Text("Glucosa (Máx)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
            Text("Estado", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
        }

        val recordsGroupedByDay = recordsForPeriod
            .filter { it.glucoseReadings.isNotEmpty() }
            .groupBy { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { entry ->
                entry.value.flatMap { it.glucoseReadings }.maxOfOrNull { it.value } ?: 0
            }

        if (recordsGroupedByDay.isEmpty()) {
            Text(
                text = "No hay registros de glucosa para este período.",
                fontSize = 14.sp,
                color = TextLight,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        } else {
            val formatter = DateTimeFormatter.ofPattern("dd/MM (EEE)", Locale("es", "ES"))

            recordsGroupedByDay.entries.sortedBy { it.key }.forEach { (recordLocalDate, glucoseValueInt) ->
                val glucoseValue = glucoseValueInt.toString()
                val glucoseFloat = glucoseValueInt.toFloat()

                val status = when {
                    glucoseFloat >= GLUCOSE_NORMAL_MIN_AN && glucoseFloat <= GLUCOSE_NORMAL_MAX_AN -> "Normal"
                    glucoseFloat > GLUCOSE_NORMAL_MAX_AN -> "Elevada"
                    glucoseFloat < GLUCOSE_NORMAL_MIN_AN -> "Baja"
                    else -> "N/A"
                }
                val statusColor = when (status) {
                    "Normal" -> GlucoseNormalColor
                    "Elevada" -> GlucoseHighColor
                    "Baja" -> GlucoseLowColor
                    else -> TextLight
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(if (recordLocalDate == LocalDate.now()) LightGrayBackground.copy(alpha = 0.5f) else Color.Transparent),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(recordLocalDate.format(formatter), fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)
                    Text(glucoseValue, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
                    Text(status, fontSize = 12.sp, color = statusColor, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun GlucoseLineChart(records: List<DailyRecordData>, daysInPeriod: Int) {
    // Expandir todas las lecturas de glucosa (no promediar)
    val validGlucoseRecords = remember(records) {
        records
            .filter { it.glucoseReadings.isNotEmpty() }
            .flatMap { record ->
                record.glucoseReadings.map { reading ->
                    Pair(record.date, reading.value.toFloat())
                }
            }
            .sortedBy { it.first.time }
    }

    if (validGlucoseRecords.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(LightGrayBackground, RoundedCornerShape(8.dp))
                .border(1.dp, TextLight.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay datos de glucosa para el gráfico.", color = TextLight)
        }
        return
    }

    val today = LocalDate.now()
    val earliestDateInRecords = validGlucoseRecords.first().first.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val latestDateInRecords = validGlucoseRecords.last().first.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    val displayStartDate = if (daysInPeriod == Int.MAX_VALUE) {
        earliestDateInRecords
    } else {
        maxOf(
            today.minusDays(daysInPeriod.toLong() - 1),
            earliestDateInRecords
        )
    }
    val displayEndDate = latestDateInRecords

    val totalDaysToDisplay = ChronoUnit.DAYS.between(displayStartDate, displayEndDate).toInt() + 1
    val paddingHorizontal = 32.dp
    val paddingVertical = 20.dp

    val minGlucoseRaw = validGlucoseRecords.minOfOrNull { it.second } ?: GLUCOSE_NORMAL_MIN_AN
    val maxGlucoseRaw = validGlucoseRecords.maxOfOrNull { it.second } ?: GLUCOSE_HYPERGLYCEMIA_THRESHOLD_AN

    val yMinChart = min(GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN - 20f, minGlucoseRaw - 20f).coerceAtLeast(0f)
    val yMaxChart = max(GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN + 20f, maxGlucoseRaw + 20f)
    val yRange = yMaxChart - yMinChart

    // Convertir lecturas a puntos relativos en el eje X
    val chartPoints = remember(validGlucoseRecords, displayStartDate) {
        validGlucoseRecords.mapNotNull { (recordDate, value) ->
            val recordLocalDate = recordDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            if (!recordLocalDate.isBefore(displayStartDate) && !recordLocalDate.isAfter(displayEndDate)) {
                val dayRelative = ChronoUnit.DAYS.between(displayStartDate, recordLocalDate).toInt()
                Pair(dayRelative, value)
            } else null
        }.sortedBy { it.first }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, TextLight.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = paddingHorizontal, end = 0.dp, top = 0.dp, bottom = paddingVertical)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val yValueToPx = if (yRange > 0) canvasHeight / yRange else 0f

            val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val strokeWidth = 2f

            fun DrawScope.drawHorizontalReferenceLine(value: Float, color: Color) {
                if (value >= yMinChart && value <= yMaxChart) {
                    val yPx = canvasHeight - ((value - yMinChart) * yValueToPx)
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(0f, yPx),
                        end = Offset(canvasWidth, yPx),
                        strokeWidth = strokeWidth,
                        pathEffect = dashPath
                    )
                }
            }

            // Líneas de referencia
            drawHorizontalReferenceLine(GLUCOSE_NORMAL_MIN_AN, GlucoseNormalColor)
            drawHorizontalReferenceLine(GLUCOSE_NORMAL_MAX_AN, GlucoseNormalColor)
            drawHorizontalReferenceLine(GLUCOSE_HYPERGLYCEMIA_THRESHOLD_AN, GlucoseHighColor)
            drawHorizontalReferenceLine(GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN, GlucoseLowColor)

            // Distribuir puntos en el eje X
            val xStepPx = if (totalDaysToDisplay > 1) canvasWidth / (totalDaysToDisplay - 1) else canvasWidth
            val mappedChartPoints = chartPoints.mapNotNull { (dayRelative, glucoseValue) ->
                val xPx = dayRelative * xStepPx
                val yPx = canvasHeight - ((glucoseValue - yMinChart) * yValueToPx)
                if (yPx.isFinite()) Offset(xPx, yPx) else null
            }

            // Dibujar líneas entre puntos
            if (mappedChartPoints.size >= 2) {
                for (i in 0 until mappedChartPoints.size - 1) {
                    drawLine(
                        color = PrimaryBlue,
                        start = mappedChartPoints[i],
                        end = mappedChartPoints[i + 1],
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Dibujar puntos
            mappedChartPoints.forEach { point ->
                drawCircle(
                    color = PrimaryBlue,
                    radius = 8f,
                    center = point
                )
            }
        }

        // Eje Y
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(paddingHorizontal)
                .align(Alignment.CenterStart)
                .padding(bottom = paddingVertical),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val yAxisStep = (yMaxChart - yMinChart) / 4f
            (0..4).reversed().forEach { i ->
                val labelValue = yMinChart + (i * yAxisStep)
                Text(String.format(Locale.US, "%.0f", labelValue), fontSize = 10.sp, color = TextLight, textAlign = TextAlign.End)
            }
        }

        // Eje X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = paddingHorizontal, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            val totalDays = ChronoUnit.DAYS.between(displayStartDate, displayEndDate).toInt() + 1
            val xAxisLabelsCount = min(totalDays, 7)
            val labelInterval = max(1, totalDays / xAxisLabelsCount)

            val effectiveLabels = (0 until totalDays).filter { it % labelInterval == 0 }
            effectiveLabels.forEach { dayOffset ->
                val labelDate = displayStartDate.plusDays(dayOffset.toLong())
                val formatter = when {
                    totalDaysToDisplay <= 7 -> DateTimeFormatter.ofPattern("EE", Locale("es", "ES"))
                    totalDaysToDisplay <= 31 -> DateTimeFormatter.ofPattern("dd/MM")
                    else -> DateTimeFormatter.ofPattern("dd/MM/yy")
                }
                Text(
                    text = labelDate.format(formatter),
                    fontSize = 10.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Leyenda
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimaryBlue)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Glucosa", fontSize = 12.sp, color = TextDark)

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlucoseNormalColor.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rango Normal", fontSize = 12.sp, color = TextDark)

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlucoseHighColor.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Umbral Alto", fontSize = 12.sp, color = TextDark)

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlucoseLowColor.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Umbral Bajo", fontSize = 12.sp, color = TextDark)
        }
    }
}



fun generateMedicalReportText(
    userProfile: UserProfile,
    medications: List<MedicationData>,
    glucoseStats: GlucoseStats,
    recordsForPeriod: List<DailyRecordData>,
    recentNotes: String,
    symptomFrequency: String,
    activitySummary: ActivitySummary,
    dietaryHabitsSummary: DietaryHabitsSummary,
    medicationAdherenceSummary: MedicationAdherenceSummary,
    selectedPeriodText: String,
    clinicalNotes: String // NUEVO PARÁMETRO
): String {
    val builder = StringBuilder()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    builder.append("--- INFORME CLÍNICO DE DIABETES ---\n\n")
    builder.append("Fecha de Generación: ${LocalDate.now().format(formatter)}\n")
    builder.append("Período del Informe: $selectedPeriodText\n\n")

    builder.append(">>> DATOS DEL PACIENTE <<<\n")
    builder.append("---------------------------------\n")
    builder.append("Nombre Completo: ${userProfile.name}\n")
    builder.append("Edad: ${userProfile.age} años\n")
    val fechaNacimiento = userProfile.dateOfBirth
        ?.toInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.toLocalDate()
        ?.format(formatter) ?: "No registrado"

    builder.append("Fecha de Nacimiento: $fechaNacimiento\n")
    builder.append("Género: ${userProfile.gender}\n")
    builder.append("Diagnóstico Principal: ${userProfile.condition}\n")
    val diabetesType = userProfile.diabetesType
    if (userProfile.condition == "Diabetes" && diabetesType != null) {
        builder.append("Tipo de Diabetes: ${diabetesType}\n")
        userProfile.diagnosisDate?.let {
            builder.append("Fecha de Diagnóstico: ${it.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)}\n")
        }
    }
    builder.append("IMC: ${String.format(Locale.US, "%.2f", userProfile.bmi)} (${userProfile.bmiCategory})\n")
    builder.append("Teléfono: ${userProfile.phone.ifEmpty { "No registrado" }}\n\n")

    builder.append(">>> TRATAMIENTO FARMACOLÓGICO ACTUAL <<<\n")
    builder.append("-------------------------------------------\n")
    if (medications.isNotEmpty()) {
        medications.forEachIndexed { index, medication ->
            builder.append("${index + 1}. ${medication.name} ${medication.dose} ${medication.unit} (Horario: ${medication.time}, Frecuencia: ${medication.frequency})\n")
        }
    } else {
        builder.append("Ningún tratamiento registrado.\n")
    }
    builder.append("\n")

    builder.append(">>> MÉTRICAS CLAVE DE GLUCEMIA ($selectedPeriodText) <<<\n")
    builder.append("-----------------------------------------------------------\n")

    val allGlucoseValues = recordsForPeriod.flatMap { it.glucoseReadings }.map { it.value.toFloat() }
    val currentGlucoseStats = if (allGlucoseValues.isNotEmpty()) {
        val average = allGlucoseValues.average().toFloat()
        val minVal = allGlucoseValues.minOrNull() ?: 0f
        val maxVal = allGlucoseValues.maxOrNull() ?: 0f
        val inRangeCount = allGlucoseValues.count { value -> value in GLUCOSE_NORMAL_MIN_AN..GLUCOSE_NORMAL_MAX_AN }
        val highCount = allGlucoseValues.count { value -> value > GLUCOSE_NORMAL_MAX_AN }
        val lowCount = allGlucoseValues.count { value -> value < GLUCOSE_NORMAL_MIN_AN }
        val severeLowCount = allGlucoseValues.count { value -> value < GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN }
        val severeHighCount = allGlucoseValues.count { value -> value > GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN }
        val totalReadings = allGlucoseValues.size
        val tir = (inRangeCount * 100f / totalReadings)
        val tar = (highCount * 100f / totalReadings)
        val tbr = (lowCount * 100f / totalReadings)
        val sumOfSquares = allGlucoseValues.map { value -> (value - average).let { diff -> diff * diff } }.sum()
        val stdDev = if (totalReadings > 1) kotlin.math.sqrt(sumOfSquares / (totalReadings - 1)) else 0f
        val cv = if (average > 0f) (stdDev / average) * 100f else 0f
        GlucoseStats(
            average, minVal, maxVal, inRangeCount, highCount, lowCount, totalReadings,
            stdDev, tir, tar, tbr, cv, severeLowCount, severeHighCount
        )
    } else {
        glucoseStats
    }

    if (currentGlucoseStats.totalReadings == 0) {
        builder.append("No hay registros de glucosa disponibles para este período.\n")
    } else {
        builder.append("Total de Lecturas: ${currentGlucoseStats.totalReadings}\n")
        builder.append("Promedio de Glucosa: ${String.format(Locale.US, "%.1f", currentGlucoseStats.average)} mg/dL\n")
        builder.append("Lectura Mínima: ${String.format(Locale.US, "%.0f", currentGlucoseStats.min)} mg/dL\n")
        builder.append("Lectura Máxima: ${String.format(Locale.US, "%.0f", currentGlucoseStats.max)} mg/dL\n")
        builder.append("Desviación Estándar (Variabilidad): ${String.format(Locale.US, "%.1f", currentGlucoseStats.stdDev)} mg/dL\n")
        builder.append("Coeficiente de Variación (CV): ${String.format(Locale.US, "%.1f", currentGlucoseStats.cv)} % (Objetivo <36%)\n")
        builder.append("Tiempo en Rango (TIR) [${GLUCOSE_NORMAL_MIN_AN}-${GLUCOSE_NORMAL_MAX_AN} mg/dL]: ${String.format(Locale.US, "%.1f", currentGlucoseStats.tir)}% (Objetivo >70%)\n")
        builder.append("Tiempo por Encima del Rango (TAR) [>${GLUCOSE_NORMAL_MAX_AN} mg/dL]: ${String.format(Locale.US, "%.1f", currentGlucoseStats.tar)}% (${currentGlucoseStats.highCount} lecturas)\n")
        builder.append("Tiempo por Debajo del Rango (TBR) [<${GLUCOSE_NORMAL_MIN_AN} mg/dL]: ${String.format(Locale.US, "%.1f", currentGlucoseStats.tbr)}% (${currentGlucoseStats.lowCount} lecturas)\n")
        builder.append("Lecturas Hipoglucémicas Severas (<${GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN} mg/dL): ${currentGlucoseStats.severeLowCount}\n")
        builder.append("Lecturas Hiperglucémicas Severas (>${GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN} mg/dL): ${currentGlucoseStats.severeHighCount}\n")
    }
    builder.append("\n")

    builder.append(">>> PROMEDIOS DE GLUCOSA <<<\n")
    builder.append("----------------------------\n")
    if (allGlucoseValues.isNotEmpty()) {
        val today = LocalDate.now()

        fun filterReadings(records: List<DailyRecordData>, startDate: LocalDate): List<Float> {
            return records
                .filter { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() >= startDate }
                .flatMap { it.glucoseReadings }
                .map { it.value.toFloat() }
        }

        val startOfWeek = today.minusWeeks(1).plusDays(1)
        val weeklyRecords = filterReadings(recordsForPeriod, startOfWeek)
        if (weeklyRecords.isNotEmpty()) {
            builder.append("Promedio Semanal: ${String.format(Locale.US, "%.2f", weeklyRecords.average())} mg/dL\n")
        }
        val startOfMonth = today.withDayOfMonth(1)
        val monthlyRecords = filterReadings(recordsForPeriod, startOfMonth)
        if (monthlyRecords.isNotEmpty()) {
            builder.append("Promedio Mensual: ${String.format(Locale.US, "%.2f", monthlyRecords.average())} mg/dL\n")
        }
        val isCustomPeriod = !listOf("Últimos 7 días", "Últimos 30 días", "Últimos 3 Meses", "Últimos 6 Meses", "Todo el Historial").contains(selectedPeriodText)
        if (isCustomPeriod) {
            builder.append("Promedio del Período Seleccionado: ${String.format(Locale.US, "%.2f", allGlucoseValues.average())} mg/dL\n")
        }
    } else {
        builder.append("No hay registros para calcular promedios.\n")
    }
    builder.append("\n")

    builder.append(">>> RESUMEN DE NOTAS Y SÍNTOMAS <<<\n")
    builder.append("-------------------------------------\n")
    builder.append("Notas y observaciones recientes:\n${recentNotes}\n")
    builder.append("Síntomas más frecuentes:\n${symptomFrequency}\n\n")

    builder.append(">>> RESUMEN DE ACTIVIDAD FÍSICA <<<\n")
    builder.append("-----------------------------------\n")
    builder.append("Total de Minutos Activos: ${activitySummary.totalMinutes} minutos\n")
    builder.append("Tipos de Actividad Registrados: ${activitySummary.uniqueActivities.ifEmpty { "Ninguno" }}\n\n")

    builder.append(">>> RESUMEN DE HÁBITOS ALIMENTICIOS <<<\n")
    builder.append("---------------------------------------\n")
    builder.append("Tipos de Alimentos Frecuentes:\n${dietaryHabitsSummary.foodTypeFrequency.ifEmpty { "- Ninguno" }}\n")
    builder.append("Registro de comidas principales:\n")
    if (dietaryHabitsSummary.mealTypeCounts.isNotEmpty()) {
        dietaryHabitsSummary.mealTypeCounts.forEach { (mealType, count) ->
            builder.append("- ${mealType.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}: $count veces\n")
        }
    } else {
        builder.append("- Ninguno\n")
    }
    builder.append("Calorías totales registradas: ${dietaryHabitsSummary.totalCaloriesRecorded} Cal\n\n")

    // OBSERVACIONES CLÍNICAS (Persistentes)
    builder.append(">>> OBSERVACIONES CLÍNICAS (PERSISTENTES) <<<\n")
    builder.append("----------------------------------------------\n")
    builder.append(clinicalNotes.ifEmpty { "No se han registrado observaciones clínicas en la aplicación." })
    builder.append("\n\n")


    builder.append(">>> ADHERENCIA A LA MEDICACIÓN <<<\n")
    builder.append("----------------------------------\n")
    builder.append("Porcentaje de Adherencia (Estimado): ${medicationAdherenceSummary.adherencePercentage}\n")
    builder.append("Nota: ${medicationAdherenceSummary.note}\n")
    if (medicationAdherenceSummary.medications.isNotEmpty()) {
        medications.forEachIndexed { index, medication ->
            builder.append("- ${index + 1}. ${medication.name} ${medication.dose} ${medication.unit} (Horario: ${medication.time}, Frecuencia: ${medication.frequency})\n")
        }
    } else {
        builder.append("No hay medicamentos registrados en el perfil.\n")
    }
    builder.append("\n")

    builder.append("--- FIN DEL INFORME CLÍNICO ---")

    return builder.toString()
}

// CLASE PDFGENERATOR
class PdfGenerator(private val context: Context) {

    private val pageHeight = 1120
    private val pageWidth = 792
    private val margin = 40f
    private val lineHeight = 18f

    // Tipografías centralizadas
    private val fontTitle = Paint().apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 24f
        color = android.graphics.Color.DKGRAY   // ✅ usa la clase de Android
    }

    private val fontSectionTitle = Paint().apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 16f
        color = PrimaryBlue.toArgb()            // ✅ convierte Compose Color a Int
    }

    private val fontText = TextPaint().apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textSize = 14f
        color = android.graphics.Color.BLACK    // ✅ usa la clase de Android
    }
    fun createPdf(
        userProfile: UserProfile,
        medications: List<MedicationData>,
        glucoseStats: GlucoseStats,
        recordsForPeriod: List<DailyRecordData>,
        activitySummary: ActivitySummary,
        dietaryHabitsSummary: DietaryHabitsSummary,
        medicationAdherenceSummary: MedicationAdherenceSummary,
        selectedPeriodText: String,
        selectedPeriodDays: Int,
        clinicalNotes: String
    ): Uri {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = margin
    // Cuando necesites una nueva página:
        pdfDocument.finishPage(page)
        page = pdfDocument.startPage(pageInfo)   // ✅ reasignación, no redeclaración
        canvas = page.canvas                     // ✅ reasignación
        yPos = margin                            // ✅ reinicia posición


    // Encabezado
        yPos = drawHeader(canvas, yPos, selectedPeriodText)

// Datos del paciente
        var ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 200f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawPatientData(canvas, yPos, userProfile)

// Tratamiento
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, medications.size * lineHeight + 100f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawMedications(canvas, yPos, medications)

// Métricas de glucosa
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 200f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawGlucoseMetrics(canvas, yPos, glucoseStats, selectedPeriodText)

// Gráfico de glucosa
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 300f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawGlucoseChart(canvas, yPos, recordsForPeriod, selectedPeriodDays)

// Adherencia
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 200f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawMedicationAdherence(canvas, yPos, medicationAdherenceSummary)

// Actividad
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 200f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawActivitySummary(canvas, yPos, activitySummary)

// Hábitos alimenticios
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 200f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawDietaryHabits(canvas, yPos, dietaryHabitsSummary)

// Observaciones clínicas
        ctx = ensureSpace(pdfDocument, pageInfo, page, yPos, 200f)
        page = ctx.page
        canvas = ctx.canvas
        yPos = ctx.yPos
        yPos = drawClinicalNotes(canvas, yPos, clinicalNotes)

        // Finalizar documento
        pdfDocument.finishPage(page)
        val file = savePdf(pdfDocument, userProfile.name)
        pdfDocument.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // -------------------------
    // Funciones privadas por sección
    // -------------------------

    private fun drawHeader(canvas: Canvas, yPos: Float, period: String): Float {
        var y = yPos
        canvas.drawText("Informe Clínico de Diabetes", margin, y, fontTitle)
        y += lineHeight + 5f
        canvas.drawText("Generado el: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", margin, y, fontText)
        y += lineHeight
        canvas.drawText("Período de Análisis: $period", margin, y, fontText)
        return y + lineHeight * 2
    }

    private fun drawPatientData(canvas: Canvas, yPos: Float, userProfile: UserProfile): Float {
        var y = yPos
        canvas.drawText("Datos del Paciente", margin, y, fontSectionTitle)
        y += lineHeight
        canvas.drawText("Nombre: ${userProfile.name}", margin, y, fontText)
        y += lineHeight
        canvas.drawText("Edad: ${userProfile.age} años", margin, y, fontText)
        y += lineHeight
        userProfile.diagnosisDate?.let {
            canvas.drawText("Diagnóstico: ${it.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", margin, y, fontText)
            y += lineHeight
        }
        canvas.drawText("IMC: ${String.format(Locale.US, "%.2f", userProfile.bmi)} (${userProfile.bmiCategory})", margin, y, fontText)
        return y + lineHeight * 2
    }

    private fun drawMedications(canvas: Canvas, yPos: Float, medications: List<MedicationData>): Float {
        var y = yPos
        canvas.drawText("Tratamiento Farmacológico", margin, y, fontSectionTitle)
        y += lineHeight
        if (medications.isNotEmpty()) {
            medications.forEachIndexed { index, med ->
                val text = "${index + 1}. ${med.name} ${med.dose}${med.unit} (Horario: ${med.time}, Frecuencia: ${med.frequency})"
                canvas.drawText(text, margin + 10, y, fontText)
                y += lineHeight
            }
        } else {
            canvas.drawText("Ningún tratamiento registrado.", margin + 10, y, fontText)
            y += lineHeight
        }
        return y + lineHeight * 2
    }

    private fun drawGlucoseMetrics(canvas: Canvas, yPos: Float, stats: GlucoseStats, period: String): Float {
        var y = yPos
        canvas.drawText("Métricas Clave de Glucosa ($period)", margin, y, fontSectionTitle)
        y += lineHeight
        if (stats.totalReadings > 0) {
            canvas.drawText("Promedio: ${String.format(Locale.US, "%.1f", stats.average)} mg/dL", margin, y, fontText); y += lineHeight
            canvas.drawText("Desviación Estándar: ${String.format(Locale.US, "%.1f", stats.stdDev)} mg/dL", margin, y, fontText); y += lineHeight
            canvas.drawText("CV: ${String.format(Locale.US, "%.1f", stats.cv)} %", margin, y, fontText); y += lineHeight
            canvas.drawText("TIR: ${String.format(Locale.US, "%.1f", stats.tir)}%", margin, y, fontText); y += lineHeight
            canvas.drawText("TAR: ${String.format(Locale.US, "%.1f", stats.tar)}%", margin, y, fontText); y += lineHeight
            canvas.drawText("TBR: ${String.format(Locale.US, "%.1f", stats.tbr)}%", margin, y, fontText); y += lineHeight
        } else {
            canvas.drawText("No hay registros de glucosa para este período.", margin, y, fontText)
            y += lineHeight
        }
        return y + lineHeight * 2
    }

    private fun drawGlucoseChart(canvas: Canvas, yPos: Float, records: List<DailyRecordData>, days: Int): Float {
        var y = yPos
        canvas.drawText("Gráfico de Tendencia de Glucosa", margin, y, fontSectionTitle)
        y += lineHeight + 10f
        val chartWidth = (pageWidth - 2 * margin).toInt()
        val chartHeight = 250
        val chartBitmap = createGlucoseChartBitmap(records, days, chartWidth, chartHeight)
        canvas.drawBitmap(chartBitmap, margin, y, null)
        return y + chartHeight + lineHeight * 2
    }

    private fun drawMedicationAdherence(canvas: Canvas, yPos: Float, summary: MedicationAdherenceSummary): Float {
        var y = yPos
        canvas.drawText("Adherencia a la Medicación", margin, y, fontSectionTitle)
        y += lineHeight
        canvas.drawText("Porcentaje de Adherencia: ${summary.adherencePercentage}", margin, y, fontText)
        y += lineHeight
        y += drawWrappedText(canvas, summary.note, y)
        return y + lineHeight
    }

    private fun drawActivitySummary(canvas: Canvas, yPos: Float, summary: ActivitySummary): Float {
        var y = yPos
        canvas.drawText("Resumen de Actividad Física", margin, y, fontSectionTitle)
        y += lineHeight
        canvas.drawText("Total de minutos activos: ${summary.totalMinutes}", margin, y, fontText)
        y += lineHeight

        val activitiesText = "Tipos de actividad: ${summary.uniqueActivities.ifEmpty { "Ninguno" }}"
        y += drawWrappedText(canvas, activitiesText, y)
        return y + lineHeight
    }

    private fun drawDietaryHabits(canvas: Canvas, yPos: Float, summary: DietaryHabitsSummary): Float {
        var y = yPos
        canvas.drawText("Resumen de Hábitos Alimenticios", margin, y, fontSectionTitle)
        y += lineHeight
        canvas.drawText("Calorías totales registradas: ${summary.totalCaloriesRecorded} Cal", margin, y, fontText)
        y += lineHeight

        val foodFreqText = "Tipos de alimento frecuentes:\n${summary.foodTypeFrequency.ifEmpty { "- Ninguno" }}"
        y += drawWrappedText(canvas, foodFreqText, y)
        return y + lineHeight
    }

    private fun drawClinicalNotes(canvas: Canvas, yPos: Float, notes: String): Float {
        var y = yPos
        canvas.drawText("Observaciones Clínicas", margin, y, fontSectionTitle)
        y += lineHeight

        val notesText = notes.ifEmpty { "No se han registrado observaciones clínicas." }
        y += drawWrappedText(canvas, notesText, y)
        return y + lineHeight
    }

    // -------------------------
    // Funciones auxiliares
    // -------------------------

    private fun drawWrappedText(canvas: Canvas, text: String, yPos: Float): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, fontText, pageWidth - (2 * margin).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, yPos)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    data class PageContext(
        val page: PdfDocument.Page,
        val canvas: Canvas,
        val yPos: Float
    )

    private fun ensureSpace(
        pdfDocument: PdfDocument,
        pageInfo: PdfDocument.PageInfo,
        page: PdfDocument.Page,
        yPos: Float,
        requiredSpace: Float
    ): PageContext {
        return if (yPos > pageHeight - requiredSpace) {
            pdfDocument.finishPage(page)
            val newPage = pdfDocument.startPage(pageInfo)
            newPage.canvas.drawColor(android.graphics.Color.WHITE)
            PageContext(newPage, newPage.canvas, margin)
        } else {
            PageContext(page, page.canvas, yPos)
        }
    }


    private fun savePdf(pdfDocument: PdfDocument, patientName: String): File {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(documentsDir, "Informe_Medico_${patientName}_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return file
    }
}

private fun createGlucoseChartBitmap(
    records: List<DailyRecordData>,
    daysInPeriod: Int,
    width: Int,
    height: Int
): Bitmap {
    val validGlucoseRecordsByDay = records
        .filter { it.glucoseReadings.isNotEmpty() }
        .groupBy { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
        .mapValues { (_, dailyRecords) ->
            val averageValue = dailyRecords
                .flatMap { it.glucoseReadings }
                .map { it.value.toFloat() }
                .average().toFloat()
            Pair(dailyRecords.first().date, averageValue)
        }
        .values.sortedBy { it.first.time }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    if (validGlucoseRecordsByDay.isEmpty()) return bitmap

    val today = LocalDate.now()
    val earliestDate = validGlucoseRecordsByDay.first().first.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val latestDate = validGlucoseRecordsByDay.last().first.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    val displayStartDate = if (daysInPeriod == Int.MAX_VALUE) earliestDate
    else maxOf(today.minusDays(daysInPeriod.toLong() - 1), earliestDate)
    val displayEndDate = latestDate

    val totalDays = ChronoUnit.DAYS.between(displayStartDate, displayEndDate).toInt() + 1
    val margin = 40f
    val chartWidth = width - 2 * margin
    val chartHeight = height - 2 * margin

    val minGlucoseRaw = validGlucoseRecordsByDay.minOfOrNull { it.second } ?: GLUCOSE_NORMAL_MIN_AN
    val maxGlucoseRaw = validGlucoseRecordsByDay.maxOfOrNull { it.second } ?: GLUCOSE_HYPERGLYCEMIA_THRESHOLD_AN

    val yMinChart = min(GLUCOSE_HYPOGLYCEMIA_THRESHOLD_AN - 20f, minGlucoseRaw - 20f).coerceAtLeast(0f)
    val yMaxChart = max(GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_AN + 20f, maxGlucoseRaw + 20f)
    val yRange = yMaxChart - yMinChart

    val chartPoints = validGlucoseRecordsByDay.mapNotNull { (recordDate, value) ->
        val recordLocalDate = recordDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        if (!recordLocalDate.isBefore(displayStartDate) && !recordLocalDate.isAfter(displayEndDate)) {
            val dayRelative = ChronoUnit.DAYS.between(displayStartDate, recordLocalDate).toInt()
            Pair(dayRelative, value)
        } else null
    }.sortedBy { it.first }

    val xStepPx = if (totalDays > 1) chartWidth / (totalDays - 1) else chartWidth
    val yValueToPx = if (yRange > 0) chartHeight / yRange else 0f

    // Líneas de referencia (rango normal)
    val dashPaint = Paint().apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        strokeWidth = 2f
        color = android.graphics.Color.GRAY
    }
    val yNormalMin = chartHeight - ((GLUCOSE_NORMAL_MIN_AN - yMinChart) * yValueToPx)
    val yNormalMax = chartHeight - ((GLUCOSE_NORMAL_MAX_AN - yMinChart) * yValueToPx)
    canvas.drawLine(margin, margin + yNormalMin, margin + chartWidth, margin + yNormalMin, dashPaint)
    canvas.drawLine(margin, margin + yNormalMax, margin + chartWidth, margin + yNormalMax, dashPaint)

    // Línea de tendencia
    val linePaint = Paint().apply {
        color = PrimaryBlue.toArgb()
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    for (i in 0 until chartPoints.size - 1) {
        val startX = margin + chartPoints[i].first * xStepPx
        val startY = margin + chartHeight - ((chartPoints[i].second - yMinChart) * yValueToPx)
        val endX = margin + chartPoints[i + 1].first * xStepPx
        val endY = margin + chartHeight - ((chartPoints[i + 1].second - yMinChart) * yValueToPx)
        canvas.drawLine(startX, startY, endX, endY, linePaint)
    }

    // Puntos
    val pointPaint = Paint().apply {
        color = PrimaryBlue.toArgb()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    chartPoints.forEach { (dayRelative, value) ->
        val x = margin + dayRelative * xStepPx
        val y = margin + chartHeight - ((value - yMinChart) * yValueToPx)
        canvas.drawCircle(x, y, 6f, pointPaint)
    }

    return bitmap
}


