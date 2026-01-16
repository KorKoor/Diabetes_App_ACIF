package com.example.diabetes_app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.diabetes_app.data.MedicationData
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    /**
     * Genera un ID único para cada alarma según nombre y hora.
     */
    private fun generateNotificationId(name: String, time: String): Int {
        return "$name-$time".hashCode()
    }

    /**
     * Programa una alarma exacta para un medicamento.
     * Se dispara 1 minuto antes de la hora configurada.
     */
    fun scheduleNotification(medication: MedicationData, time: String) {
        val notificationId = generateNotificationId(medication.name, time)
        val (hour, minute) = parseHourMinute(time) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "MEDICATION")
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("MEDICATION_TIME", time)
            putExtra("NOTIFICATION_ID", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = buildCalendar(hour, minute)

        // 🔎 Verificación de permiso en Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intentSettings = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intentSettings)
                Log.e(TAG, "⚠️ El permiso SCHEDULE_EXACT_ALARM no está concedido")
                return
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Alarma programada: ${medication.name} a las $time (1 min antes)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso SCHEDULE_EXACT_ALARM requerido en Android 12+", e)
        }
    }


    /**
     * Programa un recordatorio fijo de glucosa.
     */
    fun scheduleGlucoseCheck(id: Int, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "GLUCOSE")
            putExtra("NOTIFICATION_ID", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = buildCalendar(hour, minute)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d(TAG, "Chequeo de glucosa programado a las $hour:$minute (1 min antes, ID: $id)")
    }

    /**
     * Programa un recordatorio de comida.
     */
    fun scheduleMealReminder(id: Int, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "MEAL")
            putExtra("NOTIFICATION_ID", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = buildCalendar(hour, minute)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d(TAG, "Recordatorio de comida programado a las $hour:$minute (1 min antes, ID: $id)")
    }

    /**
     * Programa un recordatorio de actividad física.
     */
    fun scheduleActivityReminder(id: Int, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "ACTIVITY")
            putExtra("NOTIFICATION_ID", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = buildCalendar(hour, minute)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d(TAG, "Recordatorio de actividad programado a las $hour:$minute (1 min antes, ID: $id)")
    }

    /**
     * Cancela una alarma de medicamento.
     */
    fun cancelNotification(medication: MedicationData, time: String) {
        val notificationId = generateNotificationId(medication.name, time)
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarma cancelada: ${medication.name} a las $time")
    }

    /**
     * Reprograma todas las alarmas (medicamentos, glucosa, comidas, actividad).
     */
    fun rescheduleAll(
        medications: List<MedicationData>,
        glucoseChecks: List<Pair<Int, Pair<Int, Int>>>,
        meals: List<Pair<Int, Pair<Int, Int>>>,
        activities: List<Pair<Int, Pair<Int, Int>>>
    ) {
        Log.d(TAG, "Rescheduling all alarms...")

        // 🔹 Reprogramar medicamentos (usa 'time' en lugar de 'times')
        medications.forEach { med ->
            if (med.time.isNotEmpty()) {
                scheduleNotification(med, med.time)
            }
        }

        // 🔹 Reprogramar chequeos de glucosa
        glucoseChecks.forEach { (id, hm) ->
            val (hour, minute) = hm
            scheduleGlucoseCheck(id, hour, minute)
        }

        // 🔹 Reprogramar comidas
        meals.forEach { (id, hm) ->
            val (hour, minute) = hm
            scheduleMealReminder(id, hour, minute)
        }

        // 🔹 Reprogramar actividad física
        activities.forEach { (id, hm) ->
            val (hour, minute) = hm
            scheduleActivityReminder(id, hour, minute)
        }

        Log.d(TAG, "✅ Todas las alarmas reprogramadas correctamente.")
    }

    /**
     * Construye un Calendar seguro (1 min antes, nunca en el pasado).
     */
    private fun buildCalendar(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // 🔹 Restar 1 minuto
            add(Calendar.MINUTE, -1)

            // Si el resultado es antes de ahora, mover a mañana
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }
    }

    /**
     * Utilidad para parsear hora en formato HH:mm.
     */
    private fun parseHourMinute(time: String): Pair<Int, Int>? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return Pair(hour, minute)
    }
}
