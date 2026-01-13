package com.example.diabetes_app.data

import com.google.firebase.firestore.Exclude
import java.util.Date

// --- 1. MODELOS AUXILIARES PARA LECTURAS (Escalabilidad en Firestore) ---
data class GlucoseReading(
    val value: Int = 0,
    val moment: String = "" // Ej: "Ayunas", "Post-Cena"
)

data class BloodPressureReading(
    val sistolica: Int = 0,
    val diastolica: Int = 0,
    val pulso: Int = 0,
    val moment: String = "" // Ej: "Mañana", "Noche"
)

// --- 2. MODELO DE DATOS PARA MEDICAMENTO ---
data class MedicationData(
    val docId: String? = null,
    val name: String = "",
    val dose: Int = 0,
    val unit: String = "",
    val time: String = "", // Formato HH:mm
    val frequency: String = ""
)

// --- 3. MODELO DE REGISTRO DE DOSIS TOMADA ---
data class DosageTakenRecord(
    val medicationDocId: String = "",
    val timestamp: Date = Date(),
    val userId: String = "",
    val timeOfDay: String = ""
)

// --- 4. MODELO DE REGISTRO DIARIO (CORREGIDO PARA COMPATIBILIDAD) ---
data class DailyRecordData(
    val docId: String? = null,
    val date: Date = Date(),

    // Nuevas listas de objetos
    val glucoseReadings: List<GlucoseReading> = emptyList(),
    val bloodPressureReadings: List<BloodPressureReading> = emptyList(),

    val symptoms: List<String> = emptyList(),

    // CORRECCIÓN PARA LOGCAT: Se usa Any para evitar error de HashMap en registros viejos
    val activities: List<Any> = emptyList(),

    val foodTypes: List<String> = emptyList(),
    val activityTime: String? = null,
    val notes: String? = null,

    // CAMPOS DE COMPATIBILIDAD: Evitan el error "No setter/field found" en el Logcat
    // Permiten que Firestore cargue datos antiguos sin que la App se detenga.
    val glucoseValue: Any? = null,
    val sistolica: Any? = null,
    val diastolica: Any? = null,
    val pulso: Any? = null
)

// --- 5. MODELOS PARA REGISTRO DE COMIDAS ---
enum class MealType { DESAYUNO, ALMUERZO, CENA }
enum class MealQuantityLevel { PEQUEÑA, MEDIANA, GRANDE }

data class MealSelectionItem(
    val itemName: String = "",
    val quantityLevel: MealQuantityLevel = MealQuantityLevel.MEDIANA
)

data class MealRecordData(
    val date: Date = Date(),
    val mealType: MealType = MealType.DESAYUNO,
    val selectedItems: List<String> = emptyList(),
    val selectedItemsWithQuantities: List<MealSelectionItem> = emptyList(),
    val actualCalories: Int = 0
)

// --- 6. MODELO DE PERFIL DE USUARIO ---
data class UserProfile(
    val uid: String = "",
    val name: String = "Usuario",
    val email: String = "",
    val photoUrl: String? = null,
    val age: Int = 0,
    val dateOfBirth: Date? = null,
    val phone: String = "",
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val bmi: Double = 0.0,
    val bmiCategory: String = "N/A",
    val gender: String = "",
    val diagnosisDate: Date? = null,
    val condition: String = "",
    val diabetesType: String? = null,
    val avoidFlours: Boolean = false,
    val avoidFats: Boolean = false,
    val avoidSugars: Boolean = false,
    val avoidSausages: Boolean = false,
    val dailyCalorieTarget: String = "1,300",
    val streakDays: Int = 0,
    val lastStreakDate: Date? = null
)

// --- 7. MODELOS PARA UI (ELEMENTOS SELECCIONABLES) ---
interface SelectableRecordItem {
    val name: String
    val iconId: Int
    var isSelected: Boolean
}

data class SymptomItem(
    override val iconId: Int,
    override val name: String,
    override var isSelected: Boolean = false
) : SelectableRecordItem

data class ActivityItem(
    override val iconId: Int,
    override val name: String,
    override var isSelected: Boolean = false
) : SelectableRecordItem

data class FoodTypeItem(
    override val iconId: Int,
    override val name: String,
    override var isSelected: Boolean = false
) : SelectableRecordItem

// Modelos para reportes y estadísticas (Movidos desde MainActivity para limpieza)
data class GlucoseStats(
    val average: Float, val min: Float, val max: Float, val inRangeCount: Int,
    val highCount: Int, val lowCount: Int, val totalReadings: Int, val stdDev: Float,
    val tir: Float, val tar: Float, val tbr: Float, val cv: Float,
    val severeLowCount: Int, val severeHighCount: Int
)

data class NavItem(val title: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)