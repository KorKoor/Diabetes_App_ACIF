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

        Log.i(TAG, "⏰ Alarma disparada: Tipo=$type, ID=$notificationId")

        when (type) {
            TYPE_MEDICATION -> handleMedicationReminder(notificationHelper, scheduler, medName, medTime, notificationId)
            TYPE_GLUCOSE -> handleGlucoseReminder(notificationHelper, scheduler, notificationId)
            TYPE_MEAL -> notificationHelper.showMealReminder(mealType, notificationId)
            TYPE_ACTIVITY -> notificationHelper.showActivityReminder(notificationId)
            else -> notificationHelper.showGeneralMotivation(notificationId)
        }
    }

    /**
     * Maneja recordatorios de medicamentos y reprograma para el día siguiente.
     */
    private fun handleMedicationReminder(
        notificationHelper: NotificationHelper,
        scheduler: AlarmScheduler,
        medName: String,
        medTime: String,
        notificationId: Int
    ) {
        notificationHelper.showMedicationReminder(medName, medTime, notificationId)

        // Reprogramar para mañana (24h después)
        val medication = MedicationData(
            name = medName,
            dose = 0,
            unit = "",
            time = medTime,
            frequency = ""
        )
        scheduler.scheduleNotification(medication, medTime)
        Log.d(TAG, "Reprogramada alarma de medicamento: $medName a las $medTime")
    }

    /**
     * Maneja recordatorios de glucosa y reprograma a las 8:00 AM.
     */
    private fun handleGlucoseReminder(
        notificationHelper: NotificationHelper,
        scheduler: AlarmScheduler,
        notificationId: Int
    ) {
        notificationHelper.showGlucoseCheckReminder(notificationId)
        scheduler.scheduleGlucoseCheck(notificationId, 8, 0)
        Log.d(TAG, "Reprogramado chequeo de glucosa a las 08:00 AM")
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
    }
}
