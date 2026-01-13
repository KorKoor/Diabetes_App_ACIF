package com.example.diabetes_app.ui.profile

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.diabetes_app.data.UserProfile
import com.example.diabetes_app.R
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.io.File
import android.util.Log // Importar para logs de depuración
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import java.io.FileOutputStream


// --- FUNCIONES AUXILIARES (ASUMIMOS DEFINICIÓN EXTERNA O EN EL MISMO ARCHIVO) ---

// Función para guardar la imagen localmente (manteniendo la tuya)
fun saveImageLocally(context: Context, uri: Uri, uid: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_${uid}_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.toURI().toString()
    } catch (e: Exception) {
        Log.e("ProfileScreen", "Error saving image locally: ${e.message}", e)
        null
    }
}

// --- NUEVO COMPONENTE: Tarjeta de Sección Unificada ---
@Composable
fun ProfileSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.buttonPrimaryBackground), // Título con color de acento
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

// --- DIÁLOGO DE EDICIÓN DE NOMBRE MEJORADO ---
@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorResource(id = R.color.cardBackground),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "Editar Nombre",
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.primaryText)
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colorResource(id = R.color.primaryText),
                    cursorColor = colorResource(id = R.color.buttonPrimaryBackground),
                    focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
                    unfocusedBorderColor = colorResource(id = R.color.dividerColor)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(newName.trim())
                    onDismiss()
                },
                enabled = newName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.accentGreenButton))
            ) {
                Text("Guardar", color = colorResource(id = R.color.accentGreenButtonText))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.placeholderBackground))
            ) {
                Text("Cancelar", color = colorResource(id = R.color.textSecondary))
            }
        }
    )
}


