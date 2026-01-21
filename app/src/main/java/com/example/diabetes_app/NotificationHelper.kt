package com.example.diabetes_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class NotificationHelper(private val context: Context) {
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "diabetes_app_channel"
        const val CHANNEL_NAME = "Recordatorios de Diabetes"
        const val CHANNEL_DESCRIPTION = "Recordatorios importantes sobre medicación, glucosa y dieta."

        // Código base para PendingIntent (Asegura que el Intent sea único)
        private const val REQUEST_CODE_BASE = 100
    }

    // --- Configuración Base ---

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.accentGreenButton)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Función central para mostrar cualquier notificación.
     * Esta es la función que debe ser llamada por el NotificationWorker.
     */
    fun showNotification(
        title: String,
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Usamos el notificationId + BASE como requestCode
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId + REQUEST_CODE_BASE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(ContextCompat.getColor(context, R.color.accentGreenButton))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    // ----------------------------------------------------------------------
    // --- 1. Recordatorios Específicos de Medicamentos ---
    // ----------------------------------------------------------------------

    /**
     * Genera y muestra un recordatorio de medicamento con gran variedad de mensajes.
     */
    fun showMedicationReminder(medicationName: String, time: String, notificationId: Int) {
        val titles = listOf(
            "¡Hora de la Dosis! ⏰💊",
            "Recordatorio Médico Urgente 🚨",
            "¡El Reloj de tu Medicina! ✨",
            "Cuidado Personal Ahora mismo ❤️"
        )
        val messages = listOf(
            "Toma tu dosis de **$medicationName** a las $time. ¡No demores! Programa la acción inmediatamente.",
            "Recuerda tu **$medicationName**. Un paso simple para mantener tu tratamiento constante. ✅",
            "Tu cuerpo necesita **$medicationName** ahora. Si ya lo tomaste, márcalo en la app para evitar confusiones.",
            "¡Alerta de **Dosis**! La hora $time es crítica para tu medicamento $medicationName. Actúa pronto."
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }

    // ----------------------------------------------------------------------
    // --- 2. Recordatorios de Medición de Glucosa ---
    // ----------------------------------------------------------------------

    /**
     * Genera y muestra un recordatorio para medir la glucosa.
     * Añade un sentido de urgencia por el riesgo de niveles altos/bajos.
     */
    fun showGlucoseCheckReminder(notificationId: Int) {
        val titles = listOf(
            "¡Chequeo de Glucosa Mandatorio! 🩸",
            "Alerta de Monitoreo Diario 📊",
            "Controla tus Niveles ¡Ahora! ⏱️"
        )
        val messages = listOf(
            "¿Hace cuánto que no revisas tu glucosa? Un chequeo te protege de la hipoglucemia y la hiperglucemia. ¡Toma la muestra!",
            "El registro de glucosa es clave. Tómate 30 segundos y registra tu nivel. ¡Los datos son poder!",
            "No dejes que tu glucosa te sorprenda. Revisa tu nivel actual, especialmente si te sientes diferente. ⚠️"
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }

    // ----------------------------------------------------------------------
    // --- 3. Recordatorios de Dieta y Hábitos ---
    // ----------------------------------------------------------------------

    /**
     * Genera y muestra un recordatorio para registrar la comida o planear la dieta.
     */
    fun showMealReminder(mealType: String, notificationId: Int) {
        val titles = listOf(
            "¡Registro de $mealType! 🍎🥗",
            "Control Nutricional al Día 🍽️",
            "¿Listo para tu próxima comida? 😋"
        )
        val messages = listOf(
            "¡Es hora de tu **$mealType**! No olvides registrar los carbohidratos y calorías consumidas.",
            "Tu plan de dieta funciona solo si lo sigues. ¿Qué elegirás para tu $mealType que te haga sentir bien?",
            "Recordatorio de la ingesta de alimentos: Mantener el registro de tu $mealType es vital para ajustar la insulina. ¡Añade los detalles! 📝"
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }

    // ----------------------------------------------------------------------
    // --- 4. Motivación y Racha (Basado en el progreso) ---
    // ----------------------------------------------------------------------

    /**
     * Genera una notificación motivacional basada en la racha de días.
     */
    fun showStreakNotification(streakDays: Int, notificationId: Int) {
        val titles = when {
            streakDays == 1 -> "¡El Primer Paso Gigante! 👣"
            streakDays in 2..6 -> "¡Racha de Consistencia! 🎉"
            streakDays in 7..29 -> "¡Racha de una Semana! 🏆"
            else -> "¡Héroe del Mes! 🥇"
        }
        val message = when {
            streakDays == 1 -> "¡Primer día de registro completado! Eres el inicio de tu propia mejoría. Sigue así. 💪"
            streakDays == 7 -> "¡Una semana completa de dedicación! Eso demuestra compromiso. Tu control de glucosa es más estable. ✨"
            streakDays == 30 -> "¡30 DÍAS! Has creado un hábito poderoso. Tu esfuerzo inspira. ¡No rompas la racha! ❤️"
            else -> "Tu racha crece a **$streakDays días**. Sigue registrando tus datos, ¡eres un ejemplo de autogestión! 🌟"
        }
        showNotification(titles, message, notificationId)
    }

    // ----------------------------------------------------------------------
    // --- 5. Notificación de Actividad Física (Nuevo) ---
    // ----------------------------------------------------------------------

    /**
     * Recordatorio para hacer ejercicio si el registro de actividad es bajo.
     */
    fun showActivityReminder(notificationId: Int) {
        val titles = listOf(
            "¡Mueve el cuerpo! 🏃‍♂️",
            "Pequeña Dosis de Ejercicio 💪",
            "¡Actívate por tu glucosa! 🔋"
        )
        val messages = listOf(
            "Un paseo de 15 minutos puede mejorar significativamente tu sensibilidad a la insulina. ¡Vamos, tú puedes!",
            "Tu plan de salud incluye actividad física. ¿Qué tal unos estiramientos o caminar un poco? ¡El momento es ahora! 🤸‍♀️",
            "Recuerda que el ejercicio ayuda a bajar la glucosa. ¡No pospongas esa caminata! 😉"
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }

    // ----------------------------------------------------------------------
    // --- 6. Motivación y Consejos (General) ---
    // ----------------------------------------------------------------------

    /**
     * Recordatorio de motivación general o consejos aleatorios.
     */
    fun showGeneralMotivation(notificationId: Int) {
        val titles = listOf(
            "¡Mensaje Positivo! 😄",
            "Consejo para tu Día 💡",
            "¡Tú tienes el Control! 🚀"
        )
        val messages = listOf(
            "Recuerda que manejar el estrés ayuda a tu glucosa. Tómate un momento para respirar profundamente. 🧘",
            "La consistencia, no la perfección, es la clave. Si tuviste un tropiezo, ¡retoma tu plan ahora! 🔄",
            "Mantenerte hidratado es vital para tu metabolismo. ¡Bebe un vaso de agua ahora! 💧",
            "Pequeñas decisiones saludables hoy, resultan en grandes recompensas mañana. ¡Confía en el proceso! 👍"
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }
        // 🌞 Buenos días
        fun showGoodMorning(notificationId: Int) {
            val titles = listOf(
                "¡Buenos días! 🌞",
                "¡Despierta con energía! ☀️",
                "¡Un nuevo día comienza! ✨"
            )
            val messages = listOf(
                "Recuerda revisar tu glucosa matutina y empieza el día con hábitos saludables. 💪",
                "Hoy es una nueva oportunidad para cuidar tu salud. ¡Hazlo con constancia y alegría! 😄",
                "¡Sonríe! Cada día cuenta en tu progreso. Mantén tu racha y sigue adelante. 🔥"
            )
            showNotification(titles.random(), messages.random(), notificationId)
        }

        // 🔥 Racha (wrapper para compatibilidad con AlarmReceiver)
        fun showStreakReminder(notificationId: Int) {
            val streakDays = 7 // Ejemplo, luego puedes traerlo de Firestore
            showStreakNotification(streakDays, notificationId)
        }
    /**
     * 🔥 Notificación de Racha basada en los días consecutivos registrados.
     */
    fun showStreakReminder(streakDays: Int, notificationId: Int) {
        val title: String
        val message: String

        when {
            streakDays == 0 -> {
                title = "¡Empieza tu racha hoy! 🚀"
                message = "Registra tu primera dosis o glucosa y comienza tu camino hacia el hábito saludable. 💪"
            }
            streakDays == 1 -> {
                title = "¡Primer paso dado! 👣"
                message = "Has completado tu primer día de registro. ¡Sigue así, cada día cuenta! 🌟"
            }
            streakDays in 2..6 -> {
                title = "¡Racha en progreso! 🎉"
                message = "Llevas $streakDays días consecutivos cuidando tu salud. ¡La constancia es tu superpoder! ⚡"
            }
            streakDays in 7..29 -> {
                title = "¡Una semana de éxito! 🏆"
                message = "Tu racha ya es de $streakDays días. ¡Impresionante disciplina, sigue acumulando logros! 🔥"
            }
            streakDays >= 30 -> {
                title = "¡Héroe del mes! 🥇"
                message = "Has alcanzado $streakDays días consecutivos. ¡Un hábito poderoso que transforma tu vida! ❤️"
            }
            else -> {
                title = "¡Racha activa! 🔥"
                message = "Tu racha actual es de $streakDays días. ¡No la rompas, tú puedes! 💯"
            }
        }

        showNotification(title, message, notificationId)
    }
    // ----------------------------------------------------------------------
    // --- 7. Recordatorios de Seguimiento (30 min después) ---
    // ----------------------------------------------------------------------

    /**
     * 🍽️ Recordatorio de seguimiento para comidas
     */
    fun showMealFollowup(notificationId: Int) {
        val titles = listOf(
            "🍽️ ¿Ya registraste tu comida?",
            "¡No olvides tu registro nutricional! 🥗",
            "Tu control depende de tus registros 📊"
        )
        val messages = listOf(
            "Han pasado 30 minutos desde tu comida. Registra lo que consumiste para mantener tu control.",
            "¿Ya anotaste tu almuerzo/cena? Es vital para ajustar tu tratamiento y mantener tu racha.",
            "Tu diario de comidas es tu mejor aliado. ¡Regístralo ahora y sigue tu progreso!"
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }

    /**
     * 💊 Recordatorio de seguimiento para medicamentos
     */
    fun showMedicationFollowup(notificationId: Int) {
        val titles = listOf(
            "💊 ¿Ya marcaste tu medicamento?",
            "¡No pierdas tu racha de medicación! 🔔",
            "Tu tratamiento necesita constancia ✅"
        )
        val messages = listOf(
            "Han pasado 30 minutos desde tu dosis. Si ya la tomaste, regístrala en la app.",
            "¿Ya anotaste tu medicamento? Mantén tu control y evita confusiones.",
            "Tu constancia es tu fuerza. Marca tu dosis para seguir tu progreso."
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }

    /**
     * 🩸 Recordatorio de seguimiento para glucosa
     */
    fun showGlucoseFollowup(notificationId: Int) {
        val titles = listOf(
            "🩸 ¿Ya registraste tu glucosa?",
            "¡Tus datos son poder! 📊",
            "No olvides tu medición ⚠️"
        )
        val messages = listOf(
            "Han pasado 30 minutos desde tu chequeo. Registra tu nivel para mantener tu historial completo.",
            "¿Ya anotaste tu glucosa? Es clave para entender tu progreso y ajustar tu tratamiento.",
            "Tu salud depende de tus registros. ¡Guarda tu medición ahora!"
        )
        showNotification(titles.random(), messages.random(), notificationId)
    }
}