package com.example.diabetes_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.diabetes_app.data.MedicationData

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        val scheduler = AlarmScheduler(context)

        val type = intent.getStringExtra(KEY_TYPE) ?: TYPE_GENERAL
        val medName = intent.getStringExtra(KEY_MED_NAME) ?: "Medicamento"
        val medTime = intent.getStringExtra(KEY_MED_TIME) ?: "--:--"
        val mealType = intent.getStringExtra(KEY_MEAL_TYPE) ?: "Comida"
        val notificationId = intent.getIntExtra(
            KEY_NOTIFICATION_ID,
            (System.currentTimeMillis() % 10000).toInt()
        )

        // 🔎 Leer intervalo y frecuencia desde el Intent (para medicamentos dinámicos)
        val intervalo = intent.getLongExtra("INTERVALO", 24 * 60 * 60 * 1000L)
        val frequency = intent.getStringExtra("MEDICATION_FREQUENCY") ?: ""

        // 🔎 Leer días de racha si aplica
        val streakDays = intent.getIntExtra("STREAK_DAYS", 0)

        Log.i(TAG, "⏰ Alarma disparada: Tipo=$type, ID=$notificationId")

        when (type) {
            TYPE_MEDICATION -> handleMedicationReminder(
                notificationHelper,
                scheduler,
                medName,
                medTime,
                notificationId,
                intervalo,
                frequency
            )

            // 🍽️ Seguimiento de comidas
            TYPE_MEAL_FOLLOWUP -> notificationHelper.showMealFollowup(notificationId)

            // 💊 Seguimiento de medicamentos
            TYPE_MEDICATION_FOLLOWUP -> notificationHelper.showMedicationFollowup(notificationId)

            // 🩸 Seguimiento de glucosa
            TYPE_GLUCOSE_FOLLOWUP -> notificationHelper.showGlucoseFollowup(notificationId)

            TYPE_GLUCOSE -> handleGlucoseReminder(notificationHelper, scheduler, notificationId)
            TYPE_MEAL -> notificationHelper.showMealReminder(mealType, notificationId)
            TYPE_ACTIVITY -> notificationHelper.showActivityReminder(notificationId)
            TYPE_GOOD_MORNING -> notificationHelper.showGoodMorning(notificationId) // 🌞 Buenos días
            TYPE_STREAK -> notificationHelper.showStreakReminder(streakDays, notificationId) // 🔥 Racha
            TYPE_GLUCOSE_MORNING -> notificationHelper.showGlucoseCheckReminder(notificationId) // 🩸 Mañana
            TYPE_GLUCOSE_NOON -> notificationHelper.showGlucoseCheckReminder(notificationId)    // 🩸 Medio día
            TYPE_GLUCOSE_AFTERNOON -> notificationHelper.showGlucoseCheckReminder(notificationId) // 🩸 Tarde
            TYPE_GLUCOSE_NIGHT -> notificationHelper.showGlucoseCheckReminder(notificationId)   // 🩸 Noche
            else -> notificationHelper.showGeneralMotivation(notificationId)
        }
    }

    /**
     * 💊 Maneja recordatorios de medicamentos y reprograma según la frecuencia.
     */
    private fun handleMedicationReminder(
        notificationHelper: NotificationHelper,
        scheduler: AlarmScheduler,
        medName: String,
        medTime: String,
        notificationId: Int,
        intervalo: Long,
        frequency: String
    ) {
        // Mostrar notificación
        notificationHelper.showMedicationReminder(medName, medTime, notificationId)

        // Crear objeto medicamento con frecuencia real
        val medication = MedicationData(
            name = medName,
            dose = 0,
            unit = "",
            time = medTime,
            frequency = frequency
        )

        // Reprogramar la siguiente alarma sumando el intervalo
        scheduler.scheduleNotification(medication, medTime)

        Log.d(TAG, "🔄 Reprogramada alarma de medicamento: $medName cada $frequency (intervalo=$intervalo ms)")
    }

    /**
     * 🩸 Maneja recordatorios de glucosa y reprograma a las 8:00 AM.
     */
    private fun handleGlucoseReminder(
        notificationHelper: NotificationHelper,
        scheduler: AlarmScheduler,
        notificationId: Int
    ) {
        notificationHelper.showGlucoseCheckReminder(notificationId)
        scheduler.scheduleGlucoseCheck(notificationId, 8, 0)
        Log.d(TAG, "🔄 Reprogramado chequeo de glucosa a las 08:00 AM")
    }

    companion object {
        private const val TAG = "AlarmReceiver"

        // Claves de Intent
        private const val KEY_TYPE = "NOTIFICATION_TYPE"
        private const val KEY_MED_NAME = "MEDICATION_NAME"
        private const val KEY_MED_TIME = "MEDICATION_TIME"
        private const val KEY_MEAL_TYPE = "MEAL_TYPE"
        private const val KEY_NOTIFICATION_ID = "NOTIFICATION_ID"

        // Tipos de notificación
        private const val TYPE_MEDICATION = "MEDICATION"
        private const val TYPE_GLUCOSE = "GLUCOSE"
        private const val TYPE_MEAL = "MEAL"
        private const val TYPE_ACTIVITY = "ACTIVITY"
        private const val TYPE_GENERAL = "GENERAL"

        private const val TYPE_MEAL_FOLLOWUP = "MEAL_FOLLOWUP"

        private const val TYPE_MEDICATION_FOLLOWUP = "MEDICATION_FOLLOWUP"

        private const val TYPE_GLUCOSE_FOLLOWUP = "GLUCOSE_FOLLOWUP"


        // Nuevos tipos diarios 🌞🔥🩸
        private const val TYPE_GOOD_MORNING = "GOOD_MORNING"
        private const val TYPE_STREAK = "STREAK"
        private const val TYPE_GLUCOSE_MORNING = "GLUCOSE_MORNING"
        private const val TYPE_GLUCOSE_NOON = "GLUCOSE_NOON"
        private const val TYPE_GLUCOSE_AFTERNOON = "GLUCOSE_AFTERNOON"
        private const val TYPE_GLUCOSE_NIGHT = "GLUCOSE_NIGHT"
    }
}
