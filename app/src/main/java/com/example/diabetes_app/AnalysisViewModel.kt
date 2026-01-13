package com.example.diabetes_app.ui.analysis

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabetes_app.data.DailyRecordData
import com.example.diabetes_app.data.DosageTakenRecord
import com.example.diabetes_app.data.MedicationData
import com.example.diabetes_app.data.MealRecordData
import com.example.diabetes_app.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AnalysisViewModel : ViewModel() {

    // Instancias de Firebase
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // --- Estados para almacenar todos los datos brutos del usuario ---
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _medications = MutableStateFlow<List<MedicationData>>(emptyList())
    val medications: StateFlow<List<MedicationData>> = _medications.asStateFlow()

    private val _dailyRecords = MutableStateFlow<List<DailyRecordData>>(emptyList())
    val dailyRecords: StateFlow<List<DailyRecordData>> = _dailyRecords.asStateFlow()

    private val _mealRecords = MutableStateFlow<List<MealRecordData>>(emptyList())
    val mealRecords: StateFlow<List<MealRecordData>> = _mealRecords.asStateFlow()

    private val _dosageTakenRecords = MutableStateFlow<List<DosageTakenRecord>>(emptyList())
    val dosageTakenRecords: StateFlow<List<DosageTakenRecord>> = _dosageTakenRecords.asStateFlow()

    // --- Estados para controlar el período de tiempo seleccionado ---
    private val _selectedPeriodDays = MutableStateFlow(30)
    val selectedPeriodDays: StateFlow<Int> = _selectedPeriodDays.asStateFlow()

    private val _currentDateForFiltering = MutableStateFlow(LocalDate.now(ZoneId.systemDefault()))
    val currentDateForFiltering: StateFlow<LocalDate> = _currentDateForFiltering.asStateFlow()

    // --- Estados calculados reactivamente a partir de los datos brutos y el período de tiempo ---
    val filteredDailyRecords: StateFlow<List<DailyRecordData>> =
        dailyRecords.combine(_selectedPeriodDays.combine(_currentDateForFiltering) { days, date -> days to date }) { records, (days, today) ->
            records.filter {
                val d = it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                days == Int.MAX_VALUE || (!d.isBefore(today.minusDays(days.toLong() - 1)) && !d.isAfter(today))
            }.sortedBy { it.date }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMealRecords: StateFlow<List<MealRecordData>> =
        mealRecords.combine(_selectedPeriodDays.combine(_currentDateForFiltering) { days, date -> days to date }) { records, (days, today) ->
            records.filter {
                val d = it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                days == Int.MAX_VALUE || (!d.isBefore(today.minusDays(days.toLong() - 1)) && !d.isAfter(today))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDosageRecords: StateFlow<List<DosageTakenRecord>> =
        dosageTakenRecords.combine(_selectedPeriodDays.combine(_currentDateForFiltering) { days, date -> days to date }) { records, (days, today) ->
            records.filter {
                val d = it.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                days == Int.MAX_VALUE || (!d.isBefore(today.minusDays(days.toLong() - 1)) && !d.isAfter(today))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Función para calcular dosis esperadas totales ---
    fun calculateTotalExpectedDoses(
        medications: List<MedicationData>,
        filteredDailyRecords: List<DailyRecordData>,
        selectedPeriodDays: Int,
        calculateExpectedDoseTimes: (MedicationData) -> List<Pair<String, String>>
    ): Int {
        return medications.map { medication ->
            val expectedTimes = calculateExpectedDoseTimes(medication)
            val totalDays = if (selectedPeriodDays == Int.MAX_VALUE) {
                val firstRecordDate = filteredDailyRecords.minOfOrNull { it.date.time }
                if (firstRecordDate != null) {
                    val firstDate = Instant.ofEpochMilli(firstRecordDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    (ChronoUnit.DAYS.between(firstDate, LocalDate.now()) + 1).toInt()
                } else 0
            } else {
                selectedPeriodDays
            }
            expectedTimes.size * totalDays
        }.sum()
    }

    fun generateDosageDocId(record: DosageTakenRecord): String {
        val datePart = LocalDate.ofInstant(record.timestamp.toInstant(), ZoneId.systemDefault()).toString()
        val timePart = java.time.LocalTime.ofInstant(record.timestamp.toInstant(), ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH-mm-ss"))
        return "${record.medicationDocId}-$datePart-$timePart"
    }

    /**
     * Carga todos los datos del usuario desde Firestore en un solo lugar.
     */
    fun loadData(userId: String? = auth.currentUser?.uid) {
        userId ?: return
        viewModelScope.launch {
            try {
                val userDocRef = firestore.collection("users").document(userId)

                val profileDocument = userDocRef.get().await()
                val loadedProfile = profileDocument.toObject(UserProfile::class.java)
                loadedProfile?.let { _userProfile.value = it }

                val medicationsList = userDocRef.collection("medications").get().await().toObjects(MedicationData::class.java)
                _medications.value = medicationsList

                val dailyRecordsList = userDocRef.collection("dailyRecords").get().await().toObjects(DailyRecordData::class.java)
                _dailyRecords.value = dailyRecordsList

                val mealRecordsList = userDocRef.collection("mealRecords").get().await().toObjects(MealRecordData::class.java)
                _mealRecords.value = mealRecordsList

                val dosageTakenRecordsList = userDocRef.collection("dosageTakenRecords").get().await().toObjects(DosageTakenRecord::class.java)
                _dosageTakenRecords.value = dosageTakenRecordsList

                updateSelectedPeriodDays(_selectedPeriodDays.value)

            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Error al cargar datos de Firestore: ${e.message}")
            }
        }
    }

    fun updateSelectedPeriodDays(days: Int) {
        _selectedPeriodDays.value = days
    }

    init {
        setupDailyDateUpdate()
    }

    private fun setupDailyDateUpdate() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDate.now(ZoneId.systemDefault())
                if (now != _currentDateForFiltering.value) {
                    _currentDateForFiltering.value = now
                }
                val tomorrowMidnight = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val delayMillis = (tomorrowMidnight - System.currentTimeMillis()) + 1000
                delay(delayMillis.coerceAtLeast(1000L))
            }
        }
    }
}
