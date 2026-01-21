package com.example.diabetes_app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.diabetes_app.data.MedicationData
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    /**
     * 🔔 Programa una alarma exacta para un medicamento dinámico.
     * Usa la frecuencia guardada en Firestore (ej. "cada 8hr", "tres veces al dia").
     */
    fun scheduleNotification(medication: MedicationData, time: String) {
        val notificationId = generateNotificationId(medication.name, time)
        val (hour, minute) = parseHourMinute(time) ?: return

        val intervaloMillis = parseFrecuencia(medication.frequency)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "MEDICATION")
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("MEDICATION_TIME", time)
            putExtra("MEDICATION_FREQUENCY", medication.frequency)
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("INTERVALO", intervaloMillis)
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
            if (!alarmManager.canScheduleExactAlarms()) {
                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
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
            Log.d(TAG, "💊 Alarma programada: ${medication.name} a las $time cada ${medication.frequency}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso SCHEDULE_EXACT_ALARM requerido en Android 12+", e)
        }
    }

    /**
     * ⏱️ Convierte la frecuencia en texto a milisegundos.
     */
    private fun parseFrecuencia(frecuencia: String?): Long {
        return when (frecuencia?.lowercase()?.trim()) {
            "cada 6hr", "cada 6 horas" -> 6 * 60 * 60 * 1000L
            "cada 8hr", "cada 8 horas", "tres veces al dia" -> 8 * 60 * 60 * 1000L
            "cada 12hr", "cada 12 horas", "dos veces al dia" -> 12 * 60 * 60 * 1000L
            "cada 24hr", "cada 24 horas", "una vez al dia" -> 24 * 60 * 60 * 1000L
            else -> 6 * 60 * 60 * 1000L // valor por defecto: cada 6h
        }
    }

    /**
     * ✨ Programa todas las alarmas diarias fijas:
     * 🌞 Buenos días
     * 🍽️ Comidas (almuerzo, comida, cena)
     * 🔥 Racha
     * 🩸 Glucosa (mañana, medio día, tarde, noche)
     * 🏃 Actividad física
     */
    fun scheduleDailyReminders() {
        // 🌞 Buenos días
        scheduleRepeatingAlarm("GOOD_MORNING", 5001, 7, 0)

        // 🍽️ Comidas
        scheduleRepeatingAlarm("MEAL_ALMUERZO", 5002, 12, 0)
        scheduleRepeatingAlarm("MEAL_COMIDA", 5003, 14, 0)
        scheduleRepeatingAlarm("MEAL_CENA", 5004, 20, 0)

        // 🔥 Racha
        scheduleRepeatingAlarm("STREAK", 5005, 21, 0)

        // 🩸 Glucosa
        scheduleRepeatingAlarm("GLUCOSE_MORNING", 5006, 8, 0)
        scheduleRepeatingAlarm("GLUCOSE_NOON", 5007, 12, 0)
        scheduleRepeatingAlarm("GLUCOSE_AFTERNOON", 5008, 18, 0)
        scheduleRepeatingAlarm("GLUCOSE_NIGHT", 5009, 22, 0)

        // 🏃 Actividad física
        scheduleRepeatingAlarm("ACTIVITY", 5010, 17, 0)

        Log.d(TAG, "✅ Todas las alarmas diarias programadas correctamente")
    }

    /**
     * 🔔 Helper para programar alarmas repetitivas diarias.
     */
    private fun scheduleRepeatingAlarm(type: String, requestCode: Int, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", type)
            putExtra("NOTIFICATION_ID", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "🔔 Alarma diaria programada: $type a las ${calendar.time} (ID=$requestCode)")
    }


    /**
     * 🆔 Genera un ID único para cada alarma según nombre y hora.
     */
    private fun generateNotificationId(name: String, time: String): Int {
        return "$name-$time".hashCode()
    }

    /**
     * 🕒 Construye un Calendar seguro (1 min antes, nunca en el pasado).
     */
    private fun buildCalendar(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // ❌ No restamos un minuto
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1) // si ya pasó, mover al día siguiente
            }
        }
    }
    /**
     * 🩸 Programa un chequeo de glucosa a una hora fija.
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

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "🩸 Chequeo de glucosa programado a las $hour:$minute (ID=$id)")
    }
    fun cancelNotification(medication: MedicationData, time: String) {
        val notificationId = generateNotificationId(medication.name, time)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "MEDICATION")
            putExtra("MEDICATION_NAME", medication.name) // 👈 usamos el campo name
            putExtra("MEDICATION_TIME", time)
            putExtra("NOTIFICATION_ID", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "❌ Alarma cancelada: ${medication.name} a las $time (ID=$notificationId)")
    }
    fun scheduleDailyActivityReminders() {
        scheduleActivityReminder(9, 0, 2001)   // ⏰ 9:00 AM
        scheduleActivityReminder(16, 0, 2002)  // ⏰ 4:00 PM
        scheduleActivityReminder(21, 0, 2003)  // ⏰ 9:00 PM
    }

    /**
     * Programa un recordatorio de actividad física a una hora específica.
     */
    private fun scheduleActivityReminder(hour: Int, minute: Int, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "ACTIVITY")
            putExtra("NOTIFICATION_ID", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "🏃 Recordatorio de actividad programado a las $hour:$minute (ID=$requestCode)")
    }
    /**
     * Programa alarma principal y una de seguimiento (+30 min).
     */
    private fun scheduleMealWithFollowup(type: String, requestCode: Int, hour: Int, minute: Int) {
        // Alarma principal
        scheduleRepeatingAlarm(type, requestCode, hour, minute)

        // Alarma de seguimiento (+30 min)
        val followupCode = requestCode + 1000 // ID distinto
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "${type}_FOLLOWUP")
            putExtra("NOTIFICATION_ID", followupCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            followupCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute + 30)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "🍽️ Alarma de seguimiento programada: $type a las ${hour}:${minute+30}")
    }
    fun scheduleStreakReminder(streakDays: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOTIFICATION_TYPE", "STREAK")
            putExtra("NOTIFICATION_ID", 9999)
            putExtra("STREAK_DAYS", streakDays)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21) // ⏰ 9:00 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    fun scheduleMealReminders() {
        scheduleMealWithFollowup("MEAL_ALMUERZO", 5002, 12, 0)
        scheduleMealWithFollowup("MEAL_COMIDA", 5003, 14, 0)
        scheduleMealWithFollowup("MEAL_CENA", 5004, 20, 0)
    }

    fun scheduleGlucoseReminders() {
        scheduleRepeatingAlarm("GLUCOSE_MORNING", 5006, 8, 0)
        scheduleRepeatingAlarm("GLUCOSE_NOON", 5007, 12, 0)
        scheduleRepeatingAlarm("GLUCOSE_AFTERNOON", 5008, 18, 0)
        scheduleRepeatingAlarm("GLUCOSE_NIGHT", 5009, 22, 0)
    }

    fun scheduleGoodMorningReminder() {
        scheduleRepeatingAlarm("GOOD_MORNING", 5001, 8, 0)
    }

    fun scheduleStreakReminderDefault() {
        scheduleStreakReminder(0) // 🔥 racha inicial con 0 días
    }

    /**
     * ⏱️ Utilidad para parsear hora en formato HH:mm.
     */
    private fun parseHourMinute(time: String): Pair<Int, Int>? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return Pair(hour, minute)
    }
}