// --- COMPONENTE PRINCIPAL MEJORADO: ProfileScreen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    firebaseAuth: FirebaseAuth,
    firestoreDb: FirebaseFirestore,
    initialDiagnosis: String?,
    onLogoutClick: () -> Unit,
    navController: NavController,
) {
    val context = LocalContext.current
    var userProfile by remember {
        val currentUser = firebaseAuth.currentUser
        mutableStateOf(UserProfile(uid = currentUser?.uid ?: ""))
    }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var tempUserName by remember { mutableStateOf("") }
    var isInitialRegistration by remember { mutableStateOf(false) }

    var weightInputString by remember { mutableStateOf("") }
    var heightInputString by remember { mutableStateOf("") }
    var phoneInputString by remember { mutableStateOf("") }
    var dateOfBirthInput: LocalDate? by remember { mutableStateOf(null) }

    // --- Lógica de Launchers y LaunchedEffects (Mantenida) ---

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localFilePath = saveImageLocally(context, uri, userProfile.uid)
            if (localFilePath != null) {
                userProfile = userProfile.copy(photoUrl = localFilePath)
                Toast.makeText(context, "Foto seleccionada. Pulsa 'Guardar Perfil'.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error al guardar la foto.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestoreDb.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val loadedProfile = document.toObject(UserProfile::class.java) ?: UserProfile(uid = currentUser.uid)

                    if (loadedProfile.weight == 0.0 && loadedProfile.height == 0.0) {
                        isInitialRegistration = true
                    }

                    userProfile = loadedProfile.copy(
                        uid = currentUser.uid,
                        name = loadedProfile.name.ifEmpty { currentUser.displayName ?: "Usuario" },
                        email = loadedProfile.email.ifEmpty { currentUser.email ?: "" },
                        photoUrl = loadedProfile.photoUrl?.ifEmpty { currentUser.photoUrl?.toString() } ?: currentUser.photoUrl?.toString()
                    )

                    if (userProfile.condition.isEmpty() && initialDiagnosis != null) {
                        userProfile = userProfile.copy(condition = initialDiagnosis)
                    }

                    weightInputString = if (userProfile.weight == 0.0) "" else userProfile.weight.toString()
                    heightInputString = if (userProfile.height == 0.0) "" else userProfile.height.toString()
                    phoneInputString = userProfile.phone

                    userProfile.dateOfBirth?.let { dobDate ->
                        dateOfBirthInput = dobDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    tempUserName = userProfile.name
                }
                .addOnFailureListener { e ->
                    // Manejo de errores
                }
        } else {
            // Manejo de usuario no autenticado
        }
    }

    LaunchedEffect(userProfile.weight, userProfile.height) {
        val weightKg = userProfile.weight
        val heightM = userProfile.height

        if (weightKg > 0.0 && heightM > 0) {
            val bmiValue = weightKg / (heightM * heightM)
            val bmiCategory = when {
                bmiValue < 18.5 -> "Bajo peso."
                bmiValue < 25.0 -> "Peso normal/saludable."
                bmiValue < 30.0 -> "Sobrepeso."
                else -> "Obesidad."
            }
            userProfile = userProfile.copy(bmi = bmiValue, bmiCategory = bmiCategory)
        } else {
            userProfile = userProfile.copy(bmi = 0.0, bmiCategory = "N/A")
        }
    }

    LaunchedEffect(dateOfBirthInput) {
        dateOfBirthInput?.let { dob ->
            val today = LocalDate.now()
            val age = Period.between(dob, today).years
            userProfile = userProfile.copy(age = age, dateOfBirth = Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        } ?: run {
            userProfile = userProfile.copy(age = 0, dateOfBirth = null)
        }
    }

    // --- Colores para los TextField (Aseguramos la coherencia visual) ---
    @Composable
    fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colorResource(id = R.color.primaryText),
        unfocusedTextColor = colorResource(id = R.color.primaryText),
        cursorColor = colorResource(id = R.color.buttonPrimaryBackground),
        focusedBorderColor = colorResource(id = R.color.buttonPrimaryBackground),
        unfocusedBorderColor = colorResource(id = R.color.dividerColor),
        focusedLabelColor = colorResource(id = R.color.buttonPrimaryBackground),
        unfocusedLabelColor = colorResource(id = R.color.textSecondary),
        focusedLeadingIconColor = colorResource(id = R.color.buttonPrimaryBackground),
        unfocusedLeadingIconColor = colorResource(id = R.color.textSecondary),
        focusedTrailingIconColor = colorResource(id = R.color.buttonPrimaryBackground),
        unfocusedTrailingIconColor = colorResource(id = R.color.textSecondary),
    )


    Scaffold(
        // ❌ TopAppBar ELIMINADO
        containerColor = colorResource(id = R.color.appBackground) // Fondo principal
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ✅ BARRA DE TÍTULO Y REGRESO MANUAL (Estilo Cohesivo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                // El botón de regreso ahora llama directamente al navController
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = colorResource(id = R.color.headerBackground)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Mi Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorResource(id = R.color.primaryText),
                    fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Tarjeta de Foto y Nombre MEJORADA (Diseño Limpio) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Fondo superior decorativo
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.8f),
                                        colorResource(id = R.color.buttonPrimaryBackground)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            )
                    )

                    // Contenedor de Foto
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .offset(y = (-65).dp) // Superposición controlada
                            .clip(CircleShape)
                            .background(colorResource(id = R.color.cardBackground))
                            .border(4.dp, colorResource(id = R.color.cardBackground), CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = if (userProfile.photoUrl?.startsWith("file://") == true) {
                            Uri.parse(userProfile.photoUrl)
                        } else {
                            userProfile.photoUrl
                        }

                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Foto de perfil del usuario",
                            placeholder = painterResource(id = R.drawable.ic_default_profile),
                            error = painterResource(id = R.drawable.ic_default_profile),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                        // Icono de Cámara
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Cambiar foto de perfil",
                            tint = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.BottomEnd)
                                .background(colorResource(id = R.color.buttonPrimaryBackground), CircleShape)
                                .padding(8.dp)
                        )
                    }

                    // Nombre y Botón de Edición
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (-50).dp) // Compensar el offset de la foto
                    ) {
                        Text(
                            text = tempUserName, // Usar tempUserName para edición
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorResource(id = R.color.primaryText),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                        )
                        Text(
                            text = "Toca para cambiar nombre",
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.buttonPrimaryBackground),
                            modifier = Modifier
                                .clickable { showNameEditDialog = true }
                                .padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Email (Solo lectura, para referencia)
                        Text(
                            text = userProfile.email,
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.textSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }


            // --- Sección Datos Biomédicos ---
            ProfileSectionCard(title = "Datos Biomédicos 📊") {
                // Campos de Peso y Talla
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = weightInputString,
                        onValueChange = { newValue ->
                            val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                            if (filteredValue.count { it == '.' } <= 1) {
                                weightInputString = filteredValue
                                userProfile = userProfile.copy(weight = filteredValue.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text("Peso", color = colorResource(id = R.color.textSecondary)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        trailingIcon = { Text("Kg", modifier = Modifier.padding(end = 8.dp), color = colorResource(id = R.color.textSecondary)) },
                        colors = outlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = heightInputString,
                        onValueChange = { newValue ->
                            val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                            if (filteredValue.count { it == '.' } <= 1) {
                                heightInputString = filteredValue
                                userProfile = userProfile.copy(height = filteredValue.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text("Talla", color = colorResource(id = R.color.textSecondary)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        trailingIcon = { Text("m", modifier = Modifier.padding(end = 8.dp), color = colorResource(id = R.color.textSecondary)) },
                        colors = outlinedTextFieldColors()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Campos de IMC
                OutlinedTextField(
                    value = if (userProfile.bmi == 0.0) "N/A" else String.format(Locale.US, "%.2f", userProfile.bmi),
                    onValueChange = { },
                    label = { Text("Índice de Masa Corporal (IMC)", color = colorResource(id = R.color.textSecondary)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        // Indicador de IMC con color
                        Text(
                            text = userProfile.bmiCategory,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (userProfile.bmiCategory.trim()) {
                                "Bajo peso." -> colorResource(id = R.color.accentBlueButton)
                                "Peso normal/saludable." -> colorResource(id = R.color.accentGreenButton)
                                "Sobrepeso." -> colorResource(id = R.color.accentYellowButton)
                                "Obesidad." -> colorResource(id = R.color.accentRedButton)
                                else -> colorResource(id = R.color.textSecondary)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    colors = outlinedTextFieldColors()
                )
            }


            // --- Sección Datos Personales ---
            ProfileSectionCard(title = "Datos Personales 👤") {
                val calendar = Calendar.getInstance()
                val selectedYear = dateOfBirthInput?.year ?: calendar.get(Calendar.YEAR)
                val selectedMonth = dateOfBirthInput?.monthValue?.minus(1) ?: calendar.get(Calendar.MONTH)
                val selectedDay = dateOfBirthInput?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)

                // DatePickerDialog
                val datePickerDialog = remember {
                    DatePickerDialog(
                        context,
                        android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth,
                        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                            dateOfBirthInput = LocalDate.of(year, month + 1, dayOfMonth)
                        },
                        selectedYear, selectedMonth, selectedDay
                    )
                }

                // Campo Fecha de Nacimiento
                OutlinedTextField(
                    value = dateOfBirthInput?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))) ?: "",
                    onValueChange = { },
                    label = { Text("Fecha de Nacimiento", color = colorResource(id = R.color.textSecondary)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Seleccionar fecha", tint = colorResource(id = R.color.buttonPrimaryBackground))
                        }
                    },
                    colors = outlinedTextFieldColors()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Campo Edad y Teléfono
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = if (userProfile.age > 0) userProfile.age.toString() else "N/A",
                        onValueChange = { },
                        label = { Text("Edad", color = colorResource(id = R.color.textSecondary)) },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = { Text("años", modifier = Modifier.padding(end = 8.dp), color = colorResource(id = R.color.textSecondary)) },
                        colors = outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = phoneInputString,
                        onValueChange = { newValue ->
                            if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                                phoneInputString = newValue
                            }
                        },
                        label = { Text("Teléfono", color = colorResource(id = R.color.textSecondary)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Teléfono", tint = colorResource(id = R.color.buttonPrimaryBackground)) },
                        colors = outlinedTextFieldColors()
                    )
                }
            }


            // --- Sección Sexo ---
            ProfileSectionCard(title = "Sexo") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Opción Mujer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { userProfile = userProfile.copy(gender = "Mujer") }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (userProfile.gender == "Mujer") colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.1f) else colorResource(id = R.color.placeholderBackground))
                            .border(1.dp, if (userProfile.gender == "Mujer") colorResource(id = R.color.buttonPrimaryBackground) else colorResource(id = R.color.dividerColor), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = userProfile.gender == "Mujer",
                            onClick = { userProfile = userProfile.copy(gender = "Mujer") },
                            colors = RadioButtonDefaults.colors(selectedColor = colorResource(id = R.color.buttonPrimaryBackground), unselectedColor = colorResource(id = R.color.textSecondary))
                        )
                        Text("Mujer", fontSize = 16.sp, color = colorResource(id = R.color.primaryText), fontWeight = FontWeight.Medium)
                    }

                    // Opción Hombre
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { userProfile = userProfile.copy(gender = "Hombre") }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (userProfile.gender == "Hombre") colorResource(id = R.color.buttonPrimaryBackground).copy(alpha = 0.1f) else colorResource(id = R.color.placeholderBackground))
                            .border(1.dp, if (userProfile.gender == "Hombre") colorResource(id = R.color.buttonPrimaryBackground) else colorResource(id = R.color.dividerColor), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = userProfile.gender == "Hombre",
                            onClick = { userProfile = userProfile.copy(gender = "Hombre") },
                            colors = RadioButtonDefaults.colors(selectedColor = colorResource(id = R.color.buttonPrimaryBackground), unselectedColor = colorResource(id = R.color.textSecondary))
                        )
                        Text("Hombre", fontSize = 16.sp, color = colorResource(id = R.color.primaryText), fontWeight = FontWeight.Medium)
                    }
                }
            }


            // --- Sección Condición Médica ---
            ProfileSectionCard(title = "Condición Médica") {
                val medicalConditions = listOf("Ninguna", "Diabetes", "Hipertensión", "Ambas", "Otra")
                var conditionExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = !conditionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userProfile.condition.ifEmpty { "Seleccionar" },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Condición de Salud", color = colorResource(id = R.color.textSecondary)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false },
                        modifier = Modifier.background(colorResource(id = R.color.cardBackground))
                    ) {
                        medicalConditions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, color = colorResource(id = R.color.primaryText)) },
                                onClick = {
                                    userProfile = userProfile.copy(condition = selectionOption)
                                    conditionExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))


            // --- BOTÓN PRINCIPAL: GUARDAR PERFIL ---
            Button(
                onClick = {
                    // Lógica de Guardado (Mantenida)
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null) {
                        val profileToSave = userProfile.copy(
                            name = tempUserName,
                            weight = weightInputString.toDoubleOrNull() ?: 0.0,
                            height = heightInputString.toDoubleOrNull() ?: 0.0,
                            phone = phoneInputString
                        )

                        firestoreDb.collection("users").document(currentUser.uid)
                            .set(profileToSave, SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(context, "Perfil guardado y actualizado correctamente", Toast.LENGTH_LONG).show()
                                Log.d("ProfileScreen", "Perfil guardado con éxito: $profileToSave")
                                // ✅ Usar navController.popBackStack() para regresar
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error al guardar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("ProfileScreen", "Error al guardar perfil", e)
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.accentGreenButton),
                    contentColor = colorResource(id = R.color.accentGreenButtonText)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    "GUARDAR PERFIL",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- BOTÓN SECUNDARIO: CERRAR SESIÓN ---
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = BorderStroke(1.dp, colorResource(id = R.color.accentRedButton)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorResource(id = R.color.accentRedButton)
                ),
                shape = RoundedCornerShape(12.dp)
            ){
                Text(
                    "Cerrar Sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // --- Diálogo de Edición de Nombre ---
            if (showNameEditDialog) {
                EditNameDialog(
                    currentName = tempUserName,
                    onDismiss = { showNameEditDialog = false },
                    onSave = { newName -> tempUserName = newName }
                )
            }
        }
    }
}
