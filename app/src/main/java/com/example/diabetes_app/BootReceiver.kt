package com.example.diabetes_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.diabetes_app.data.MedicationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!isBootCompleted(intent)) return

        Log.i(TAG, "Teléfono encendido. Reprogramando alarmas...")

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "No hay usuario autenticado. No se reprograman alarmas.")
            return
        }

        val scheduler = AlarmScheduler(context)
        reprogramUserMedications(userId, scheduler)
        reprogramFixedReminders(scheduler)
    }

    /**
     * Verifica si la acción recibida corresponde a un encendido del dispositivo.
     */
    private fun isBootCompleted(intent: Intent): Boolean {
        return intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == "android.intent.action.QUICKBOOT_POWERON"
    }

    /**
     * Reprograma las alarmas de medicamentos del usuario desde Firestore.
     */
    private fun reprogramUserMedications(userId: String, scheduler: AlarmScheduler) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("medications")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val medications = querySnapshot.toObjects(MedicationData::class.java)
                medications.forEach { med ->
                    med.time.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { time ->
                            scheduler.scheduleNotification(med, time)
                            Log.d(TAG, "Reprogramada: ${med.name} a las $time")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al recuperar medicamentos tras reinicio", e)
            }
    }

    /**
     * Reprograma recordatorios fijos (ejemplo: chequeo de glucosa).
     */
    private fun reprogramFixedReminders(scheduler: AlarmScheduler) {
        scheduler.scheduleGlucoseCheck(GLUCOSE_CHECK_ID, 8, 0) // 8:00 AM fijo
        Log.d(TAG, "Recordatorio de glucosa reprogramado a las 08:00")
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val GLUCOSE_CHECK_ID = 1001
    }
}
