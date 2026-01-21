@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.diabetes_app

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.diabetes_app.ui.theme.Diabetes_AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import com.google.gson.Gson
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.draw.shadow
import com.google.firebase.firestore.SetOptions
import com.example.diabetes_app.data.UserProfile
import com.example.diabetes_app.data.MedicationData
import com.example.diabetes_app.data.DailyRecordData
import com.example.diabetes_app.data.MealRecordData
import com.example.diabetes_app.data.DosageTakenRecord
import com.example.diabetes_app.data.MealType
import com.example.diabetes_app.data.SelectableRecordItem
import com.example.diabetes_app.data.SymptomItem
import com.example.diabetes_app.data.ActivityItem
import com.example.diabetes_app.data.FoodTypeItem
import com.example.diabetes_app.data.MealQuantityLevel
import com.example.diabetes_app.data.MealSelectionItem
import com.example.diabetes_app.ui.analysis.AnalysisScreen
import com.example.diabetes_app.ui.profile.ProfileScreen
import com.example.diabetes_app.data.GlucoseReading
import com.example.diabetes_app.data.BloodPressureReading
import androidx.compose.material.icons.filled.LocalFireDepartment
import java.text.SimpleDateFormat
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// --- DEFINICIONES DE COLORES EN KOTLIN (se asume que R.color.* están definidos) ---
val LightBlueCustom = Color(0xFFADD8E6)
val LavenderCustom = Color(0xFFE6E6FA)
val CoralRedCustom = Color(0xFFFF6F61)
val LightGrayCustom = Color(0xFFD3D3D3)
val WhiteCustom = Color.White
val BlueGrayCustom = Color(0xFF607D8B)
val SoftOrangeCustom = Color(0xFFFFCC80)
// --- FIN DEFINICIONES DE COLORES EN KOTLIN ---


const val GLUCOSE_NORMAL_MIN_REPORT = 70f
const val GLUCOSE_NORMAL_MAX_REPORT = 130f
const val GLUCOSE_HYPOGLYCEMIA_THRESHOLD_REPORT = 54f
const val GLUCOSE_HYPERGLYCEMIA_THRESHOLD_REPORT = 180f
const val GLUCOSE_SEVERE_HYPERGLYCEMIA_THRESHOLD_REPORT = 250f

object AppConstants {
    val GLYCEMIA_TIPS = listOf(
        "Controla tu nivel de azúcar en sangre regularmente para tomar decisiones informadas.",
        "Mantén una dieta equilibrada, rica en fibra y baja en azúcares procesados.",
        "Realiza actividad física de forma regular, al menos 30 minutos al día.",
        "Bebe suficiente agua para mantenerte hidratado y ayudar a tu metabolismo.",
        "Gestiona el estrés; puede afectar tus niveles de glucosa.",
        "No olvides tomar tus medicamentos según lo prescrito por tu médico.",
        "Conoce los síntomas de hipoglucemia e hiperglucemia y cómo actuar.",
        "Prioriza el sueño de calidad; impacta directamente en tu salud.",
        "Consulta a tu médico o nutricionista para un plan personalizado.",
        "Evita fumar y el consumo excesivo de alcohol para una mejor salud diabética."
    )
}

data class NavItem(val title: String, val route: String, val icon: ImageVector)

// CLASES AUXILIARES (SE MANTIENEN AQUÍ PARA EVITAR ERRORES DE REFERENCIA CON ANALYSISSCREEN)
data class GlucoseStats(
    val average: Float, val min: Float, val max: Float, val inRangeCount: Int, val highCount: Int, val lowCount: Int, val totalReadings: Int, val stdDev: Float, val tir: Float, val tar: Float, val tbr: Float, val cv: Float, val severeLowCount: Int, val severeHighCount: Int
)

data class ActivitySummary(val totalMinutes: Int, val uniqueActivities: String)

data class DietaryHabitsSummary(
    val foodTypeFrequency: String, val mealTypeCounts: Map<MealType, Int>, val totalCaloriesRecorded: Int
)

data class MedicationAdherenceSummary(
    val adherencePercentage: String, val note: String, val medications: List<MedicationData>
)

data class MealItem(val name: String, val drawableRes: Int)

val mealItems = listOf(
    MealItem("Desayuno", R.drawable.ic_breakfast),
    MealItem("Comida", R.drawable.ic_lunch),
    MealItem("Cena", R.drawable.ic_dinner)
)
// FIN CLASES AUXILIARES


class MainActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        if (firebaseAuth.currentUser == null) {
            val loginIntent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(loginIntent)
            finish()
            return
        }

        val diagnosis = intent.getStringExtra("USER_DIAGNOSIS")
        if (!diagnosis.isNullOrEmpty()) {
            Toast.makeText(this, "Diagnóstico recibido: $diagnosis", Toast.LENGTH_LONG).show()
        }

        // Crear canal de notificación
        NotificationHelper(this).createNotificationChannel()

        // 🔹 Pedir permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // 🔹 Programar alarmas diarias
        val scheduler = AlarmScheduler(this)
        scheduler.scheduleDailyReminders()          // 🌞🍽️🩸🔥🏃 todo junto
        scheduler.scheduleDailyActivityReminders()  // 🏃 actividad 9 AM, 4 PM, 9 PM


        setContent {
            Diabetes_AppTheme {
                MainScreen(
                    firebaseAuth = firebaseAuth,
                    firestoreDb = firestoreDb,
                    initialDiagnosis = diagnosis,
                    onLogoutClick = {
                        firebaseAuth.signOut()
                        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                        val loginIntent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(loginIntent)
                        finish()
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    firebaseAuth: FirebaseAuth,
    firestoreDb: FirebaseFirestore,
    initialDiagnosis: String?,
    onLogoutClick: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val alarmScheduler = remember { AlarmScheduler(context) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var medications by remember { mutableStateOf<List<MedicationData>>(emptyList()) }
    var dailyRecords by remember { mutableStateOf<List<DailyRecordData>>(emptyList()) }
    var mealRecords by remember { mutableStateOf<List<MealRecordData>>(emptyList()) }
    var dosageTakenRecords by remember { mutableStateOf<List<DosageTakenRecord>>(emptyList()) }

    var isLoadingData by remember { mutableStateOf(true) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDate.now()
            if (now != currentDate) {
                currentDate = now
                Log.d("MainScreen", "New day detected. Reloading data.")
                firebaseAuth.currentUser?.uid?.let {
                }
            }
            kotlinx.coroutines.delay(30000L)
        }
    }

    val onUpdateProfile: (UserProfile) -> Unit = { updatedProfile ->
        firebaseAuth.currentUser?.uid?.let { userId ->
            firestoreDb.collection("users").document(userId)
                .set(updatedProfile, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show()
                    userProfile = updatedProfile
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("MainScreen", "Error al actualizar perfil Firestore: ${e.message}")
                }
        }
    }

    val checkAndAdvanceStreak: suspend (String, UserProfile) -> Unit = { userId, currentUserProfile ->
        val today = LocalDate.now()
        val lastStreakLocalDate = currentUserProfile.lastStreakDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

        if (lastStreakLocalDate == null || lastStreakLocalDate.isBefore(today)) {
            val yesterday = today.minusDays(1)

            val yesterdayDailyRecords = firestoreDb.collection("users").document(userId)
                .collection("dailyRecords")
                .whereGreaterThanOrEqualTo("date", Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .whereLessThan("date", Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .get().await().toObjects(DailyRecordData::class.java)

            val yesterdayMealRecords = firestoreDb.collection("users").document(userId)
                .collection("mealRecords")
                .whereGreaterThanOrEqualTo("date", Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .whereLessThan("date", Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .get().await().toObjects(MealRecordData::class.java)

            // CORRECCIÓN: Usar los nuevos campos de lista de objetos
            val hasVitalsRecord = yesterdayDailyRecords.any {
                it.glucoseReadings.isNotEmpty() || it.bloodPressureReadings.isNotEmpty()
            }
            val hasBreakfast = yesterdayMealRecords.any { it.mealType == MealType.DESAYUNO }
            val hasLunch = yesterdayMealRecords.any { it.mealType == MealType.ALMUERZO }
            val hasDinner = yesterdayMealRecords.any { it.mealType == MealType.CENA }

            val didCompleteYesterday = hasVitalsRecord && hasBreakfast && hasLunch && hasDinner

            var newStreakDays = currentUserProfile.streakDays
            val updateNeeded = if (didCompleteYesterday) {
                if (lastStreakLocalDate == yesterday) {
                    newStreakDays += 1
                } else if (lastStreakLocalDate == null || ChronoUnit.DAYS.between(lastStreakLocalDate, today) > 1L) {
                    newStreakDays = 1
                }
                newStreakDays != currentUserProfile.streakDays || lastStreakLocalDate == null || lastStreakLocalDate.isBefore(today)
            } else {
                newStreakDays = 0
                currentUserProfile.streakDays != 0
            }

            if (updateNeeded) {
                firestoreDb.collection("users").document(userId).update(
                    mapOf(
                        "streakDays" to newStreakDays,
                        "lastStreakDate" to Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    )
                )
                    .addOnSuccessListener {
                        userProfile = userProfile.copy(
                            streakDays = newStreakDays,
                            lastStreakDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        )
                        Toast.makeText(context, "Racha actualizada a $newStreakDays días!", Toast.LENGTH_SHORT).show()
                        Log.d("MainScreen", "Racha actualizada a $newStreakDays días")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error al actualizar la racha: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("MainScreen", "Error al actualizar la racha: ${e.message}")
                    }
            }
        }
    }


    LaunchedEffect(firebaseAuth.currentUser?.uid, currentDate) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userDocRef = firestoreDb.collection("users").document(currentUser.uid)

            try {
                val profileDocument = userDocRef.get().await()
                val loadedProfile = profileDocument.toObject(UserProfile::class.java) ?: UserProfile(uid = currentUser.uid)

                userProfile = loadedProfile.copy(
                    uid = currentUser.uid,
                    name = loadedProfile.name.ifEmpty { currentUser.displayName ?: "Usuario" },
                    email = loadedProfile.email.ifEmpty { currentUser.email ?: "" },
                    photoUrl = loadedProfile.photoUrl?.ifEmpty { currentUser.photoUrl?.toString() } ?: currentUser.photoUrl?.toString()
                )
                Log.d("MainScreen", "Perfil cargado: $userProfile")

                if (userProfile.condition.isEmpty() && initialDiagnosis != null) {
                    userProfile = userProfile.copy(condition = initialDiagnosis)
                    userDocRef.set(userProfile, SetOptions.merge()).addOnFailureListener { e ->
                        Log.e("MainScreen", "Error al guardar initialDiagnosis: ${e.message}")
                    }
                }

                // Obtenemos el "snapshot" completo en lugar de solo los objetos
                val medicationsSnapshot = userDocRef.collection("medications").get().await()

                // Mapeamos cada documento manualmente para extraer su ID
                medications = medicationsSnapshot.documents.mapNotNull { document ->
                    val med = document.toObject(MedicationData::class.java)
                    // Usamos .copy para meter el ID del documento (document.id) en el campo docId
                    med?.copy(docId = document.id)
                }
                Log.d("MainScreen", "Medicamentos cargados con IDs: ${medications.map { it.docId }}")
                dailyRecords = userDocRef.collection("dailyRecords").get().await().toObjects(DailyRecordData::class.java)
                mealRecords = userDocRef.collection("mealRecords").get().await().toObjects(MealRecordData::class.java)

                val todayStart = Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val tomorrowStart = Date.from(currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

                dosageTakenRecords = userDocRef.collection("dosageTakenRecords")
                    .whereGreaterThanOrEqualTo("timestamp", todayStart)
                    .whereLessThan("timestamp", tomorrowStart)
                    .get().await().toObjects(DosageTakenRecord::class.java)

                if (userProfile.uid.isNotBlank()) {
                    checkAndAdvanceStreak(currentUser.uid, userProfile)
                }

                fun calculateExpectedDoseTimes(med: MedicationData): List<String> {
                    return when (med.frequency.lowercase()) {
                        "daily" -> listOf(med.time) // cada día a la misma hora
                        "weekly" -> listOf(med.time) // aquí podrías añadir lógica para día de la semana
                        else -> listOf(med.time) // por defecto, usa el horario único
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar datos iniciales: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("MainScreen", "Error al cargar datos iniciales de Firestore: ${e.message}")
                userProfile = UserProfile(
                    uid = currentUser.uid,
                    name = currentUser.displayName ?: "Usuario",
                    email = currentUser.email ?: "",
                    photoUrl = currentUser.photoUrl?.toString()
                )
                medications = emptyList()
                dailyRecords = emptyList()
                mealRecords = emptyList()
                dosageTakenRecords = emptyList()
            } finally {
                isLoadingData = false
            }
        } else {
            Log.d("MainScreen", "Usuario no autenticado al inicio. Redirigiendo a LoginActivity.")
            userProfile = UserProfile(name = "Invitado")
            medications = emptyList()
            dailyRecords = emptyList()
            mealRecords = emptyList()
            dosageTakenRecords = emptyList()
            isLoadingData = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.currentValue == DrawerValue.Open, // ✅ corrección
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f), // ✅ ancho dinámico 80%
                drawerContainerColor = colorResource(id = R.color.drawerBackground)
            ) {
                // ✅ Scroll para que no se corte en pantallas pequeñas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- HEADER con gradiente oscuro fijo ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E1E1E), // gris oscuro
                                        Color(0xFF121212)  // casi negro
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(4.dp)
                                .border(2.dp, Color.White, CircleShape)
                        ) {
                            AsyncImage(
                                model = userProfile.photoUrl ?: R.drawable.ic_default_profile,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White, // ✅ texto blanco fijo
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userProfile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f), // ✅ texto blanco fijo
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = colorResource(id = R.color.drawerDivider)
                    )

                    // ✅ Items corregidos
                    val drawerItems = listOf(
                        NavItem("Inicio", "home", Icons.Filled.Home),
                        NavItem("Signos Vitales", "daily_record", Icons.Filled.MonitorHeart), // añadido
                        NavItem("Medicamentos", "medication", Icons.Filled.MedicalServices), // texto corregido
                        NavItem("Alimentos", "diet", Icons.Filled.Restaurant),
                        NavItem("Análisis Clínico", "analysis", Icons.Filled.Analytics), // texto corregido
                        NavItem("Calendario Histórico", "calendar", Icons.Filled.CalendarMonth),
                        NavItem("Mi Perfil", "profile", Icons.Filled.Person),
                        NavItem("Acerca de", "about", Icons.Filled.Info),
                        NavItem("Cerrar Sesión", "logout", Icons.AutoMirrored.Filled.ExitToApp)
                    )

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    drawerItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant // ✅ dinámico
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface // ✅ dinámico
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.route == "logout") {
                                    onLogoutClick()
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // ✅ dinámico
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp)) // espacio final para scroll
                }
            }
        }
    ){
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = colorResource(id = R.color.bottomNavBackground),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = colorResource(id = R.color.bottomNavStroke).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                ) {
                    val items = listOf(
                        NavItem("Medica", "medication", Icons.Filled.MedicalServices),
                        NavItem("Alimentos", "diet", Icons.Filled.Restaurant),
                        NavItem("Inicio", "home", Icons.Filled.Home),
                        NavItem("Análisis", "analysis", Icons.Filled.Analytics),
                        NavItem("Menú", "drawer", Icons.Default.Menu)
                    )

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val iconColor = if (isSelected) colorResource(id = R.color.bottomNavItemActive)
                        else colorResource(id = R.color.bottomNavItemInactive)
                        val labelColor = if (isSelected) colorResource(id = R.color.bottomNavItemActive)
                        else colorResource(id = R.color.bottomNavItemInactive)

                        if (item.route == "home") {
                            NavigationBarItem(
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(55.dp)
                                            .offset(y = 5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        colorResource(id = R.color.drawerHomeCircle),
                                                        colorResource(id = R.color.bottomNavActiveIndicator)
                                                    ),
                                                    radius = 52f
                                                )
                                            )
                                            .shadow(8.dp, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .align(Alignment.Center),
                                            tint = colorResource(id = R.color.textOnPrimaryColor)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        color = Color.Black
                                    )
                                },
                                selected = isSelected,
                                // ✅ CORRECCIÓN DE NAVEGACIÓN A INICIO
                                onClick = {
                                    navController.navigate("home") {
                                        popUpTo("home") {
                                            inclusive = true // Limpia el estado anterior y fuerza el re-render
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    unselectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    unselectedTextColor = Color.Black,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else if (item.route == "drawer") {
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = iconColor
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        color = labelColor
                                    )
                                },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.open() }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    unselectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    unselectedTextColor = Color.Black,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = iconColor
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        color = labelColor
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    unselectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    unselectedTextColor = Color.Black,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(
                    colorResource(id = R.color.backgroundGradientStart),
                    colorResource(id = R.color.backgroundGradientEnd)
                )))
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoadingData) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorResource(id = R.color.accentGreenButton))
                        Text("Cargando datos...", color = colorResource(id = R.color.textSecondary), fontSize = 20.sp, modifier = Modifier.padding(top = 80.dp))
                    }
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                                    // 1. EXTRAER LA ÚLTIMA GLUCOSA REAL
                            // 1. EXTRAER LA ÚLTIMA GLUCOSA REAL
                            val latestGlucoseReading = dailyRecords
                                .sortedByDescending { it.date?.time ?: 0L } // Ordenar por fecha más reciente
                                .firstNotNullOfOrNull { record -> // Busca el primero que no sea nulo
                                    val fromList = record.glucoseReadings.lastOrNull()

                                    if (fromList != null) {
                                        fromList
                                    } else {
                                        // Caso B: Compatibilidad con datos antiguos
                                        val oldVal = record.glucoseValue?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                                        if (oldVal > 0) GlucoseReading(value = oldVal, moment = "Registro") else null
                                    }
                                }

                            // 2. EXTRAER LA ÚLTIMA PRESIÓN REAL
                            val latestPressureReading = dailyRecords
                                .sortedByDescending { it.date?.time ?: 0L }
                                .firstNotNullOfOrNull { record ->
                                    val fromList = record.bloodPressureReadings.lastOrNull()

                                    if (fromList != null) {
                                        fromList
                                    } else {
                                        // Caso B: Compatibilidad con campos antiguos (sistolica/diastolica)
                                        val sis = record.sistolica?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                                        val dia = record.diastolica?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                                        val pul = record.pulso?.toString()?.toDoubleOrNull()?.toInt() ?: 0

                                        if (sis > 0 && dia > 0) {
                                            BloodPressureReading(sistolica = sis, diastolica = dia, pulso = pul, moment = "Registro")
                                        } else null
                                    }
                                }

                            // 3. REGISTRO GENERAL
                            val latestOverallDailyRecord = dailyRecords.maxByOrNull { it.date?.time ?: 0L }

                            val currentDayMealRecords = mealRecords.filter {
                                it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == currentDate
                            }
                            val todayDosageRecords = dosageTakenRecords.filter {
                                it.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == currentDate
                            }

                            HomeScreen(
                                // .ifBlank evita que se vea vacío si el perfil no ha cargado aún
                                userName = userProfile.name.ifBlank { "Usuario" },
                                profileImageUrl = userProfile.photoUrl,
                                streakDays = userProfile.streakDays,
                                latestOverallDailyRecord = latestOverallDailyRecord,

                                // Estos valores ya vienen calculados de tu lógica anterior (firstOrNull)
                                latestGlucoseReading = latestGlucoseReading,
                                latestPressureReading = latestPressureReading,

                                medications = medications,
                                mealRecords = currentDayMealRecords,
                                todayDosageRecords = todayDosageRecords,
                                onAddRecordClick = { navController.navigate("daily_record") },
                                onRegisterMedicationClick = { navController.navigate("add_edit_medication") },
                                onRegisterFoodClick = { navController.navigate("diet") },
                                onMarkDoseTaken = { medName, timeOfDay ->
                                    val currentUser = firebaseAuth.currentUser
                                    if (currentUser != null) {
                                        val userId = currentUser.uid

                                        // 1. Crear el objeto de registro
                                        val newDosageRecord = DosageTakenRecord(
                                            medicationDocId = medName,
                                            timestamp = Date(),
                                            userId = userId,
                                            timeOfDay = timeOfDay
                                        )

                                        // 2. Crear un ID de documento seguro (sin espacios ni caracteres raros)
                                        val safeMedName = medName.replace(Regex("[^a-zA-Z0-9]"), "_")
                                        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"))
                                        val docId = "${safeMedName}_${LocalDate.now()}_$nowTime"

                                        // 3. Guardar en Firestore
                                        firestoreDb.collection("users").document(userId)
                                            .collection("dosageTakenRecords").document(docId)
                                            .set(newDosageRecord)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "✅ Dosis de $medName registrada", Toast.LENGTH_SHORT).show()
                                                // Actualizar estado local para que la UI reaccione al instante
                                                dosageTakenRecords = dosageTakenRecords + newDosageRecord
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "❌ Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        composable("medication") {
                            MedicationScreen(
                                navController = navController,
                                medications = medications,
                                dosageTakenRecords = dosageTakenRecords,
                                onAddMedicationClick = { navController.navigate("add_edit_medication") },
                                onEditMedication = { medication ->
                                    val medicationJson = Gson().toJson(medication)
                                    navController.navigate("add_edit_medication?medicationJson=$medicationJson")
                                },
                                onRemoveMedication = { medicationToRemove ->
                                    medications = medications.filter { it.name != medicationToRemove.name }
                                    firebaseAuth.currentUser?.uid?.let { userId ->
                                        fun calculateExpectedDoseTimes(med: MedicationData): List<String> {
                                            return listOf(med.time) // o varios horarios si los calculas
                                        }
                                        val dosesToCancel: List<String> = calculateExpectedDoseTimes(medicationToRemove)
                                        dosesToCancel.forEach { timeStr ->
                                            alarmScheduler.cancelNotification(medicationToRemove, timeStr)
                                        }
                                        firestoreDb.collection("users").document(userId)
                                            .collection("medications").document(medicationToRemove.name)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Medicamento eliminado.", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error al eliminar medicamento: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                },
                                onMarkDoseTaken = { medName, timeOfDay ->
                                    firebaseAuth.currentUser?.uid?.let { userId ->
                                        val newDosageRecord = DosageTakenRecord(
                                            medicationDocId = medName,
                                            timestamp = Date(),
                                            userId = userId,
                                            timeOfDay = timeOfDay
                                        )
                                        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"))
                                        val docId = "${medName}-${LocalDate.now()}-$now"

                                        firestoreDb.collection("users").document(userId)
                                            .collection("dosageTakenRecords").document(docId)
                                            .set(newDosageRecord)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Dosis de $medName marcada como tomada.", Toast.LENGTH_SHORT).show()
                                                dosageTakenRecords = dosageTakenRecords + newDosageRecord
                                                val med = MedicationData(
                                                    name = medName,
                                                    dose = 0,
                                                    unit = "",
                                                    time = timeOfDay,
                                                    frequency = ""
                                                )
                                                alarmScheduler.cancelNotification(med, med.time)
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error al marcar dosis: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                // ❌ ELIMINAR onBackClick
                            )
                        }
                        composable("analysis") {
                            // Análisis mantiene el TopAppBar para la navegación de regreso ya que no está en el bottom nav
                            AnalysisScreen(
                                navController = navController,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("calendar") {
                            CalendarScreen(
                                navController = navController,
                                dailyRecords = dailyRecords,
                                mealRecords = mealRecords,
                                onAddRecordClick = { selectedDay ->
                                    if (selectedDay != null) {
                                        navController.navigate("daily_record?initialDate=${selectedDay.toString()}")
                                    } else {
                                        Toast.makeText(context, "Selecciona un día en el calendario", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                // ❌ ELIMINAR onBackClick
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                navController = navController,
                                firebaseAuth = firebaseAuth,
                                firestoreDb = firestoreDb,
                                initialDiagnosis = initialDiagnosis,
                                onLogoutClick = onLogoutClick,
                            )
                        }
                        composable("diet") {
                            DietScreen(
                                navController = navController,
                                initialMealRecords = mealRecords.filter { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == currentDate },
                                onSaveMealRecord = { newRecord ->
                                    firebaseAuth.currentUser?.uid?.let { userId ->
                                        val docId = "${newRecord.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()}-${newRecord.mealType.name}"
                                        firestoreDb.collection("users").document(userId)
                                            .collection("mealRecords").document(docId)
                                            .set(newRecord)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Comida '${newRecord.mealType}' registrada/actualizada.", Toast.LENGTH_SHORT).show()
                                                val updatedList = mealRecords.toMutableList()
                                                val existingIndex = updatedList.indexOfFirst {
                                                    it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == newRecord.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() &&
                                                            it.mealType == newRecord.mealType
                                                }
                                                if (existingIndex != -1) {
                                                    updatedList[existingIndex] = newRecord
                                                } else {
                                                    updatedList.add(newRecord)
                                                }
                                                mealRecords = updatedList.sortedBy { it.date.time }
                                            }
                                            .addOnFailureListener { e -> Toast.makeText(context, "Error al guardar comida: ${e.message}", Toast.LENGTH_SHORT).show() }
                                    }
                                },
                                // ❌ ELIMINAR onBackClick
                                userProfile = userProfile,
                                onUpdateProfile = onUpdateProfile
                            )
                        }
                        composable(
                            route = "add_edit_medication?medicationJson={medicationJson}",
                            arguments = listOf(navArgument("medicationJson") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val medicationJson = backStackEntry.arguments?.getString("medicationJson")
                            val initialMedication = if (medicationJson != null) {
                                Gson().fromJson(medicationJson, MedicationData::class.java)
                            } else null

                            AddEditMedicationScreen(
                                navController = navController,
                                initialMedication = initialMedication,
                                onSaveMedication = { updatedMed ->
                                    firebaseAuth.currentUser?.uid?.let { userId ->
                                        // 1. Prioridad absoluta al docId.
                                        // Si no existe, usamos el nombre (pero solo para registros nuevos).
                                        val documentId = if (!updatedMed.docId.isNullOrEmpty()) {
                                            updatedMed.docId!!
                                        } else {
                                            updatedMed.name // Esto solo ocurrirá la primera vez que se crea
                                        }

                                        val medicationRef = firestoreDb.collection("users").document(userId)
                                            .collection("medications").document(documentId)

                                        medicationRef.set(updatedMed)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Medicamento guardado con éxito", Toast.LENGTH_SHORT).show()

                                                // 2. Actualización reactiva de la lista local
                                                val updatedList = medications.toMutableList()

                                                // Buscamos por docId primero, luego por nombre si es nuevo
                                                val existingIndex = updatedList.indexOfFirst {
                                                    it.docId == documentId || it.name == updatedMed.name
                                                }

                                                if (existingIndex != -1) {
                                                    updatedList[existingIndex] = updatedMed
                                                } else {
                                                    updatedList.add(updatedMed)
                                                }

                                                // Ordenamos y asignamos a la variable de estado
                                                medications = updatedList.sortedBy { it.name }

                                                // 3. Reprogramar notificaciones
                                                val expectedTimes = calculateExpectedDoseTimes(updatedMed)
                                                expectedTimes.forEach { (timeStr, _) ->
                                                    alarmScheduler.scheduleNotification(updatedMed, timeStr)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        // RUTA DE EDICIÓN/AÑADIDO DIARIO ACTUALIZADA PARA SOPORTAR EL OBJETO JSON COMPLETO
                        composable(
                            route = "daily_record?initialDate={initialDate}&initialRecordJson={initialRecordJson}",
                            arguments = listOf(
                                navArgument("initialDate") { type = NavType.StringType; nullable = true },
                                navArgument("initialRecordJson") { type = NavType.StringType; nullable = true }
                            )
                        ) { backStackEntry ->
                            val initialDateString = backStackEntry.arguments?.getString("initialDate")
                            val initialDate = initialDateString?.let { LocalDate.parse(it) }

                            val initialRecordJson = backStackEntry.arguments?.getString("initialRecordJson")
                            val initialRecord = if (initialRecordJson != null) {
                                Gson().fromJson(initialRecordJson, DailyRecordData::class.java)
                            } else null

                            DailyRecordScreen(
                                navController = navController,
                                initialDate = initialDate,
                                initialRecordToEdit = initialRecord,
                                onSaveRecord = { newRecord ->
                                    // Esta función se vació en DailyRecordScreen, por lo que solo navegamos de vuelta.
                                    navController.popBackStack()
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("about") {
                            AboutScreen(
                                navController = navController,
                                // ❌ ELIMINAR onBackClick
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        // ❌ TopAppBar ELIMINADO para usar solo Bottom Nav
        containerColor = colorResource(id = R.color.appBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ Botón de regreso añadido al cuerpo, ya que eliminamos el TopAppBar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = colorResource(id = R.color.headerBackground)
                    )
                }
            }

            Text(
                text = "ACIF App",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Versión 1.0.0",
                fontSize = 16.sp,
                color = colorResource(id = R.color.textSecondary),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "ACIF es una aplicación diseñada para ayudar a las personas con diabetes a gestionar su condición de forma más efectiva. Te permite llevar un registro de tus niveles de glucosa, presión arterial y pulso, así como monitorear tu actividad física y tu alimentación. Con esta herramienta, podrás tener un control detallado de tu salud y compartir reportes con tu médico.",
                fontSize = 16.sp,
                color = colorResource(id = R.color.primaryText),
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Características principales:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "• Registro de mediciones diarias.",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.primaryText)
                )
                Text(
                    text = "• Recordatorios de medicación.",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.primaryText)
                )
                Text(
                    text = "• Monitoreo de alimentación y actividad.",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.primaryText)
                )
                Text(
                    text = "• Generación de reportes médicos.",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.primaryText)
                )
                Text(
                    text = "• Consejos diarios para la salud.",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.primaryText)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Desarrollado por el equipo de ACIF.\n\n" +
                        "Desarrolladores:\n" +
                        "Carlos García Huerta\n" +
                        "Maria Fernanda Avila Silva ❤",
                fontSize = 14.sp,
                color = colorResource(id = R.color.textSecondary)
            )
        }
    }
}


@Composable
fun HomeScreen(
    userName: String,
    profileImageUrl: String?,
    streakDays: Int,
    latestOverallDailyRecord: DailyRecordData?, // Se mantiene por si lo usas en otro lado
    latestGlucoseReading: GlucoseReading?,    // <--- Usaremos este
    latestPressureReading: BloodPressureReading?, // <--- Usaremos este
    medications: List<MedicationData>,
    mealRecords: List<MealRecordData>,
    todayDosageRecords: List<DosageTakenRecord>,
    onAddRecordClick: () -> Unit,
    onRegisterMedicationClick: () -> Unit,
    onRegisterFoodClick: () -> Unit,
    onMarkDoseTaken: (String, String) -> Unit
) {
    val gradientStartColor = colorResource(id = R.color.backgroundGradientStart)
    val gradientEndColor = colorResource(id = R.color.backgroundGradientEnd)

    // ELIMINAMOS LAS LÍNEAS QUE SOBRES ESCRIBÍAN LAS VARIABLES AQUÍ
    // Ahora el Composable usará directamente los parámetros que recibe la función.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Brush.verticalGradient(listOf(gradientStartColor, gradientEndColor)))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ProfileHeader(
            userName = userName,
            profileImageUrl = profileImageUrl,
            streakDays = streakDays
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Esta tarjeta ahora recibirá los datos procesados correctamente
        GlucoseAndPressureCard(
            onAddRecordClick = onAddRecordClick,
            latestGlucoseReading = latestGlucoseReading,
            latestPressureReading = latestPressureReading
        )

        Spacer(modifier = Modifier.height(16.dp))

        MedicationReminderCard(
            medications = medications,
            todayDosageRecords = todayDosageRecords,
            onRegisterMedicationClick = onRegisterMedicationClick,
            onMarkDoseTaken = onMarkDoseTaken
        )

        Spacer(modifier = Modifier.height(16.dp))

        FoodAndDietCard(
            todayMeals = mealRecords,
            onRegisterFoodClick = onRegisterFoodClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        TipOfTheDayCard()
        Spacer(modifier = Modifier.height(16.dp))
    }
}
@Composable
fun ProfileHeader(userName: String, profileImageUrl: String?, streakDays: Int) {
    val imageModel = remember(profileImageUrl) {
        if (profileImageUrl?.startsWith("file://") == true) {
            android.net.Uri.parse(profileImageUrl)
        } else {
            profileImageUrl
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Gradiente Azul/Gris para contraste
                    Brush.verticalGradient(
                        colors = listOf(
                            colorResource(id = R.color.headerBackground), // Azul oscuro (#4A55A2)
                            colorResource(id = R.color.primaryText)       // Azul medio (#3A5A80)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically // Centra toda la fila verticalmente
            ) {
                // 1. FOTO DE PERFIL (Mantiene la simetría)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    AsyncImage(
                        model = imageModel ?: R.drawable.ic_default_profile,
                        contentDescription = "Foto de perfil",
                        placeholder = painterResource(id = R.drawable.ic_default_profile),
                        error = painterResource(id = R.drawable.ic_default_profile),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. SALUDO Y NOMBRE (Alineación estética)
                Column(
                    // Usamos un peso (weight) para darle espacio flexible, pero limitamos a 1f
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center // Asegura el centrado vertical del texto
                ) {
                    Text(
                        text = "¡Hola!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = colorResource(id = R.color.textOnPrimaryColor) // Blanco
                    )
                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.textOnPrimaryColor), // Blanco
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tu progreso hoy:",
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.textOnPrimaryColor).copy(alpha = 0.8f) // Blanco con transparencia
                    )
                }

                // 3. DISTINTIVO DE RACHA ESTILIZADO (Queda a la derecha, bien espaciado)
                StreakBadge(days = streakDays)
            }
        }
    }
}

@Composable
fun StreakBadge(days: Int) {
    val activeColor = colorResource(id = R.color.accentPinkButton)
    val inactiveColor = colorResource(id = R.color.textSecondary)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (days > 0) {
                        // Degradado de tu color de acción
                        Brush.verticalGradient(
                            listOf(activeColor.copy(alpha = 0.8f), activeColor)
                        )
                    } else {
                        // Color secundario (gris) si no hay racha
                        Brush.verticalGradient(
                            listOf(inactiveColor.copy(alpha = 0.8f), inactiveColor)
                        )
                    }
                )
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Racha",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = days.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }
        Text(
            text = "DÍAS",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(id = R.color.textOnPrimaryColor) // Texto blanco para el contraste
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseAndPressureCard(
    onAddRecordClick: () -> Unit,
    latestGlucoseReading: GlucoseReading?,
    latestPressureReading: BloodPressureReading?
) {
    // Preparación de datos (Mantenemos la misma lógica)
    val glucoseValue = latestGlucoseReading?.value
    val glucoseMoment = latestGlucoseReading?.moment ?: "Momento"
    val pressureSistolica = latestPressureReading?.sistolica
    val pressureDiastolica = latestPressureReading?.diastolica
    val latestPulso = latestPressureReading?.pulso?.toString() ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.vitalsCardBackground)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Tu Estado Reciente",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorResource(id = R.color.vitalsTextPrimary),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- Fila 1: Glucosa y Presión Sistólica ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. GLUCOSA
                VitalsSection(
                    modifier = Modifier.weight(1f),
                    title = "Glucosa (mg/dL)",
                    icon = Icons.Default.Bloodtype,
                    value = glucoseValue?.toString() ?: "N/A",
                    subText = glucoseMoment
                )
                // 2. PRESIÓN SISTÓLICA
                VitalsSection(
                    modifier = Modifier.weight(1f),
                    title = "Sistólica (mmHg)",
                    icon = Icons.Default.FavoriteBorder, // Nuevo icono para variedad visual
                    value = pressureSistolica?.toString() ?: "N/A",
                    subText = "Máxima"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Fila 2: Presión Diastólica y Pulso ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 3. PRESIÓN DIASTÓLICA
                VitalsSection(
                    modifier = Modifier.weight(1f),
                    title = "Diastólica (mmHg)",
                    icon = Icons.Default.Favorite,
                    value = pressureDiastolica?.toString() ?: "N/A",
                    subText = "Mínima"
                )
                // 4. PULSO
                VitalsSection(
                    modifier = Modifier.weight(1f),
                    title = "Pulso (LPM)",
                    icon = Icons.Default.MonitorHeart,
                    value = latestPulso,
                    subText = "Frecuencia"
                )
            }
            // --- Fin Cuadrícula de Datos Clave ---

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de acción (sin cambios)
            Button(
                onClick = onAddRecordClick,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.accentPinkButton)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Registrar",
                    tint = colorResource(id = R.color.accentPinkButtonText),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Registrar Nueva Medición",
                    color = colorResource(id = R.color.accentPinkButtonText),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun VitalsSection(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    value: String,
    subText: String
) {
    // Usamos el color primario de texto para el borde y el icono
    val primaryVitalsColor = colorResource(id = R.color.vitalsTextPrimary)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White) // Fondo blanco para que los valores resalten
            .border(1.dp, colorResource(id = R.color.dividerColor), RoundedCornerShape(12.dp)) // Borde sutil
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icono y Título
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = primaryVitalsColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryVitalsColor
            )
        }

        // Valor Principal (Grande y en Negrita)
        Text(
            text = value,
            fontSize = 24.sp, // Valor clave más grande
            fontWeight = FontWeight.ExtraBold,
            color = primaryVitalsColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtexto / Contexto (Pequeño y Secundario)
        Text(
            text = subText,
            fontSize = 10.sp,
            color = colorResource(id = R.color.textSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VitalsRow(icon: ImageVector, label: String, value: String) {
    val valueColor = colorResource(id = R.color.vitalsTextPrimary) // El color principal de tus vitales

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Separa la etiqueta del valor
    ) {
        // Lado Izquierdo: Icono + Etiqueta
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = valueColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$label:",
                fontSize = 15.sp,
                color = colorResource(id = R.color.vitalsTextSecondary),
                fontWeight = FontWeight.Normal
            )
        }

        // Lado Derecho: Valor (más grande y negrita)
        Text(
            text = value,
            fontSize = 16.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MedicationReminderCard(
    medications: List<MedicationData>,
    todayDosageRecords: List<DosageTakenRecord>,
    onRegisterMedicationClick: () -> Unit,
    onMarkDoseTaken: (String, String) -> Unit
) {
    val currentLocalTime = LocalTime.now()
    val today = LocalDate.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // Color de fondo blanco para que las listas de dosis resalten
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Título Principal
            Text(
                text = "Recordatorios de Medicación",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Sección de Dosis
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (medications.isEmpty()) {
                    Text(
                        text = buildAnnotatedString {
                            append("No tienes medicaciones registradas. ")
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(id = R.color.primaryText)
                                )
                            ) {
                                append("¡Pulsa el botón de abajo para añadir una!")
                            }
                        },
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.textSecondary)
                    )
                } else {
                    val pendingDosesByMedication = remember(medications, todayDosageRecords, currentLocalTime) {
                        medications.flatMap { medication ->
                            calculateExpectedDoseTimes(medication).mapNotNull { (timeStr, label) ->
                                val expectedTime = try { LocalTime.parse(timeStr) } catch (e: Exception) { null }
                                if (expectedTime == null) return@mapNotNull null

                                val isTaken = todayDosageRecords.any { it.medicationDocId == medication.name && it.timeOfDay == timeStr }
                                val isPastDue = expectedTime.isBefore(currentLocalTime)

                                if (!isTaken && (isPastDue || expectedTime.isBefore(currentLocalTime.plusMinutes(60)))) {
                                    Triple(medication, timeStr, label)
                                } else {
                                    null
                                }
                            }
                        }.sortedBy { it.second } // Ordena por hora
                    }

                    if (pendingDosesByMedication.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Dosis completadas",
                                tint = colorResource(id = R.color.accentGreenButton),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¡Todas tus dosis importantes de hoy están cubiertas!",
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.accentGreenButton),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Iterar por las dosis pendientes y usar el nuevo Composable DoseItem
                        pendingDosesByMedication.forEach { (medication, timeStr, label) ->
                            val expectedTime = try { LocalTime.parse(timeStr) } catch (e: Exception) { LocalTime.MAX }
                            // Consideramos que la dosis es "tomable" si es la hora actual o hasta una hora después
                            val isAvailableToTake = expectedTime.isBefore(currentLocalTime.plusMinutes(60))

                            DoseItem(
                                medicationName = medication.name,
                                doseTime = timeStr,
                                doseLabel = label,
                                isAvailableToTake = isAvailableToTake,
                                onMarkDoseTaken = { onMarkDoseTaken(medication.name, timeStr) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Gestión
            Button(
                onClick = onRegisterMedicationClick,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.buttonPrimaryBackground)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = "Gestionar Medicamentos",
                    tint = colorResource(id = R.color.textOnPrimaryColor),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Gestionar Medicamentos",
                    color = colorResource(id = R.color.textOnPrimaryColor),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DoseItem(
    medicationName: String,
    doseTime: String,
    doseLabel: String,
    isAvailableToTake: Boolean,
    onMarkDoseTaken: () -> Unit
) {
    val buttonColor = if (isAvailableToTake) colorResource(id = R.color.accentGreenButton) else colorResource(id = R.color.textHint)
    val buttonTextColor = if (isAvailableToTake) colorResource(id = R.color.accentGreenButtonText) else colorResource(id = R.color.textSecondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorResource(id = R.color.placeholderBackground)) // Fondo sutil para la fila
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Información de la Dosis
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = medicationName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colorResource(id = R.color.primaryText)
            )
            Text(
                text = "$doseTime ($doseLabel)",
                fontSize = 13.sp,
                color = colorResource(id = R.color.textSecondary)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botón de Acción
        Button(
            onClick = onMarkDoseTaken,
            enabled = isAvailableToTake,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Text(
                if (isAvailableToTake) "Tomar Ahora" else "Próxima",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = buttonTextColor
            )
        }
    }
}

fun calculateExpectedDoseTimes(medication: MedicationData): List<Pair<String, String>> {
    val parsedTime = try {
        LocalTime.parse(medication.time, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        LocalTime.MIN
    }

    return when (medication.frequency) {
        "Una vez al día" -> listOf(medication.time to "Dosis única")
        "Dos veces al día" -> listOf(
            medication.time to "Mañana",
            parsedTime.plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm")) to "Noche"
        )
        "Tres veces al día" -> listOf(
            parsedTime.minusHours(4).format(DateTimeFormatter.ofPattern("HH:mm")) to "Mañana",
            parsedTime.format(DateTimeFormatter.ofPattern("HH:mm")) to "Tarde",
            parsedTime.plusHours(4).format(DateTimeFormatter.ofPattern("HH:mm")) to "Noche"
        )
        "Cada 6 hr" -> listOf(
            medication.time to "Dosis 1",
            parsedTime.plusHours(6).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 2",
            parsedTime.plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 3",
            parsedTime.plusHours(18).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 4"
        )
        "Cada 8 hr" -> listOf(
            medication.time to "Dosis 1",
            parsedTime.plusHours(8).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 2",
            parsedTime.plusHours(16).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 3"
        )
        "Cada 12 hr" -> listOf(
            parsedTime.plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm")) to "Dosis 2",
            medication.time to "Dosis 1"
        )
        "Cada 24 hr" -> listOf(medication.time to "Dosis única")
        else -> emptyList()
    }
}


@Composable
fun FoodAndDietCard(todayMeals: List<MealRecordData>, onRegisterFoodClick: () -> Unit) {
    // Cálculo de estados
    val hasBreakfast = todayMeals.any { it.mealType == MealType.DESAYUNO }
    val hasLunch = todayMeals.any { it.mealType == MealType.ALMUERZO }
    val hasDinner = todayMeals.any { it.mealType == MealType.CENA }

    val totalMeals = 3
    val mealsCompleted = listOf(hasBreakfast, hasLunch, hasDinner).count { it }
    val progress = mealsCompleted.toFloat() / totalMeals

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Registro de Comidas",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Indicador de Progreso Global
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Progreso diario: $mealsCompleted de $totalMeals",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(id = R.color.primaryText)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colorResource(id = R.color.accentGreenButton),
                        trackColor = colorResource(id = R.color.dividerColor)
                    )
                }
            }

            // Listado visual de comidas
            MealStatusRow(
                label = "Desayuno",
                isCompleted = hasBreakfast,
                icon = Icons.Default.Coffee // Icono de Desayuno
            )
            Spacer(modifier = Modifier.height(8.dp))
            MealStatusRow(
                label = "Almuerzo",
                isCompleted = hasLunch,
                icon = Icons.Default.Restaurant // Icono de Almuerzo/Comida
            )
            Spacer(modifier = Modifier.height(8.dp))
            MealStatusRow(
                label = "Cena",
                isCompleted = hasDinner,
                icon = Icons.Default.NightsStay // Icono de Cena/Noche
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Acción con Icono
            Button(
                onClick = onRegisterFoodClick,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.accentGreenButton)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    Icons.Default.Fastfood,
                    contentDescription = "Registrar Comida",
                    tint = colorResource(id = R.color.accentGreenButtonText),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Registrar Comida",
                    color = colorResource(id = R.color.accentGreenButtonText),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun MealStatusRow(label: String, isCompleted: Boolean, icon: ImageVector) {
    val statusColor = if (isCompleted) colorResource(id = R.color.accentGreenButton) else colorResource(id = R.color.textHint)
    val statusText = if (isCompleted) "Registrado" else "Pendiente"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Icono y Etiqueta de la comida
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                color = colorResource(id = R.color.primaryText),
                fontWeight = FontWeight.SemiBold
            )
        }

        // Estado (Pendiente / Registrado)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completado",
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                fontSize = 14.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TipOfTheDayCard() {
    val primaryColor = colorResource(id = R.color.primaryText)
    val cardBackgroundColor = colorResource(id = R.color.cardBackground) // Blanco
    val textOnPrimary = colorResource(id = R.color.textOnPrimaryColor) // Blanco
    val secondaryTextColor = colorResource(id = R.color.textSecondary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Elevación notable
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. HEADER / BANNER DE ACENTO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Clip y Background crean el banner de color
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(primaryColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates, // Un icono más específico
                    contentDescription = "Consejo del Día",
                    tint = textOnPrimary, // Icono en blanco
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONSEJO DEL DÍA",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = textOnPrimary, // Título en blanco
                    letterSpacing = 1.sp // Pequeño toque estético
                )
            }

            // 2. CONTENIDO DEL CONSEJO
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Título secundario para jerarquía
                Text(
                    text = "Recomendación para tu salud glucémica:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = primaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // El Consejo
                Text(
                    text = AppConstants.GLYCEMIA_TIPS.random(),
                    fontSize = 15.sp,
                    color = secondaryTextColor,
                    lineHeight = 22.sp,
                )
            }

            // 3. Footer Sutil para el borde inferior
            Divider(
                color = colorResource(id = R.color.dividerColor),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    navController: NavController,
    medications: List<MedicationData>,
    dosageTakenRecords: List<DosageTakenRecord>,
    onAddMedicationClick: () -> Unit,
    onEditMedication: (MedicationData) -> Unit,
    onRemoveMedication: (MedicationData) -> Unit,
    onMarkDoseTaken: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val currentTime = LocalTime.now()

    Scaffold(
        containerColor = colorResource(id = R.color.appBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(colorResource(id = R.color.appBackground))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = colorResource(id = R.color.primaryText)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Medicamentos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.primaryText),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            if (medications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "No hay medicamentos",
                            tint = colorResource(id = R.color.textHint),
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                        )
                        Text(
                            "Aún no tienes medicamentos registrados.",
                            textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.primaryText),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        MedicationAddButton(onAddMedicationClick)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        MedicationAddButton(onAddMedicationClick)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(medications) { medication ->
                        // Lógica de cálculo de dosis (Mantenemos la lógica existente)
                        val dosesForMedication = remember(medication, dosageTakenRecords) {
                            val todayDosages = dosageTakenRecords.filter {
                                it.medicationDocId == medication.name &&
                                        it.timestamp.toInstant().atZone(ZoneId.systemDefault())
                                            .toLocalDate() == today
                            }

                            calculateExpectedDoseTimes(medication).map { (time, label) ->
                                Triple(
                                    time,
                                    label,
                                    todayDosages.any { it.medicationDocId == medication.name && it.timeOfDay == time })
                            }
                        }

                        MedicationItemWithTracking(
                            medication = medication,
                            dosesForMedication = dosesForMedication,
                            currentTime = currentTime,
                            onEditClick = { onEditMedication(medication) },
                            onRemoveClick = { onRemoveMedication(medication) },
                            onMarkDoseTaken = onMarkDoseTaken
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun MedicationAddButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.buttonPrimaryBackground)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Añadir",
            tint = colorResource(id = R.color.textOnPrimaryColor),
            modifier = Modifier.padding(end = 8.dp).size(20.dp)
        )
        Text(
            "Añadir Medicamento",
            color = colorResource(id = R.color.textOnPrimaryColor),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MedicationItemWithTracking(
    medication: MedicationData,
    dosesForMedication: List<Triple<String, String, Boolean>>,
    currentTime: LocalTime,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onMarkDoseTaken: (String, String) -> Unit
) {
    // Definimos los colores base para esta tarjeta
    val primaryColor = colorResource(id = R.color.primaryText)
    val cardBackground = colorResource(id = R.color.cardBackground)
    val secondaryText = colorResource(id = R.color.textSecondary)
    // Contar el progreso
    val totalDoses = dosesForMedication.size
    val dosesTaken = dosesForMedication.count { it.third }
    val progress = if (totalDoses > 0) dosesTaken.toFloat() / totalDoses else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // Borde más suave
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Elevación destacada
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- HEADER: Nombre, Dosis y Botones de Gestión ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.placeholderBackground)) // Fondo sutil para el encabezado
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = primaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${medication.dose} ${medication.unit} (${medication.frequency})",
                        fontSize = 13.sp,
                        color = secondaryText
                    )
                }
                // Botones de Acción Agrupados
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = colorResource(id = R.color.vitalsButtonAddNoteText),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // --- SEPARADOR Y BARRA DE PROGRESO ---
            Divider(color = colorResource(id = R.color.dividerColor), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progreso
                Text(
                    text = "Dosis tomadas: $dosesTaken de $totalDoses",
                    fontSize = 14.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .width(100.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colorResource(id = R.color.accentGreenButton),
                    trackColor = colorResource(id = R.color.dividerColor)
                )
            }
            Divider(color = colorResource(id = R.color.dividerColor), thickness = 1.dp)
            // --- DETALLE DE DOSIS CON BOTONES ---
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dosesForMedication.forEach { (timeStr, label, isTaken) ->
                    DoseTrackingRow(
                        timeStr = timeStr,
                        label = label,
                        isTaken = isTaken,
                        currentTime = currentTime,
                        medicationName = medication.name,
                        onMarkDoseTaken = onMarkDoseTaken
                    )
                }
            }
        }
    }
}

@Composable
fun DoseTrackingRow(
    timeStr: String,
    label: String,
    isTaken: Boolean,
    currentTime: LocalTime,
    medicationName: String,
    onMarkDoseTaken: (String, String) -> Unit
) {
    val expectedTime = try { LocalTime.parse(timeStr) } catch (e: Exception) { LocalTime.MAX }

    // --- LÓGICA CORREGIDA PARA PERMITIR REGISTRO ATRASADO ---
    // Se puede tomar desde 60 min antes y NO hay límite de tiempo después (se elimina isPast)
    val isAvailableToTake = currentTime.isAfter(expectedTime.minusMinutes(60))
    val isLate = currentTime.isAfter(expectedTime.plusMinutes(10)) && !isTaken

    val indicatorColor = when {
        isTaken -> colorResource(id = R.color.accentGreenButton) // Verde: Tomada
        isLate -> Color(0xFFE57373) // Rojo suave: Atrasada pero disponible
        isAvailableToTake -> colorResource(id = R.color.accentPinkButton) // Rosa: Ahora/Pendiente
        else -> colorResource(id = R.color.textHint) // Gris: Próxima
    }

    val buttonText = when {
        isTaken -> "TOMADA"
        isLate -> "ATRASADA" // En lugar de PERDIDA
        isAvailableToTake -> "TOMAR"
        else -> "PRÓXIMA"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorResource(id = R.color.placeholderBackground).copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Hora y Etiqueta
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Hora",
                tint = colorResource(id = R.color.primaryText).copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$timeStr ",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText)
            )
            Text(
                text = "($label)",
                fontSize = 14.sp,
                color = colorResource(id = R.color.textSecondary)
            )
        }

        // Botón de Estado / Acción
        // ✅ CORRECCIÓN: El botón de acción ahora se muestra siempre que no esté tomada y ya sea la hora
        if (isTaken) {
            Text(
                text = buttonText,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = indicatorColor
            )
        } else if (isAvailableToTake) {
            Button(
                onClick = { onMarkDoseTaken(medicationName, timeStr) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = indicatorColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Caso para dosis futuras (Próxima)
            Text(
                text = buttonText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.textHint)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    navController: NavController,
    initialMedication: MedicationData? = null,
    onSaveMedication: (MedicationData) -> Unit,
    onCancel: () -> Unit
) {
    // CLAVE PARA EDICIÓN: Mantener el ID original si existe.
    val originalId: String? = initialMedication?.docId

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Estado de los campos (sin cambios en la lógica)
    var medicationName by remember { mutableStateOf(initialMedication?.name ?: "") }
    var medicationDoseNumber by remember {
        mutableStateOf(
            initialMedication?.dose?.toString() ?: ""
        )
    }

    var medicationUnitExpanded by remember { mutableStateOf(false) }
    val units = listOf("mg", "UI", "g", "ml")
    var selectedMedicationUnit by remember { mutableStateOf(initialMedication?.unit ?: units[0]) }

    var medicationTimeInput by remember {
        mutableStateOf(
            TextFieldValue(
                initialMedication?.time ?: LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            )
        )
    }

    var frequencyExpanded by remember { mutableStateOf(false) }
    val frequencies = listOf(
        "Cada 6 hr", "Cada 8 hr", "Cada 12 hr", "Cada 24 hr",
        "Una vez al día", "Dos veces al día", "Tres veces al día"
    )

    var selectedFrequency by remember {
        mutableStateOf(
            initialMedication?.frequency ?: frequencies[0]
        )
    }
    val title = if (initialMedication == null) "Añadir Medicamento" else "Editar Medicamento"

    //(Tema Holo Light)
    val timePickerDialog = TimePickerDialog(
        context,
        // Usamos Theme_Holo_Light_Dialog_NoActionBar para asegurar un fondo claro y texto oscuro,
        // corrigiendo el problema de colapso de colores.
        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
        { _, hour: Int, minute: Int ->
            val newTime = LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("HH:mm"))
            medicationTimeInput = TextFieldValue(newTime)
        },
        // Le pasamos la hora actual como valor inicial
        LocalTime.now().hour, LocalTime.now().minute, true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colorResource(id = R.color.primaryText),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = colorResource(id = R.color.primaryText)
                ),
                modifier = Modifier
                    .height(60.dp)
                    .padding(start = 16.dp, top = 4.dp)
            )
        },
        containerColor = colorResource(id = R.color.appBackground),
        modifier = Modifier.clickable(onClick = { keyboardController?.hide() })
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(colorResource(id = R.color.appBackground))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // --- SECCIÓN 1: INFORMACIÓN BÁSICA ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detalles del medicamento",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.primaryText),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = medicationName,
                        onValueChange = { medicationName = it },
                        label = { Text("Nombre del medicamento") },
                        placeholder = { Text("Insulina / Metformina") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Medication, contentDescription = "Nombre") },
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN 2: DOSIS, HORA Y FRECUENCIA ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configuración de dosis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.primaryText),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 2.1 DOSIS Y UNIDAD
                    Text(
                        text = "Dosis y unidad prescrita",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(id = R.color.primaryText),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = medicationDoseNumber,
                            onValueChange = { newValue ->
                                // Permite solo números (y vacío)
                                if (newValue.matches(Regex("^\\d*\$"))) {
                                    medicationDoseNumber = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.6f),
                            singleLine = true,
                            placeholder = { Text("Ej. 50") },
                            colors = outlinedTextFieldColors()
                        )

                        ExposedDropdownMenuBox(
                            expanded = medicationUnitExpanded,
                            onExpandedChange = { medicationUnitExpanded = !medicationUnitExpanded },
                            modifier = Modifier.weight(0.4f)
                        ) {
                            OutlinedTextField(
                                value = selectedMedicationUnit,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = medicationUnitExpanded) },
                                modifier = Modifier.menuAnchor(),
                                singleLine = true,
                                colors = outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = medicationUnitExpanded,
                                onDismissRequest = { medicationUnitExpanded = false },
                                modifier = Modifier.background(colorResource(id = R.color.cardBackground))
                            ) {
                                units.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit, color = colorResource(id = R.color.primaryText)) },
                                        onClick = {
                                            selectedMedicationUnit = unit
                                            medicationUnitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // 2.2 HORA DE INICIO
                    OutlinedTextField(
                        value = medicationTimeInput,
                        onValueChange = { /* Deshabilitado */ },
                        readOnly = true,
                        label = { Text("Hora de inicio") },
                        placeholder = { Text("Ej. 08:30") },
                        trailingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Seleccionar hora",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = "Hora") },
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 2.3 FRECUENCIA
                    Text(
                        text = "Frecuencia de toma",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorResource(id = R.color.primaryText),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = frequencyExpanded,
                        onExpandedChange = { frequencyExpanded = !frequencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedFrequency,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = "Frecuencia") },
                            colors = outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = { frequencyExpanded = false },
                            modifier = Modifier.background(colorResource(id = R.color.cardBackground))
                        ) {
                            frequencies.forEach { frequency ->
                                DropdownMenuItem(
                                    text = { Text(frequency, color = colorResource(id = R.color.primaryText)) },
                                    onClick = {
                                        selectedFrequency = frequency
                                        frequencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN 3: BOTONES DE ACCIÓN (Mejor Jerarquía) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón principal: GUARDAR/ACTUALIZAR
                Button(
                    onClick = {
                        if (medicationName.isBlank() || medicationDoseNumber.isBlank()) {
                            Toast.makeText(context, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val dose = medicationDoseNumber.toIntOrNull() ?: 0

                        // *** MODIFICACIÓN CLAVE PARA EDICIÓN ***
                        val newMedication = MedicationData(
                            // Si originalId es null, se creará uno nuevo (comportamiento de "Añadir").
                            // Si originalId tiene un valor, se usará para actualizar el registro existente.
                            docId = originalId, // Asume que MedicationData usa -1 o 0 para un nuevo ID
                            name = medicationName,
                            dose = dose,
                            unit = selectedMedicationUnit,
                            time = medicationTimeInput.text,
                            frequency = selectedFrequency
                        )
                        onSaveMedication(newMedication)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.buttonPrimaryBackground)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Guardar", tint = colorResource(id = R.color.textOnPrimaryColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (initialMedication == null) "Guardar" else "Actualizar",
                        color = colorResource(id = R.color.textOnPrimaryColor),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Botón secundario: CANCELAR
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(0.7f) // Botón de cancelar ligeramente más pequeño
                        .height(52.dp),
                    border = BorderStroke(2.dp, colorResource(id = R.color.accentPinkButton)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        "Cancelar",
                        color = colorResource(id = R.color.accentPinkButton),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colorResource(id = R.color.primaryText),
    unfocusedTextColor = colorResource(id = R.color.primaryText),
    cursorColor = colorResource(id = R.color.primaryText),
    focusedBorderColor = colorResource(id = R.color.primaryText),
    unfocusedBorderColor = colorResource(id = R.color.dividerColor),
    focusedLabelColor = colorResource(id = R.color.primaryText),
    unfocusedLabelColor = colorResource(id = R.color.textSecondary),
    focusedLeadingIconColor = colorResource(id = R.color.primaryText),
    unfocusedLeadingIconColor = colorResource(id = R.color.textSecondary),
    focusedTrailingIconColor = colorResource(id = R.color.primaryText),
    unfocusedTrailingIconColor = colorResource(id = R.color.textSecondary),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    dailyRecords: List<DailyRecordData>,
    mealRecords: List<MealRecordData>,
    onAddRecordClick: (LocalDate?) -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val initialDate = remember { LocalDate.now() }
    val currentMonthYear by remember { mutableStateOf(initialDate) }

    val currentMonth = currentMonthYear.month
    val currentYear = currentMonthYear.year
    val daysInMonth = remember { currentMonthYear.lengthOfMonth() }
    val firstDayOfMonth = remember { currentMonthYear.dayOfWeek.value }

    var selectedDay by remember { mutableStateOf<LocalDate?>(today) }
    var showRecordDetailsDialog by remember { mutableStateOf<DailyRecordData?>(null) }

    val recordsForSelectedDay = remember(selectedDay, dailyRecords) {
        selectedDay?.let { day ->
            dailyRecords.filter {
                it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == day
            }.sortedByDescending { it.date.time }
        } ?: emptyList()
    }
    val mealsForSelectedDay = remember(selectedDay, mealRecords) {
        selectedDay?.let { day ->
            mealRecords.filter {
                it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == day
            }.sortedBy { it.mealType.ordinal }
        } ?: emptyList()
    }
    val detailsRecord = remember(recordsForSelectedDay) {
        recordsForSelectedDay.firstOrNull { it.activities.isNotEmpty() || it.symptoms.isNotEmpty() || it.notes?.isNotBlank() == true }
    }

    // --- FUNCIÓN PARA EXPORTAR ---
    fun compartirReporte() {
        if (dailyRecords.isEmpty()) {
            Toast.makeText(context, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val reporte = StringBuilder()
        reporte.append("Reporte de Salud - ACIF App\n")
        reporte.append("Generado el: ${LocalDate.now()}\n\n")

        // Tomar los últimos 7 días de registros
        dailyRecords.sortedByDescending { it.date }.take(10).forEach { record ->
            val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(record.date)
            reporte.append("📅 Fecha: $fechaFormateada\n")

            record.glucoseReadings.forEach { g ->
                reporte.append("🩸 Glucosa: ${g.value} mg/dL (${g.moment})\n")
            }
            record.bloodPressureReadings.forEach { p ->
                reporte.append("❤️ Presión: ${p.sistolica}/${p.diastolica} mmHg (Pulso: ${p.pulso})\n")
            }
            if (!record.notes.isNullOrBlank()) reporte.append("📝 Notas: ${record.notes}\n")
            reporte.append("--------------------------\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mi Reporte de Glucosa y Presión")
            putExtra(Intent.EXTRA_TEXT, reporte.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Reporte con mi Médico"))
    }

    Scaffold(
        containerColor = colorResource(id = R.color.appBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(colorResource(id = R.color.appBackground))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = colorResource(id = R.color.primaryText))
                    }
                    Text(
                        "Historial y Reportes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.primaryText)
                    )
                }

                // ✅ BOTÓN CORREGIDO: Ahora llama a compartirReporte()
                IconButton(onClick = { compartirReporte() }) {
                    Icon(
                        imageVector = Icons.Filled.Share, // Cambiado a Share para que sea más claro
                        contentDescription = "Exportar Reporte",
                        tint = colorResource(id = R.color.buttonPrimaryBackground)
                    )
                }
            }

            Text(
                text = currentMonth.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "ES")).uppercase() + " $currentYear",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // --- GRID DEL CALENDARIO ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                            Text(day, fontWeight = FontWeight.Bold, color = colorResource(id = R.color.textSecondary), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val displayOffset = (firstDayOfMonth - 1 + 7) % 7
                    var dayCounter = 1 - displayOffset
                    repeat((daysInMonth + displayOffset + 6) / 7) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            repeat(7) {
                                if (dayCounter in 1..daysInMonth) {
                                    val dateForCell = LocalDate.of(currentYear, currentMonth, dayCounter)
                                    val isSelected = selectedDay == dateForCell
                                    val hasRecord = dailyRecords.any { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == dateForCell }

                                    Box(
                                        modifier = Modifier.size(40.dp).padding(2.dp)
                                            .background(if (isSelected) colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                                            .clickable { selectedDay = dateForCell },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = dayCounter.toString(), color = if (dateForCell == today) colorResource(id = R.color.buttonPrimaryBackground) else colorResource(id = R.color.primaryText), fontWeight = if (dateForCell == today) FontWeight.Bold else FontWeight.Normal)
                                        if (hasRecord) {
                                            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).background(colorResource(id = R.color.accentGreenButton), CircleShape))
                                        }
                                    }
                                } else { Spacer(modifier = Modifier.size(40.dp)) }
                                dayCounter++
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedDay != null) {
                DailyRecordsSection(
                    selectedDay = selectedDay!!,
                    recordsForSelectedDay = recordsForSelectedDay,
                    mealsForSelectedDay = mealsForSelectedDay,
                    detailsRecord = detailsRecord,
                    onAddRecordClick = onAddRecordClick,
                    onViewRecord = { showRecordDetailsDialog = it },
                    onEditRecord = { recordToEdit ->
                        val recordJson = Gson().toJson(recordToEdit)
                        navController.navigate("daily_record?initialRecordJson=$recordJson")
                    }
                )
            }
        }

        showRecordDetailsDialog?.let { record ->
            RecordDetailsDialog(record = record, onDismiss = { showRecordDetailsDialog = null })
        }
    }
}
@Composable
fun DailyRecordsSection(
    selectedDay: LocalDate,
    recordsForSelectedDay: List<DailyRecordData>,
    mealsForSelectedDay: List<MealRecordData>,
    detailsRecord: DailyRecordData?,
    onAddRecordClick: (LocalDate?) -> Unit,
    onViewRecord: (DailyRecordData) -> Unit,
    onEditRecord: (DailyRecordData) -> Unit
) {
    val primaryColor = colorResource(id = R.color.primaryText)
    val hasRecords = recordsForSelectedDay.isNotEmpty() || mealsForSelectedDay.isNotEmpty()

    // Título de la sección de registros
    Text(
        text = "Actividad del ${
            selectedDay.format(
                DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("es", "ES"))
            )
        }",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = primaryColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
    )

    if (!hasRecords) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.MoodBad,
                contentDescription = "Sin registros",
                tint = colorResource(id = R.color.textHint),
                modifier = Modifier.size(48.dp).padding(8.dp)
            )
            Text(
                "No hay registros para este día.",
                fontSize = 16.sp,
                color = colorResource(id = R.color.textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. MEDICIONES VITALES (Glucosa, Presión) ---
            val vitalsOnlyRecords = recordsForSelectedDay.filter {
                it.glucoseReadings.isNotEmpty() || it.bloodPressureReadings.isNotEmpty()
            }
            if (vitalsOnlyRecords.isNotEmpty()) {
                RecordGroupCard(title = "Mediciones Vitales", icon = Icons.Default.MonitorHeart) {
                    vitalsOnlyRecords.forEach { record ->
                        // Asumimos que RecordCardItemWithActions renderiza una tarjeta con botones de Ver/Editar
                        RecordCardItemWithActions(
                            record = record,
                            onViewClick = onViewRecord,
                            onEditClick = onEditRecord
                        )
                    }
                }
            }

            // --- 2. REGISTROS DE COMIDAS ---
            if (mealsForSelectedDay.isNotEmpty()) {
                RecordGroupCard(title = "Comidas Registradas", icon = Icons.Default.Restaurant) {
                    mealsForSelectedDay.forEach { mealRecord ->
                        val capitalizedMealType = when (mealRecord.mealType) {
                            MealType.ALMUERZO -> "Comida"
                            else -> mealRecord.mealType.name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
                        }
                        // Usamos una tarjeta simple para comidas
                        RecordCardItem(
                            title = "$capitalizedMealType (${mealRecord.actualCalories} Cal)",
                            content = {
                                Text(
                                    "Items: ${
                                        mealRecord.selectedItemsWithQuantities.joinToString(", ") { (name, level) ->
                                            "$name (${level.name.lowercase(Locale.ROOT)})"
                                        }
                                    }",
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.textSecondary)
                                )
                            }
                        )
                    }
                }
            }

            // --- 3. DETALLES DEL DÍA (Notas, Síntomas, Actividad) ---
            if (detailsRecord != null && (detailsRecord.symptoms.isNotEmpty() || detailsRecord.activities.isNotEmpty() || detailsRecord.notes?.isNotBlank() == true)) {
                RecordGroupCard(title = "Hábitos y Notas", icon = Icons.Default.StickyNote2) {
                    RecordCardItem(
                        title = "Resumen de Hábitos",
                        content = {
                            if (detailsRecord.symptoms.isNotEmpty()) {
                                Text("Síntomas: ${detailsRecord.symptoms.joinToString(", ")}", fontSize = 14.sp, color = primaryColor)
                            }
                            if (detailsRecord.activities.isNotEmpty()) {
                                Text("Actividad: ${detailsRecord.activities.joinToString()} (${detailsRecord.activityTime})", fontSize = 14.sp, color = primaryColor)
                            }
                            detailsRecord.notes?.let {
                                if (it.isNotBlank()) {
                                    Text("Notas: $it", fontSize = 14.sp, color = primaryColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Botón de Añadir Registro
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = { onAddRecordClick(selectedDay) },
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.accentPinkButton)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth(0.8f).height(50.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Añadir", tint = colorResource(id = R.color.accentPinkButtonText))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Añadir Registro",
            color = colorResource(id = R.color.accentPinkButtonText),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun RecordGroupCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colorResource(id = R.color.buttonPrimaryBackground),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = colorResource(id = R.color.buttonPrimaryBackground)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = colorResource(id = R.color.dividerColor))
            Spacer(modifier = Modifier.height(12.dp))

            // Contenido dinámico (las RecordCardItem)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun RecordCardItemWithActions(
    record: DailyRecordData,
    onViewClick: (DailyRecordData) -> Unit,
    onEditClick: (DailyRecordData) -> Unit
) {
    val primaryColor = colorResource(id = R.color.primaryText)
    val secondaryText = colorResource(id = R.color.textSecondary)
    val accentColor = colorResource(id = R.color.accentPinkButton)
    val buttonPrimary = colorResource(id = R.color.buttonPrimaryBackground)

    // Calcular el tiempo de registro
    val recordTime = remember {
        record.date.toInstant()
            .atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), // Bordes más suaves
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- HORA y TÍTULO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = "Hora",
                    tint = secondaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Registro de Vitales",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    recordTime,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = buttonPrimary
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = colorResource(id = R.color.dividerColor))

            // --- LECTURAS VITALES CON ICONOS ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Glucosa
                record.glucoseReadings.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bloodtype, // Gota de sangre
                            contentDescription = "Glucosa",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${entry.value} mg/dL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${entry.moment})",
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    }
                }

                // Presión/Pulso
                record.bloodPressureReadings.lastOrNull()?.let { pressureEntry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MonitorHeart, // Corazón/Monitor
                            contentDescription = "Presión",
                            tint = colorResource(id = R.color.accentGreenButton),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${pressureEntry.sistolica}/${pressureEntry.diastolica} mmHg",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Pulso: ${pressureEntry.pulso} LPM",
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp), color = colorResource(id = R.color.dividerColor))

            // --- ACCIONES DE LA TARJETA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ver (Primario)
                TextButton(onClick = { onViewClick(record) }) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = "Ver",
                        tint = buttonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Detalles", color = buttonPrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Editar (Secundario/Accento)
                OutlinedButton(
                    onClick = { onEditClick(record) },
                    border = BorderStroke(1.dp, accentColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RecordDetailsDialog(record: DailyRecordData, onDismiss: () -> Unit) {
    val primaryColor = colorResource(id = R.color.primaryText)
    val accentColor = colorResource(id = R.color.buttonPrimaryBackground)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Detalles", tint = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Detalles del Registro",
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // --- METADATOS ---
                Text(
                    "📅 Fecha: ${record.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
                Text(
                    "⏱️ Hora: ${record.date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- SECCIÓN: VITALES ---
                Text("Vitales", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor)
                Spacer(modifier = Modifier.height(8.dp))

                if (record.glucoseReadings.isNotEmpty() || record.bloodPressureReadings.isNotEmpty()) {
                    record.glucoseReadings.forEachIndexed { index, entry ->
                        Text(
                            "🩸 Glucosa ${index + 1}: ${entry.value} mg/dL (${entry.moment})",
                            color = primaryColor
                        )
                    }
                    record.bloodPressureReadings.lastOrNull()?.let { pressureEntry ->
                        Text("❤️ Presión: ${pressureEntry.sistolica}/${pressureEntry.diastolica} mmHg", color = primaryColor)
                        Text("Pulso: ${pressureEntry.pulso} LPM", color = primaryColor)
                    }
                } else {
                    Text("No se registraron mediciones vitales.", color = colorResource(id = R.color.textSecondary))
                }


                // --- SECCIÓN: HÁBITOS ---
                if (record.symptoms.isNotEmpty() || record.activities.isNotEmpty() || record.foodTypes.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Hábitos y Contexto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (record.symptoms.isNotEmpty()) {
                        Text("🤒 Síntomas: ${record.symptoms.joinToString(", ")}", color = primaryColor)
                    }
                    if (record.activities.isNotEmpty()) {
                        Text("🏃 Actividad: ${record.activities.joinToString()} (Duración: ${record.activityTime ?: "N/A"})", color = primaryColor)
                    }
                    if (record.foodTypes.isNotEmpty()) {
                        Text("🍽️ Tipo de comida: ${record.foodTypes.joinToString(", ")}", color = primaryColor)
                    }
                }

                // --- SECCIÓN: NOTAS ---
                record.notes?.let {
                    if (it.isNotBlank()) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Notas Adicionales", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📝 $it", color = primaryColor)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Cerrar", color = colorResource(id = R.color.textOnPrimaryColor), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colorResource(id = R.color.cardBackground) // Fondo del diálogo
    )
}

@Composable
fun RecordCardItem(title: String, content: @Composable () -> Unit) {
    val primaryColor = colorResource(id = R.color.primaryText)
    val accentColor = colorResource(id = R.color.buttonPrimaryBackground)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Borde sutil para acentuar la tarjeta
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = accentColor // Título con color de acento
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DietScreen(
    navController: NavController,
    initialMealRecords: List<MealRecordData>,
    onSaveMealRecord: (MealRecordData) -> Unit,
    // ❌ onBackClick: () -> Unit ELIMINADO
    userProfile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var avoidFlours by remember { mutableStateOf(userProfile.avoidFlours) }
    var avoidFats by remember { mutableStateOf(userProfile.avoidFats) }
    var avoidSugars by remember { mutableStateOf(userProfile.avoidSugars) }
    var avoidSausages by remember { mutableStateOf(userProfile.avoidSausages) }
    var dailyCalorieTargetInput by remember { mutableStateOf(userProfile.dailyCalorieTarget) }

    // Estado para el diálogo de visualización
    var showMealRecordsDialog by remember { mutableStateOf(false) }

    val mealItems = remember {
        mutableStateListOf(
            MealItem("Proteínas", R.drawable.ic_protein),
            MealItem("Carbohidratos", R.drawable.ic_carbs),
            MealItem("Verduras", R.drawable.ic_vegetables),
            MealItem("Frutas", R.drawable.ic_fruits),
            MealItem("Lácteos", R.drawable.ic_dairy),
            MealItem("Grasas", R.drawable.ic_fat),
            MealItem("Azúcares", R.drawable.ic_sugar),
            MealItem("Procesados", R.drawable.ic_processed_food)
        )
    }


    var breakfastMealItems by remember { mutableStateOf(mapOf<String, MealQuantityLevel>()) }
    var lunchMealItems by remember { mutableStateOf(mapOf<String, MealQuantityLevel>()) }
    var dinnerMealItems by remember { mutableStateOf(mapOf<String, MealQuantityLevel>()) }


    fun extractMealMap(
        records: List<MealRecordData>,
        tipo: MealType
    ): Map<String, MealQuantityLevel> {
        return records
            .firstOrNull { it.mealType == tipo }
            ?.selectedItemsWithQuantities
            ?.associate { it.itemName to it.quantityLevel }
            ?: emptyMap()
    }

    LaunchedEffect(initialMealRecords) {
        breakfastMealItems = extractMealMap(initialMealRecords, MealType.DESAYUNO)
        lunchMealItems = extractMealMap(initialMealRecords, MealType.ALMUERZO)
        dinnerMealItems = extractMealMap(initialMealRecords, MealType.CENA)
    }


    fun calculateCalories(selectedItemsWithQuantities: Map<String, MealQuantityLevel>): Int {
        var total = 0
        val baseCalories = mapOf(
            "Proteínas" to 150,
            "Carbohidratos" to 200,
            "Verduras" to 50,
            "Frutas" to 80,
            "Lácteos" to 120,
            "Grasas" to 180,
            "Azúcares" to 250,
            "Procesados" to 300
        )
        for ((itemName, quantityLevel) in selectedItemsWithQuantities) {
            val itemCalories = baseCalories[itemName] ?: 0
            val multiplier = when (quantityLevel) {
                MealQuantityLevel.PEQUEÑA -> 0.75
                MealQuantityLevel.MEDIANA -> 1.0
                MealQuantityLevel.GRANDE -> 1.25
            }
            total += (itemCalories * multiplier).toInt()
        }
        return total
    }


    val totalCaloriesToday = remember(breakfastMealItems, lunchMealItems, dinnerMealItems) {
        calculateCalories(breakfastMealItems) + calculateCalories(lunchMealItems) + calculateCalories(
            dinnerMealItems
        )
    }


    val targetCalories = remember(dailyCalorieTargetInput) {
        dailyCalorieTargetInput.toIntOrNull() ?: 0
    }

    // Calculamos el progreso para la barra visual (entre 0.0 y 1.0)
    val calorieProgress = remember(totalCaloriesToday, targetCalories) {
        if (targetCalories > 0) totalCaloriesToday.toFloat() / targetCalories.toFloat() else 0f
    }

    // CORRECCIÓN: Función para verificar si un ítem debe ser evitado (para alerta visual)
    val isItemAvoided: (String) -> Boolean = { itemName ->
        when (itemName) {
            "Carbohidratos" -> avoidFlours
            "Grasas" -> avoidFats
            "Azúcares" -> avoidSugars
            "Procesados" -> avoidSausages
            else -> false
        }
    }


    Scaffold(

        topBar = {
            // ❌ TopAppBar ELIMINADO
        },
        containerColor = colorResource(id = R.color.appBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(colorResource(id = R.color.appBackground))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ✅ Botón de regreso añadido al cuerpo, ya que eliminamos el TopAppBar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colorResource(id = R.color.primaryText)
                        )
                    }
                    Text(
                        "Dieta y Alimentos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.primaryText),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                // Acción de Ver Registros
                IconButton(onClick = { showMealRecordsDialog = true }) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = "Ver Registros",
                        tint = colorResource(id = R.color.primaryText)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- 🎯 OBJETIVO CALÓRICO (CARD) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Elevación ligeramente mayor
                shape = RoundedCornerShape(12.dp), // Esquinas más redondeadas
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(id = R.drawable.ic_calories),
                                contentDescription = "Calories",
                                modifier = Modifier.size(32.dp),
                                tint = colorResource(id = R.color.accentPinkButton)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Objetivo Calórico Diario:",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorResource(id = R.color.primaryText)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = dailyCalorieTargetInput,
                        onValueChange = { newValue ->
                            val filteredValue = newValue.filter { it.isDigit() }
                            if (filteredValue.length <= 4) {
                                dailyCalorieTargetInput = filteredValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                        textStyle = TextStyle(
                            textAlign = TextAlign.End,
                            color = colorResource(id = R.color.primaryText),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        singleLine = true,
                        suffix = { Text("Cal", fontSize = 14.sp, color = colorResource(id = R.color.textSecondary)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                            unfocusedBorderColor = colorResource(id = R.color.dividerColor),
                            cursorColor = colorResource(id = R.color.buttonPrimaryBackground),
                            focusedTextColor = colorResource(id = R.color.primaryText),
                            unfocusedTextColor = colorResource(id = R.color.primaryText),
                        )
                    )
                }
            }


            // --- REGISTRO DE COMIDAS (Encabezado) ---
            Text(
                text = "Registro de Comidas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 8.dp)
            )

            Divider(color = colorResource(id = R.color.dividerColor))
            Spacer(modifier = Modifier.height(16.dp))


            // Cards de Registro
            MealRegistrationCard(
                mealType = "Desayuno",
                mealTypeEnum = MealType.DESAYUNO,
                items = mealItems,
                selectedItemsWithQuantities = breakfastMealItems,
                onItemsChange = { newSelection -> breakfastMealItems = newSelection },
                isItemAvoided = isItemAvoided
            )
            Spacer(modifier = Modifier.height(16.dp))

            MealRegistrationCard(
                mealType = "Comida",
                mealTypeEnum = MealType.ALMUERZO,
                items = mealItems,
                selectedItemsWithQuantities = lunchMealItems,
                onItemsChange = { newSelection -> lunchMealItems = newSelection },
                isItemAvoided = isItemAvoided
            )
            Spacer(modifier = Modifier.height(16.dp))

            MealRegistrationCard(
                mealType = "Cena",
                mealTypeEnum = MealType.CENA,
                items = mealItems,
                selectedItemsWithQuantities = dinnerMealItems,
                onItemsChange = { newSelection -> dinnerMealItems = newSelection },
                isItemAvoided = isItemAvoided
            )

            Spacer(modifier = Modifier.height(32.dp))


            // --- RESUMEN CALÓRICO DIARIO (CARD MEJORADA CON BARRA) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Resumen de Calorías Diarias",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.primaryText)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // INDICADOR DE PROGRESO VISUAL
                    LinearProgressIndicator(
                        progress = { calorieProgress.coerceIn(0f, 1f) }, // Asegura que esté entre 0 y 1
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (calorieProgress > 1.1f) Color(0xFFEF5350) else colorResource(id = R.color.accentGreenButton),
                        trackColor = colorResource(id = R.color.placeholderBackground)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total hoy: ${totalCaloriesToday} Cal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(id = R.color.primaryText)
                        )
                        Text(
                            text = "Objetivo: ${targetCalories} Cal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(id = R.color.textSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val calorieStatus = when {
                        targetCalories == 0 -> "Establece un objetivo calórico para hacer seguimiento."
                        totalCaloriesToday < targetCalories * 0.9 -> "¡Necesitas más calorías para alcanzar tu objetivo!"
                        totalCaloriesToday > targetCalories * 1.1 -> "¡Has superado tu objetivo de calorías!"
                        else -> "¡Objetivo de calorías alcanzado!"
                    }
                    val statusColor = when {
                        targetCalories == 0 -> colorResource(id = R.color.textSecondary)
                        totalCaloriesToday < targetCalories * 0.9 -> Color(0xFFFFA726) // Naranja
                        totalCaloriesToday > targetCalories * 1.1 -> Color(0xFFEF5350) // Rojo
                        else -> colorResource(id = R.color.accentGreenButton)
                    }

                    Text(
                        text = calorieStatus,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))


            // --- ALIMENTOS A EVITAR (Encabezado) ---
            Text(
                text = "Mis Guías Alimentarias",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Card de Checkboxes
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AvoidFoodCheckbox(
                        label = "HARINAS REFINADAS",
                        isChecked = avoidFlours
                    ) { avoidFlours = it }
                    Divider(color = colorResource(id = R.color.dividerColor))
                    AvoidFoodCheckbox(
                        label = "GRASAS NO SALUDABLES",
                        isChecked = avoidFats
                    ) { avoidFats = it }
                    Divider(color = colorResource(id = R.color.dividerColor))
                    AvoidFoodCheckbox(
                        label = "AZÚCARES AÑADIDOS",
                        isChecked = avoidSugars
                    ) { avoidSugars = it }
                    Divider(color = colorResource(id = R.color.dividerColor))
                    AvoidFoodCheckbox(
                        label = "EMBUTIDOS / PROCESADOS",
                        isChecked = avoidSausages
                    ) { avoidSausages = it }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))


            // --- BOTONES DE ACCIÓN (MEJORADOS) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón GUARDAR (Color principal, Gradiente, Sombra)
                Button(
                    onClick = {
                        val todayDate = Date.from(
                            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                        )

                        // Lógica de Guardado (sin cambios)
                        if (breakfastMealItems.isNotEmpty()) {
                            val breakfastSelectionItems =
                                breakfastMealItems.map { (itemName, level) ->
                                    MealSelectionItem(itemName, level)
                                }
                            onSaveMealRecord(
                                MealRecordData(
                                    date = todayDate,
                                    mealType = MealType.DESAYUNO,
                                    selectedItems = breakfastMealItems.keys.toList(),
                                    selectedItemsWithQuantities = breakfastSelectionItems,
                                    actualCalories = calculateCalories(breakfastMealItems)
                                )
                            )
                        }
                        if (lunchMealItems.isNotEmpty()) {
                            val lunchSelectionItems = lunchMealItems.map { (itemName, level) ->
                                MealSelectionItem(itemName, level)
                            }
                            onSaveMealRecord(
                                MealRecordData(
                                    date = todayDate,
                                    mealType = MealType.ALMUERZO,
                                    selectedItems = lunchMealItems.keys.toList(),
                                    selectedItemsWithQuantities = lunchSelectionItems,
                                    actualCalories = calculateCalories(lunchMealItems)
                                )
                            )
                        }
                        if (dinnerMealItems.isNotEmpty()) {
                            val dinnerSelectionItems = dinnerMealItems.map { (itemName, level) ->
                                MealSelectionItem(itemName, level)
                            }
                            onSaveMealRecord(
                                MealRecordData(
                                    date = todayDate,
                                    mealType = MealType.CENA,
                                    selectedItems = dinnerMealItems.keys.toList(),
                                    selectedItemsWithQuantities = dinnerSelectionItems,
                                    actualCalories = calculateCalories(dinnerMealItems)
                                )
                            )
                        }
                        val updatedUserProfile = userProfile.copy(
                            avoidFlours = avoidFlours,
                            avoidFats = avoidFats,
                            avoidSugars = avoidSugars,
                            avoidSausages = avoidSausages,
                            dailyCalorieTarget = dailyCalorieTargetInput
                        )
                        onUpdateProfile(updatedUserProfile)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.accentGreenButton),
                        contentColor = colorResource(id = R.color.accentGreenButtonText)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp) // Sombra sutil
                ) {
                    Text(
                        "GUARDAR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Botón CANCELAR (Secundario, Outlined)
                OutlinedButton(
                    onClick = { navController.popBackStack() }, // Usamos navController.popBackStack() para volver
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    border = BorderStroke(2.dp, colorResource(id = R.color.accentPinkButton)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colorResource(id = R.color.accentPinkButton)
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "CANCELAR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

        }
        // Diálogo para ver los registros del día
        if (showMealRecordsDialog) {
            MealRecordsDialog(
                records = initialMealRecords,
                onDismiss = { showMealRecordsDialog = false }
            )
        }
    }
}

// Nuevo Composable: Contenedor para la sección de registro de comidas
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealRegistrationCard(
    mealType: String,
    mealTypeEnum: MealType,
    items: List<MealItem>,
    selectedItemsWithQuantities: Map<String, MealQuantityLevel>,
    onItemsChange: (Map<String, MealQuantityLevel>) -> Unit,
    isItemAvoided: (String) -> Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        // Incrementamos la elevación para que se sienta más profundo
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        // Mantenemos esquinas redondeadas para un look moderno
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- Encabezado de la Comida Mejorado ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                // Seleccionar un color de acento basado en el tipo de comida (ejemplo)
                val mealColor = when (mealTypeEnum) {
                    MealType.DESAYUNO -> Color(0xFFFFA726) // Naranja
                    MealType.ALMUERZO -> Color(0xFF66BB6A)  // Verde
                    MealType.CENA -> Color(0xFF42A5F5)    // Azul
                }

                // Icono decorativo (asume un recurso genérico de comida)
                Icon(
                    painter = painterResource(id = R.drawable.ic_restaurant_menu), // Usar un icono de menú genérico
                    contentDescription = null,
                    tint = mealColor,
                    modifier = Modifier.size(28.dp).padding(end = 8.dp)
                )

                Text(
                    text = mealType,
                    fontSize = 22.sp, // Fuente un poco más grande
                    fontWeight = FontWeight.ExtraBold, // Mayor peso de fuente
                    color = colorResource(id = R.color.primaryText)
                )
            }

            // Separador visualmente limpio
            Divider(color = colorResource(id = R.color.dividerColor))
            Spacer(modifier = Modifier.height(12.dp))

            // Se usa el componente original con la nueva lógica de visualización
            MealInputCardContent(
                items = items,
                selectedItemsWithQuantities = selectedItemsWithQuantities,
                onItemsChange = onItemsChange,
                isItemAvoided = isItemAvoided
            )
        }
    }
}

// Contenido original de MealInputCardWithQuantity extraído para usar en el nuevo contenedor
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealInputCardContent(
    items: List<MealItem>,
    selectedItemsWithQuantities: Map<String, MealQuantityLevel>,
    onItemsChange: (Map<String, MealQuantityLevel>) -> Unit,
    isItemAvoided: (String) -> Boolean
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Mayor espaciado vertical
    ) {
        items.forEach { mealItem ->
            val currentQuantity = selectedItemsWithQuantities[mealItem.name]
            val isSelected = currentQuantity != null
            val avoided = isItemAvoided(mealItem.name)

            // --- 1. Contenedor Principal del Item ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(90.dp)
            ) {

                // --- 2. Caja de Icono (Clickable) ---
                Box(
                    modifier = Modifier
                        .size(75.dp) // Tamaño ligeramente mayor
                        .clip(RoundedCornerShape(16.dp)) // Esquinas más redondeadas
                        .background(
                            when {
                                avoided -> Color.Red.copy(alpha = 0.15f) // Más color para Evitar
                                isSelected -> colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.2f)
                                else -> colorResource(id = R.color.placeholderBackground)
                            }
                        )
                        .border(
                            2.dp, // Borde más grueso
                            when {
                                avoided -> Color.Red
                                isSelected -> colorResource(id = R.color.buttonPrimaryBackground)
                                else -> colorResource(id = R.color.dividerColor).copy(alpha = 0.5f)
                            },
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onItemsChange(
                                if (isSelected) {
                                    selectedItemsWithQuantities.toMutableMap()
                                        .apply { remove(mealItem.name) }
                                } else {
                                    selectedItemsWithQuantities.toMutableMap().apply {
                                        put(
                                            mealItem.name,
                                            MealQuantityLevel.MEDIANA
                                        ) // Default a Mediana
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val iconId = when (mealItem.name) {
                        "Proteínas" -> R.drawable.ic_protein
                        "Carbohidratos" -> R.drawable.ic_carbs
                        "Verduras" -> R.drawable.ic_vegetables
                        "Frutas" -> R.drawable.ic_fruits
                        "Lácteos" -> R.drawable.ic_dairy
                        "Grasas" -> R.drawable.ic_fat
                        "Azúcares" -> R.drawable.ic_sugar
                        "Procesados" -> R.drawable.ic_processed_food
                        else -> R.drawable.ic_restaurant_menu
                    }
                    Icon(
                        painterResource(id = iconId), contentDescription = mealItem.name,
                        modifier = Modifier.size(44.dp), // Icono más grande
                        tint = if (avoided) Color.Red else colorResource(id = R.color.primaryText)
                    )

                    // Indicador de "Seleccionado" en el círculo
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = colorResource(id = R.color.accentGreenButton),
                            modifier = Modifier
                                .size(28.dp) // Icono de check más grande
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp) // Pequeño offset para que no toque el borde
                        )
                    }

                    // Etiqueta ¡EVITAR! superpuesta en el fondo rojo
                    if (avoided) {
                        Text(
                            text = "¡Evitar!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.8f))
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp)) // Espaciado mayor

                // --- 3. Nombre del Ítem ---
                Text(
                    text = mealItem.name,
                    fontSize = 13.sp, // Fuente un poco más grande
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = R.color.primaryText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // --- 4. Selectores de Cantidad ---
                if (isSelected && !avoided) { // No mostrar selector si es evitado
                    Spacer(modifier = Modifier.height(10.dp)) // Espaciado mayor
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MealQuantityLevel.entries.forEach { level ->
                            val isCurrentLevel = currentQuantity == level

                            if (isCurrentLevel) {
                                // Opción Seleccionada (Botón de color sólido)
                                Button(
                                    onClick = {
                                        onItemsChange(
                                            selectedItemsWithQuantities.toMutableMap().apply {
                                                put(mealItem.name, level)
                                            })
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .padding(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.buttonPrimaryBackground)
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when (level) {
                                            MealQuantityLevel.PEQUEÑA -> "P"
                                            MealQuantityLevel.MEDIANA -> "M"
                                            MealQuantityLevel.GRANDE -> "G"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onItemsChange(
                                            selectedItemsWithQuantities.toMutableMap().apply {
                                                put(mealItem.name, level)
                                            })
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .padding(horizontal = 2.dp),
                                    border = BorderStroke(1.dp, colorResource(id = R.color.textHint)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = colorResource(id = R.color.primaryText)
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when (level) {
                                            MealQuantityLevel.PEQUEÑA -> "Peq"
                                            MealQuantityLevel.MEDIANA -> "Med"
                                            MealQuantityLevel.GRANDE -> "Gra"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun RecordItem(record: MealRecordData) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormatter.format(record.date)
    val capitalizedMealType = when (record.mealType) {
        MealType.ALMUERZO -> "Comida"
        else -> record.mealType.name.lowercase(Locale.ROOT)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.placeholderBackground))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila Superior: Tipo de Comida y Hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = capitalizedMealType,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = colorResource(id = R.color.buttonPrimaryBackground)
                )
                Text(
                    text = formattedTime,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.textSecondary)
                )
            }
            Divider(color = colorResource(id = R.color.dividerColor).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
            Text(
                text = "🔥 Calorías: ${record.actualCalories} Cal",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            val itemsList = record.selectedItemsWithQuantities.joinToString(", ") { (name, level) ->
                val levelText = when (level) {
                    MealQuantityLevel.PEQUEÑA -> "Peq"
                    MealQuantityLevel.MEDIANA -> "Med"
                    MealQuantityLevel.GRANDE -> "Grande"
                }
                "$name ($levelText)"
            }
            Text(
                text = "Items: $itemsList",
                fontSize = 13.sp,
                color = colorResource(id = R.color.textSecondary)
            )
        }
    }
}

@Composable
fun MealRecordsDialog(records: List<MealRecordData>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📋 Registros de Comida",
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText),
                fontSize = 20.sp
            )
        },
        text = {
            if (records.isEmpty()) {
                Text(
                    "Aún no tienes registros de comidas para el día de hoy. ¡Empieza a registrar!",
                    color = colorResource(id = R.color.textSecondary)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Mayor espaciado entre cards
                ) {
                    items(records) { record ->
                        RecordItem(record) // Usamos el componente visual mejorado
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.accentGreenButton),
                    contentColor = colorResource(id = R.color.accentGreenButtonText)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cerrar")
            }
        },
        containerColor = colorResource(id = R.color.cardBackground), // Fondo del diálogo
        shape = RoundedCornerShape(20.dp) // Esquinas redondeadas del diálogo
    )
}

@Composable
fun AvoidFoodCheckbox(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // Usamos el color de fondo para darle un toque visual al ser seleccionado
    val backgroundColor = if (isChecked) {
        colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)) // Esquinas redondeadas para el área clickeable
            .background(backgroundColor)
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 12.dp, horizontal = 8.dp) // Relleno interno
    ) {

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                // Usamos el color de acento principal para el check
                checkedColor = colorResource(id = R.color.accentPinkButton),
                uncheckedColor = colorResource(id = R.color.textSecondary),
                checkmarkColor = colorResource(id = R.color.cardBackground) // Checkmark blanco
            ),
            modifier = Modifier.size(24.dp) // Tamaño estándar
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, // Texto en negrita
            color = colorResource(id = R.color.primaryText)
        )
    }
}
@Composable
fun RecordCategorySection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.primaryText),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
@Composable
fun SelectableItem(item: SelectableRecordItem, onItemClick: (SelectableRecordItem) -> Unit) {
    val isSelected = item.isSelected
    val backgroundColor = if (isSelected) {
        colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.2f)
    } else {
        colorResource(id = R.color.placeholderBackground)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp) // Ancho fijo
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                1.5.dp,
                if (isSelected) colorResource(id = R.color.buttonPrimaryBackground) else colorResource(id = R.color.dividerColor),
                RoundedCornerShape(12.dp)
            )
            .clickable { onItemClick(item) }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        // ✅ CORRECCIÓN DE USO DE iconId
        Icon(
            painter = painterResource(id = item.iconId),
            contentDescription = item.name,
            modifier = Modifier.size(36.dp),
            tint = if (isSelected) colorResource(id = R.color.buttonPrimaryBackground) else colorResource(id = R.color.textSecondary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = colorResource(id = R.color.primaryText),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyRecordScreen(
    navController: NavController,
    onSaveRecord: (DailyRecordData) -> Unit,
    onBackClick: () -> Unit,
    initialDate: LocalDate? = null,
    initialRecordToEdit: DailyRecordData? = null // Objeto de registro completo para edición
) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val firestoreDb = FirebaseFirestore.getInstance()
    val recordDate = remember {
        mutableStateOf(initialDate ?: initialRecordToEdit?.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now())
    }
    val userId = firebaseAuth.currentUser?.uid ?: ""
    val initialGlucose = initialRecordToEdit?.glucoseReadings?.lastOrNull()?.value?.toString() ?: ""
    val initialSistolica = initialRecordToEdit?.bloodPressureReadings?.lastOrNull()?.sistolica?.toString() ?: ""
    val initialDiastolica = initialRecordToEdit?.bloodPressureReadings?.lastOrNull()?.diastolica?.toString() ?: ""
    val initialPulso = initialRecordToEdit?.bloodPressureReadings?.lastOrNull()?.pulso?.toString() ?: ""
    val initialMoment = initialRecordToEdit?.glucoseReadings?.lastOrNull()?.moment ?: "Ayunas"
    var glucoseValue by remember { mutableStateOf(initialGlucose) }
    var sistolica by remember { mutableStateOf(initialSistolica) }
    var diastolica by remember { mutableStateOf(initialDiastolica) }
    var pulso by remember { mutableStateOf(initialPulso) }
    var activityTimeInput by remember { mutableStateOf(initialRecordToEdit?.activityTime ?: "") }
    var notes by remember { mutableStateOf(initialRecordToEdit?.notes ?: "") }
    var measurementMomentExpanded by remember { mutableStateOf(false) }
    val measurementMoments = listOf("Ayunas", "Comida", "Cena", "Casual")
    var selectedMeasurementMoment by remember { mutableStateOf(initialMoment) }
    var showMoreDetails by remember {
        mutableStateOf(initialRecordToEdit != null &&
                (initialRecordToEdit.symptoms.isNotEmpty() ||
                        initialRecordToEdit.activities.isNotEmpty() ||
                        initialRecordToEdit.notes?.isNotBlank() == true))
    }

    // ✅ CORRECCIÓN: Se eliminan los Painters y se usan directamente los IDs
    val initSelectableState: (List<SelectableRecordItem>, List<String>) -> List<SelectableRecordItem> = { currentItems, savedNames ->
        currentItems.map { item ->
            when (item) {
                is SymptomItem -> item.copy(isSelected = savedNames.contains(item.name))
                is ActivityItem -> item.copy(isSelected = savedNames.contains(item.name))
                is FoodTypeItem -> item.copy(isSelected = savedNames.contains(item.name))
                else -> item
            } as SelectableRecordItem
        }
    }

    var selectableSymptoms by remember {
        mutableStateOf(
            initSelectableState(
                listOf(
                    SymptomItem(R.drawable.ic_thumb_up, "Estoy bien"),
                    SymptomItem(R.drawable.ic_dizzy_face, "Mareos"),
                    SymptomItem(R.drawable.ic_headache, "Dolor de cabeza"),
                    SymptomItem(R.drawable.ic_blur_vision, "Visión borrosa"),
                    SymptomItem(R.drawable.ic_ear_ringing, "Zumbido en oídos"),
                    SymptomItem(R.drawable.ic_chest_pain, "Dolor en el pecho"),
                    SymptomItem(R.drawable.ic_fatigue, "Fatiga"),
                    SymptomItem(R.drawable.ic_nosebleed, "Sangrado nasal"),
                    SymptomItem(R.drawable.ic_breathing_difficulty, "Dificultad para respirar"),
                    SymptomItem(R.drawable.ic_nausea, "Náuseas"),
                    SymptomItem(R.drawable.ic_vomiting, "Vómitos")
                ),
                initialRecordToEdit?.symptoms ?: emptyList()
            ).map { it as SymptomItem }
        )
    }

    var selectableActivities by remember {
        mutableStateOf(
            initSelectableState(
                listOf(
                    ActivityItem(R.drawable.ic_footsteps, "Correr"),
                    ActivityItem(R.drawable.ic_walking, "Caminar"),
                    ActivityItem(R.drawable.ic_gym, "Gym"),
                    ActivityItem(R.drawable.ic_cycling, "Ciclismo"),
                    ActivityItem(R.drawable.ic_home_exercise, "Ejercicios en casa"),
                    ActivityItem(R.drawable.ic_jump_rope, "Cuerda"),
                    ActivityItem(R.drawable.ic_yoga, "Yoga"),
                    ActivityItem(R.drawable.ic_add_circle, "Otro")
                ),
                // CORRECCIÓN AQUÍ: Filtramos para que solo pasen los Strings a la UI
                initialRecordToEdit?.activities?.filterIsInstance<String>() ?: emptyList()
            ).map { it as ActivityItem }
        )
    }
    var selectableFoodTypes by remember {
        mutableStateOf(
            initSelectableState(
                listOf(
                    FoodTypeItem(R.drawable.balanced_diet, "Dieta balanceada"),
                    FoodTypeItem(R.drawable.ic_burger, "Comida rápida"),
                    FoodTypeItem(R.drawable.ic_chinese_food, "Comida china"),
                    FoodTypeItem(R.drawable.ic_flour, "Harinas"),
                    FoodTypeItem(R.drawable.ic_meat, "Carnes"),
                    FoodTypeItem(R.drawable.ic_salt, "Añade sal extra"),
                    FoodTypeItem(R.drawable.ic_spices, "Especias")
                ),
                initialRecordToEdit?.foodTypes ?: emptyList()
            ).map { it as FoodTypeItem }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colorResource(id = R.color.primaryText),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = colorResource(id = R.color.primaryText)
                ),
                modifier = Modifier
                    .height(60.dp)
                    .padding(start = 16.dp, top = 4.dp)
            )
        },
        containerColor = colorResource(id = R.color.appBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(colorResource(id = R.color.appBackground))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if(initialRecordToEdit != null) "EDICIÓN DE MEDICIÓN" else "DESGLOSE PARA EL REGISTRO POR DIA",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold, // Más énfasis
                color = colorResource(id = R.color.primaryText),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Text(
                text = "Fecha: ${
                    recordDate.value.format(
                        DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy",
                            Locale("es", "ES")
                        )
                    )
                }",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.textSecondary),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.vitalsCardBackground)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Mayor elevación
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp) // Mayor padding
                ) {
                    Text("Nivel de Glucosa", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorResource(id = R.color.vitalsTextPrimary))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), // Mayor espaciado
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = glucoseValue,
                            onValueChange = { newValue ->
                                if (newValue.matches(Regex("^\\d*\$"))) {
                                    glucoseValue = newValue
                                }
                            },
                            label = { Text("Glucosa (mg/dL)", color = colorResource(id = R.color.vitalsTextSecondary)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(fontSize = 24.sp, color = colorResource(id = R.color.vitalsTextPrimary), fontWeight = FontWeight.Bold), // Texto más grande
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colorResource(id = R.color.vitalsTextPrimary), unfocusedTextColor = colorResource(id = R.color.vitalsTextPrimary),
                                cursorColor = colorResource(id = R.color.vitalsTextPrimary), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground), // Color de acento
                                unfocusedBorderColor = colorResource(id = R.color.vitalsTextSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                                unfocusedLabelColor = colorResource(id = R.color.vitalsTextSecondary)
                            ),
                            modifier = Modifier.weight(0.55f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = measurementMomentExpanded,
                            onExpandedChange = { measurementMomentExpanded = !measurementMomentExpanded },
                            modifier = Modifier.weight(0.45f)
                        ) {
                            OutlinedTextField(
                                value = selectedMeasurementMoment,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Momento") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = measurementMomentExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxHeight(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colorResource(id = R.color.vitalsTextPrimary), unfocusedTextColor = colorResource(id = R.color.vitalsTextPrimary),
                                    cursorColor = colorResource(id = R.color.vitalsTextPrimary), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground), // Color de acento
                                    unfocusedBorderColor = colorResource(id = R.color.vitalsTextSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                                    unfocusedLabelColor = colorResource(id = R.color.vitalsTextSecondary)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = measurementMomentExpanded,
                                onDismissRequest = { measurementMomentExpanded = false },
                                modifier = Modifier.background(colorResource(id = R.color.cardBackground))
                            ) {
                                measurementMoments.forEach { moment ->
                                    DropdownMenuItem(
                                        text = { Text(moment, color = colorResource(id = R.color.primaryText)) },
                                        onClick = {
                                            selectedMeasurementMoment = moment
                                            measurementMomentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Divider(color = colorResource(id = R.color.vitalsTextSecondary).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 20.dp)) // Separador
                    Text("Presión Arterial y Pulso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorResource(id = R.color.vitalsTextPrimary))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = sistolica,
                            onValueChange = { newValue -> if (newValue.matches(Regex("^\\d*\$"))) { sistolica = newValue } },
                            label = { Text("Sistólica", color = colorResource(id = R.color.vitalsTextSecondary)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(fontSize = 20.sp, color = colorResource(id = R.color.vitalsTextPrimary), fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colorResource(id = R.color.vitalsTextPrimary), unfocusedTextColor = colorResource(id = R.color.vitalsTextPrimary),
                                cursorColor = colorResource(id = R.color.vitalsTextPrimary), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                                unfocusedBorderColor = colorResource(id = R.color.vitalsTextSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                                unfocusedLabelColor = colorResource(id = R.color.vitalsTextSecondary)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value = diastolica,
                            onValueChange = { newValue -> if (newValue.matches(Regex("^\\d*\$"))) { diastolica = newValue } },
                            label = { Text("Diastólica", color = colorResource(id = R.color.vitalsTextSecondary)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(fontSize = 20.sp, color = colorResource(id = R.color.vitalsTextPrimary), fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colorResource(id = R.color.vitalsTextPrimary), unfocusedTextColor = colorResource(id = R.color.vitalsTextPrimary),
                                cursorColor = colorResource(id = R.color.vitalsTextPrimary), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                                unfocusedBorderColor = colorResource(id = R.color.vitalsTextSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                                unfocusedLabelColor = colorResource(id = R.color.vitalsTextSecondary)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("mm/HG", fontSize = 14.sp, color = colorResource(id = R.color.vitalsTextPrimary), modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pulso,
                        onValueChange = { newValue -> if (newValue.matches(Regex("^\\d*\$"))) { pulso = newValue } },
                        label = { Text("Pulso (LPM)", color = colorResource(id = R.color.vitalsTextSecondary)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 20.sp, color = colorResource(id = R.color.vitalsTextPrimary), fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorResource(id = R.color.vitalsTextPrimary), unfocusedTextColor = colorResource(id = R.color.vitalsTextPrimary),
                            cursorColor = colorResource(id = R.color.vitalsTextPrimary), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                            unfocusedBorderColor = colorResource(id = R.color.vitalsTextSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                            unfocusedLabelColor = colorResource(id = R.color.vitalsTextSecondary)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // --- BOTÓN MÁS DETALLES MEJORADO (Toggle) ---
            Button(
                onClick = { showMoreDetails = !showMoreDetails },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.accentPinkButton)),
                shape = RoundedCornerShape(12.dp) // Esquinas más redondeadas
            ) {
                Text(
                    if (showMoreDetails) "Ocultar Detalles (Síntomas, Actividad, Notas)" else "Mostrar Más Detalles (Síntomas, Actividad, Notas)",
                    color = colorResource(id = R.color.accentPinkButtonText),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (showMoreDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (showMoreDetails) "Ocultar" else "Mostrar",
                    tint = colorResource(id = R.color.accentPinkButtonText),
                    modifier = Modifier.padding(start = 8.dp).size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (showMoreDetails) {
                // --- SECCIÓN DETALLES (Usando RecordCategorySection) ---
                RecordCategorySection(title = "Síntomas Reportados 🤕") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            selectableSymptoms.forEach { item ->
                                // ✅ CORRECCIÓN DE USO DE iconId
                                SelectableItem(
                                    item = item,
                                    onItemClick = { selectedItem ->
                                        selectableSymptoms = selectableSymptoms.toMutableList().apply {
                                            val index = indexOfFirst { it.name == selectedItem.name }
                                            if (index != -1) {
                                                val updated = (selectedItem as SymptomItem)
                                                    .copy(isSelected = !selectedItem.isSelected)
                                                this[index] = updated
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                RecordCategorySection(title = "Actividad Física y Duración 🏃") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                selectableActivities.forEach { item ->
                                    // ✅ CORRECCIÓN DE USO DE iconId
                                    SelectableItem(
                                        item = item,
                                        onItemClick = { selectedItem ->
                                            if (selectedItem is ActivityItem) {
                                                val updated = selectedItem.copy(isSelected = !selectedItem.isSelected)
                                                selectableActivities = selectableActivities.toMutableList().apply {
                                                    val index = indexOfFirst { it.name == selectedItem.name }
                                                    if (index != -1) {
                                                        this[index] = updated
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // Campo de Duración mejorado
                            OutlinedTextField(
                                value = activityTimeInput,
                                onValueChange = { newValue -> activityTimeInput = newValue },
                                label = { Text("Duración (ej. 1h 30m)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(textAlign = TextAlign.Start, color = colorResource(id = R.color.primaryText)),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colorResource(id = R.color.primaryText), unfocusedTextColor = colorResource(id = R.color.primaryText),
                                    cursorColor = colorResource(id = R.color.buttonPrimaryBackground), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                                    unfocusedBorderColor = colorResource(id = R.color.textSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                                    unfocusedLabelColor = colorResource(id = R.color.textSecondary)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Campo de Notas mejorado
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas Adicionales / Observaciones Clínicas 📝") },
                    placeholder = { Text("Ej: Me sentí mareado después de caminar. (Estas notas se actualizarán durante el día)", color = colorResource(id = R.color.textHint)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorResource(id = R.color.primaryText), unfocusedTextColor = colorResource(id = R.color.primaryText),
                        cursorColor = colorResource(id = R.color.buttonPrimaryBackground), focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                        unfocusedBorderColor = colorResource(id = R.color.textSecondary), focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
                        unfocusedLabelColor = colorResource(id = R.color.textSecondary)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            // --- BOTÓN DE GUARDAR MEJORADO ---
            Button(
                onClick = {
                    val currentDateTime = Date()
                    val recordDateLocal = recordDate.value
                    val userId = firebaseAuth.currentUser?.uid ?: ""

                    // Determinar ID del documento para Signos Vitales
                    val vitalsDocId = if (initialRecordToEdit != null) {
                        initialRecordToEdit.docId ?: "${recordDateLocal}_TAKE_${LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))}"
                    } else {
                        "${recordDateLocal}_TAKE_${LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))}"
                    }

                    val vitalsDocRef = firestoreDb.collection("users").document(userId)
                        .collection("dailyRecords").document(vitalsDocId)

                    var vitalsSaved = false

                    // --- 1. Guardar Signos Vitales (Glucosa, Presión, Pulso) ---
                    if (glucoseValue.isNotBlank() || (sistolica.isNotBlank() && diastolica.isNotBlank()) || pulso.isNotBlank()) {
                        val dateForRecord = initialRecordToEdit?.date ?: currentDateTime
                        val newVitalsRecord = DailyRecordData(
                            date = dateForRecord,
                            docId = vitalsDocId,
                            glucoseReadings = if (glucoseValue.isNotBlank()) {
                                listOf(GlucoseReading(glucoseValue.toIntOrNull() ?: 0, selectedMeasurementMoment))
                            } else emptyList(),
                            bloodPressureReadings = if (sistolica.isNotBlank() && diastolica.isNotBlank()) {
                                listOf(
                                    BloodPressureReading(
                                        sistolica = sistolica.toIntOrNull() ?: 0,
                                        diastolica = diastolica.toIntOrNull() ?: 0,
                                        pulso = pulso.toIntOrNull() ?: 0,
                                        moment = selectedMeasurementMoment
                                    )
                                )
                            } else emptyList()
                        )

                        vitalsDocRef.set(newVitalsRecord, SetOptions.merge())
                            .addOnSuccessListener {
                                vitalsSaved = true
                                Log.d("DailyRecord", "Vitals guardados exitosamente")
                            }
                    }

                    // --- 2. Guardar Detalles (Síntomas, Actividad, Notas) ---
                    val detailsDocId = "${recordDateLocal}_DETAILS"
                    val detailsDocRef = firestoreDb.collection("users").document(userId)
                        .collection("dailyRecords").document(detailsDocId)

                    val selectedSymptoms = selectableSymptoms.filter { it.isSelected }.map { it.name }
                    val selectedActivities = selectableActivities.filter { it.isSelected }.map { it.name }

                    val detailsRecord = DailyRecordData(
                        date = Date.from(recordDateLocal.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                        docId = detailsDocId,
                        symptoms = selectedSymptoms,
                        activities = selectedActivities,
                        activityTime = activityTimeInput.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )

                    detailsDocRef.set(detailsRecord, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Registro guardado correctamente", Toast.LENGTH_SHORT).show()

                            // ✅ REDIRECCIÓN DEFINITIVA A INICIO
                            // Esto evita que regreses a una pantalla vacía
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.accentGreenButton),
                    contentColor = colorResource(id = R.color.accentGreenButtonText)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "GUARDAR REGISTRO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
@Composable
fun <T : SelectableRecordItem> SelectableItem(
    item: T,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = item.isSelected
    // Color de acento para la selección (ej. el color del check y borde)
    val accentColor = colorResource(id = R.color.accentGreenButton) // Usar un color de acento para ítems seleccionables
    // --- 1. Contenedor Principal ---
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(85.dp) // Ancho un poco mayor para mejor toque y texto
            .clip(RoundedCornerShape(16.dp))
            .clickable { onItemClick(item) }
            .padding(vertical = 4.dp)
    ) {
        // --- 2. Caja de Ícono y Selección ---
        Box(
            modifier = Modifier
                .size(75.dp) // Icono más grande
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) accentColor.copy(alpha = 0.15f) // Fondo más claro y visible
                    else colorResource(id = R.color.placeholderBackground)
                )
                .border(
                    2.dp, // Borde más grueso
                    if (isSelected) accentColor else colorResource(id = R.color.dividerColor).copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // ✅ CORRECCIÓN CLAVE: Usar item.iconId y painterResource
            Icon(
                painter = painterResource(id = item.iconId),
                contentDescription = item.name,
                modifier = Modifier.size(42.dp), // Ícono más grande dentro de la caja
                tint = if (isSelected) accentColor else colorResource(id = R.color.primaryText)
            )
            // Indicador de "Seleccionado"
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = accentColor, // El check usa el mismo color de acento
                    modifier = Modifier
                        .size(28.dp) // Check más grande
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp) // Pequeño offset para mayor visibilidad
                )
            }
        }
        // --- 3. Etiqueta de Texto ---
        Spacer(modifier = Modifier.height(6.dp)) // Espaciado mayor
        Text(
            text = item.name,
            fontSize = 13.sp, // Fuente un poco más grande
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, // Negrita al seleccionar
            textAlign = TextAlign.Center,
            color = colorResource(id = R.color.primaryText),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Diabetes_AppTheme {
        MainScreen(
            firebaseAuth = FirebaseAuth.getInstance(),
            firestoreDb = FirebaseFirestore.getInstance(),
            initialDiagnosis = "Diabetes",
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MedicationScreenPreview() {
    Diabetes_AppTheme {
        val dummyMedications = remember {
            mutableStateListOf(
                // Usamos docId con un String, simulando el ID que daría Firebase
                MedicationData(docId = "id_1", name = "Losartán", dose = 50, unit = "mg", time = "13:00", frequency = "Cada 8 hr"),
                MedicationData(docId = "id_2", name = "Insulina Lispro", dose = 10, unit = "UI", time = "08:00", frequency = "Antes de cada comida"),
                MedicationData(docId = "id_3", name = "Metformina", dose = 850, unit = "mg", time = "21:00", frequency = "Cada 24 hr")
            )
        }
        MedicationScreen(
            navController = rememberNavController(),
            medications = dummyMedications,
            dosageTakenRecords = emptyList(),
            onAddMedicationClick = {},
            onEditMedication = {},
            onRemoveMedication = {},
            onMarkDoseTaken = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditMedicationScreenPreview() {
    Diabetes_AppTheme {
        AddEditMedicationScreen(
            navController = rememberNavController(),
            onSaveMedication = {},
            onCancel = {}
        )
    }
}
@Preview(showBackground = true)
@Composable
fun DailyRecordScreenPreview() {
    DailyRecordScreen(
        navController = rememberNavController(),
        onSaveRecord = {},
        onBackClick = {}
    )
}