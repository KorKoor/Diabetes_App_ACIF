package com.example.diabetes_app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Etiqueta de depuración
    private val TAG = "NotificationWorker"

    override suspend fun doWork(): Result {
        // --- 1. Extracción y Validación de Datos ---
        val medicationName = inputData.getString("MEDICATION_NAME") ?: "tu medicamento"
        val notificationTitle = inputData.getString("NOTIFICATION_TITLE") ?: "Recordatorio"
        val notificationMessage = inputData.getString("NOTIFICATION_MESSAGE") ?: "Es hora de tomar tu medicamento."
        val notificationId = inputData.getInt("NOTIFICATION_ID", 0)

        // Validación crítica: si no hay ID, no podemos notificar.
        if (notificationId == 0) {
            Log.e(TAG, "Worker failed: NOTIFICATION_ID missing or 0.")
            return Result.failure()
        }

        Log.d(TAG, "Starting notification work for $medicationName (ID: $notificationId)")

        try {
            // --- 2. Ejecución de la Tarea (Mostrar Notificación) ---
            val notificationHelper = NotificationHelper(applicationContext)

            // Usamos la función base showNotification, que es robusta y centralizada
            notificationHelper.showNotification(
                title = notificationTitle,
                message = notificationMessage,
                notificationId = notificationId
            )

            // ✅ Lógica de Negocio (Opcional pero recomendable):
            // Si necesitaras registrar en Firestore que el recordatorio fue enviado,
            // este es el lugar para hacerlo, usando el notificationId como referencia.

            Log.d(TAG, "Notification successfully displayed for $medicationName.")

            // Si la tarea se completa con éxito, retornamos éxito.
            // WorkManager se encargará de reprogramar la siguiente ejecución (Periodic Work Request).
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker failed to display notification or encountered exception: ${e.message}", e)

            // Si la falla es temporal (ej. error de red, aunque improbable aquí),
            // podemos retornar retry. Para fallas de datos, retornamos failure.
            return Result.failure()
        }
    }
}
