package com.example.diabetes_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * NOTA IMPORTANTE:
 * Esta clase BroadcastReceiver ya no es necesaria, ya que toda la programación
 * y disparo de notificaciones se realiza ahora a través de WorkManager (clase AlarmScheduler).
 *
 * Se recomienda eliminar este archivo o dejar su contenido vacío.
 *
 * Si este archivo debe permanecer por compatibilidad con código antiguo, se puede dejar así,
 * pero no tendrá efecto ya que WorkManager es el responsable del trabajo ahora.
 *
 * Asegúrate de que este componente NO esté declarado en AndroidManifest.xml.
 */
class AlarmReceiver : BroadcastReceiver() {

    // Este código ya no debería ejecutarse si WorkManager es el único programador.
    override fun onReceive(context: Context, intent: Intent) {
        // Log de precaución si, por alguna razón, una alarma antigua aún dispara este Receiver.
        Log.w("AlarmReceiver", "AlarmReceiver ejecutado. Esto es inesperado en la arquitectura WorkManager. Comprobando intent...")

        val medicationName = intent.getStringExtra("MEDICATION_NAME")
        val medicationTime = intent.getStringExtra("MEDICATION_TIME")

        if (medicationName != null) {
            // El código antiguo se mantiene intacto si es necesario un fallback,
            // pero la lógica principal debe ser WorkManager.
            val notificationId = "${medicationName}-${medicationTime}".hashCode()

            val notificationHelper = NotificationHelper(context)
            notificationHelper.showMedicationReminder(medicationName, medicationTime ?: "hora no especificada", notificationId)
        }
    }
}
