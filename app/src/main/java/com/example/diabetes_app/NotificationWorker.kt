package com.example.diabetes_app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"

        // Claves de inputData
        const val KEY_MEDICATION_NAME = "MEDICATION_NAME"
        const val KEY_NOTIFICATION_TITLE = "NOTIFICATION_TITLE"
        const val KEY_NOTIFICATION_MESSAGE = "NOTIFICATION_MESSAGE"
        const val KEY_NOTIFICATION_ID = "NOTIFICATION_ID"
    }

    override suspend fun doWork(): Result {
        // --- 1. Extracción y Validación de Datos ---
        val medicationName = inputData.getString(KEY_MEDICATION_NAME) ?: "tu medicamento"
        val notificationTitle = inputData.getString(KEY_NOTIFICATION_TITLE) ?: "Recordatorio"
        val notificationMessage = inputData.getString(KEY_NOTIFICATION_MESSAGE)
            ?: "Es hora de tomar tu medicamento."
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0)

        if (notificationId == 0) {
            Log.e(TAG, "Worker failed: NOTIFICATION_ID missing or 0.")
            return Result.failure()
        }

        Log.d(TAG, "Starting notification work for $medicationName (ID: $notificationId)")

        return try {
            // --- 2. Mostrar Notificación ---
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.createNotificationChannel() // 🔹 Asegurar canal
            notificationHelper.showNotification(
                title = notificationTitle,
                message = notificationMessage,
                notificationId = notificationId
            )

            // --- 3. Lógica adicional opcional ---
            // Aquí podrías registrar en Firestore que la notificación fue enviada:
            // FirebaseFirestore.getInstance().collection("notifications").add(...)

            Log.d(TAG, "Notification successfully displayed for $medicationName.")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker failed: ${e.message}", e)
            Result.retry() // 🔹 Mejor usar retry para fallas temporales
        }
    }
}
