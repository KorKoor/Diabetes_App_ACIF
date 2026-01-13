package com.example.diabetes_app

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diabetes_app.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Actividad encargada de gestionar el restablecimiento de contraseña.
 *
 * Asume la existencia de:
 * 1. Un layout activity_forgot_password.xml.
 * 2. Componentes en el layout con IDs: editTextEmail, buttonResetPassword, buttonBack, progressBarLoading.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    // Se asume la existencia de la clase de View Binding generada por Android.
    private lateinit var binding: ActivityForgotPasswordBinding
    // Instancia de FirebaseAuth para manejar la lógica de restablecimiento.
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización del View Binding
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de Firebase Auth
        auth = FirebaseAuth.getInstance()

        setupListeners()
    }

    /**
     * Configura los listeners de los botones de la actividad.
     */
    private fun setupListeners() {
        // Maneja el botón de retroceso (asumiendo un ID 'buttonBack' en el layout)
        binding.buttonBack.setOnClickListener {
            // Usa el despachador de botón de regreso moderno
            onBackPressedDispatcher.onBackPressed()
        }

        // Maneja el clic en el botón de restablecer contraseña
        binding.buttonResetPassword.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }
    }

    /**
     * Valida que el campo de correo electrónico no esté vacío y tenga un formato válido.
     */
    private fun validateEmail(email: String): Boolean {
        // Se asume que editTextEmail está envuelto en un TextInputLayout para el manejo de errores
        return if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Ingresa una dirección de correo válida."
            false
        } else {
            binding.editTextEmail.error = null // Limpia el error si es válido
            true
        }
    }

    /**
     * Muestra u oculta la barra de progreso y deshabilita los elementos interactivos.
     */
    private fun showLoading(isLoading: Boolean) {
        // Asume un ProgressBar con id 'progressBarLoading'
        binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonResetPassword.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
    }

    /**
     * Envía la solicitud de restablecimiento de contraseña usando Firebase Auth.
     */
    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // Éxito: Se ha enviado el correo.
                    Toast.makeText(
                        this,
                        "Se ha enviado un enlace de restablecimiento a tu correo. ¡Revisa tu bandeja de entrada!",
                        Toast.LENGTH_LONG
                    ).show()
                    // Finaliza la actividad para regresar a la pantalla de Login/Inicio
                    finish()
                } else {
                    // Error: Manejo de fallos (ej: correo no registrado, problema de red)
                    val errorMessage = task.exception?.message ?: "Error desconocido al enviar la solicitud."
                    Toast.makeText(
                        this,
                        "Error: $errorMessage. Por favor, verifica el correo.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("ForgotPassword", "Fallo al restablecer contraseña para $email: $errorMessage")
                }
            }
    }
}
