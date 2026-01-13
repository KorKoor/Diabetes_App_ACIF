package com.example.diabetes_app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import android.util.Log
import com.example.diabetes_app.data.MedicationData
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

// Nota: Mantenemos el nombre AlarmScheduler para no modificar tu MainActivity,
// pero internamente, usa WorkManager.
class AlarmScheduler(private val context: Context) {

    private fun generateNotificationId(medicationName: String, time: String): Int {
        // Usamos la combinación de nombre y hora como un ID de Request Code único para WorkManager
        // Esto asegura que cada dosis tenga una tarea única.
        return "${medicationName}-${time}".hashCode()
    }

    /**
     * Calcula el retraso inicial en minutos entre la hora actual y la hora de la dosis.
     * Si la hora ya pasó hoy, calcula el retraso hasta la misma hora de mañana.
     */
    private fun calculateInitialDelayMinutes(timeStr: String): Long {
        val doseTime = try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            Log.e("WorkScheduler", "Error parsing time: $timeStr")
            return 0L
        }

        val now = LocalTime.now()
        var delayMinutes = ChronoUnit.MINUTES.between(now, doseTime)

        // Si el retraso es negativo (la hora ya pasó hoy), añade 24 horas (1440 minutos)
        if (delayMinutes <= 0) {
            delayMinutes += 24 * 60
        }

        return delayMinutes
    }

    /**
     * Programa una notificación utilizando WorkManager para que se ejecute diariamente
     * a la hora de la dosis.
     */
    fun scheduleNotification(medication: MedicationData, time: String) {
        val notificationId = generateNotificationId(medication.name, time)
        val initialDelayMinutes = calculateInitialDelayMinutes(time)
        val uniqueWorkName = "medication_dose_${notificationId}" // Nombre único para la tarea

        // 1. Crear los datos que WorkManager pasará al Worker (NotificationWorker)
        val inputData = Data.Builder()
            .putString("MEDICATION_NAME", medication.name)
            .putString("NOTIFICATION_TITLE", "Recordatorio: ${medication.name} (${time})")
            .putString("NOTIFICATION_MESSAGE", "¡Es hora de tomar tu dosis de ${medication.name} a las ${time}!")
            .putInt("NOTIFICATION_ID", notificationId)
            .build()

        // 2. Crear la solicitud de trabajo periódico (se repite cada 24 horas)
        // WorkManager requiere un mínimo de 15 minutos para la repetición.
        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .addTag(uniqueWorkName)
            .build()

        // 3. Encolar la tarea. REPLACE garantiza que la alarma anterior se cancele si la editas.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWorkRequest
        )

        Log.d("WorkScheduler", "Scheduled WorkManager task: $uniqueWorkName for $time (Initial delay: $initialDelayMinutes min)")
    }

    /**
     * Cancela la tarea programada en WorkManager usando el nombre único.
     */
    fun cancelNotification(medication: MedicationData, time: String) {
        val notificationId = generateNotificationId(medication.name, time)
        val uniqueWorkName = "medication_dose_${notificationId}"

        // WorkManager cancela la tarea usando su nombre único
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        Log.d("WorkScheduler", "Cancelled WorkManager task: $uniqueWorkName")
    }
}