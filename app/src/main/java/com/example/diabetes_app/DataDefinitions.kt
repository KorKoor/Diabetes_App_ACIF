/* package com.example.diabetes_app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.google.firebase.firestore.Exclude // Importa la anotación @Exclude
import java.util.Date

// --- Modelo de Datos para Medicamento (¡Compatible con Firestore!) ---
// Es una buena práctica usar 'val' para hacer las clases de datos inmutables siempre que sea posible.
data class MedicationData(
    val name: String = "",
    val dose: Int = 0,
    val unit: String = "",
    val time: String = "", // Formato HH:mm
    val frequency: String = "" // Ej. "Cada 8 hr", "Una vez al día"
)

// --- Modelo de Datos para un Registro de Dosis Tomada (¡Compatible con Firestore!) ---
data class DosageTakenRecord(
    val medicationId: String = "", // ID (nombre) del medicamento
    val timestamp: Date = Date(),  // Momento en que se tomó
    val userId: String = "",
    val timeOfDay: String = ""     // "Mañana", "Tarde", "Noche" o la hora exacta de la dosis
)

// --- Modelo de Datos para un Registro Diario Completo (¡Compatible con Firestore!) ---
data class DailyRecordData(
    val date: Date = Date(), // Usamos java.util.Date para Firestore
    val glucoseValue: String? = null,
    val sistolica: String? = null,
    val diastolica: String? = null,
    val pulso: String? = null,
    val symptoms: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val activityTime: String? = null, // Ej. "1h 30m"
    val foodTypes: List<String> = emptyList(),
    val notes: String? = null
)

// --- Modelos de Datos para Registro de Comidas (¡Compatible con Firestore!) ---
enum class MealType {
    DESAYUNO, ALMUERZO, CENA
}

enum class MealQuantityLevel {
    PEQUEÑA, MEDIANA, GRANDE
}

// Clase de datos auxiliar que representa un ítem de comida con su cantidad.
// Esta es la forma más robusta para que Firestore almacene los datos de 'selectedItemsWithQuantities'.
data class MealSelectionItem(
    val itemName: String = "",
    val quantityLevel: MealQuantityLevel = MealQuantityLevel.MEDIANA
)

// Clase principal para el registro de comidas.
data class MealRecordData(
    val date: Date = Date(),
    val mealType: MealType = MealType.DESAYUNO,
    val selectedItems: List<String> = emptyList(),
    val selectedItemsWithQuantities: List<MealSelectionItem> = emptyList(), // ¡Uso de la clase auxiliar!
    val actualCalories: Int = 0
)

// --- Modelo de Datos para el Perfil del Usuario (¡VERSIÓN FINAL!) ---
data class UserProfile(
    val uid: String = "",
    val name: String = "Usuario",
    val email: String = "",
    val photoUrl: String? = null,

    // --- CAMPOS DE PERFIL PERSONAL ---
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

    // --- CAMPOS PARA LAS PREFERENCIAS DE DIETA ---
    val avoidFlours: Boolean = false,
    val avoidFats: Boolean = false,
    val avoidSugars: Boolean = false,
    val avoidSausages: Boolean = false,
    val dailyCalorieTarget: String = "1,300",

    val streakDays: Int = 0,
    val lastStreakDate: Date? = null
)

// --- Modelos de Datos para elementos seleccionables en el registro diario ---
// INTERFACE para elementos seleccionables genéricos
interface SelectableRecordItem {
    val name: String
    val icon: Painter
    var isSelected: Boolean
}

// @Exclude evita que Firestore intente guardar el campo 'icon'
// ya que un 'Painter' no es un tipo de dato compatible con la base de datos.
data class SymptomItem(
    override val name: String,
    @Exclude override val icon: Painter,
    override var isSelected: Boolean = false
) : SelectableRecordItem

data class ActivityItem(
    override val name: String,
    @Exclude override val icon: Painter,
    override var isSelected: Boolean = false
) : SelectableRecordItem

data class FoodTypeItem(
    override val name: String,
    @Exclude override val icon: Painter,
    override var isSelected: Boolean = false
) : SelectableRecordItem             */