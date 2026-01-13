package com.example.diabetes_app // Nombre del paquete actualizado

import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.diabetes_app.databinding.ActivityLoginBinding // Ruta de binding actualizada
import com.google.android.material.button.MaterialButton // Necesario para evitar conflictos
import com.google.android.material.textfield.TextInputEditText // Necesario para acceder a los campos

// Asegúrate de importar tu actividad de Olvidé Contraseña
import com.example.diabetes_app.ForgotPasswordActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private companion object {
        private const val TAG = "LoginActivity_DiabetesApp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // El error indica que binding.buttonLogin falló.
        // Asumiendo que el binding sí genera la propiedad, la clase es MaterialButton.
        // El sistema de build debe estar limpio para que esto funcione:
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        binding.buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // --- CORRECCIÓN AQUÍ ---
        // Reemplazamos el Toast por la navegación a ForgotPasswordActivity.
        binding.textForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
        // --- FIN DE LA CORRECCIÓN ---
    }

    override fun onStart() {
        super.onStart()
        // Comprobar si el usuario ya ha iniciado sesión
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si ya hay sesión, ir directamente a la selección de diagnóstico
            updateUI(currentUser)
        }
    }

    private fun performLogin() {
        // Acceder a los campos de texto
        val emailOrUser = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (emailOrUser.isEmpty()) {
            // NOTA: Usar el método setError en TextInputEditText es la forma correcta.
            binding.editTextEmail.error = "Por favor, ingresa tu correo o usuario."
            binding.editTextEmail.requestFocus()
            return
        }

        // Simplificado: verifica si parece un email.
        if (!Patterns.EMAIL_ADDRESS.matcher(emailOrUser).matches()) {
            binding.editTextEmail.error = "Por favor, ingresa un correo electrónico válido."
            binding.editTextEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            // NOTA: Usar el método setError en TextInputEditText es la forma correcta.
            binding.editTextPassword.error = "Por favor, ingresa tu contraseña."
            binding.editTextPassword.requestFocus()
            return
        }

        auth.signInWithEmailAndPassword(emailOrUser, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                    // Después de un inicio de sesión exitoso, ir a la selección de diagnóstico
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    val exception = task.exception
                    var errorMessage = "Error de autenticación. Intenta de nuevo."
                    if (exception != null) {
                        errorMessage = when (exception) {
                            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No se encontró una cuenta con este correo."
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "La contraseña es incorrecta. Por favor, verifica."
                            else -> "No se pudo iniciar sesión. Revisa tu conexión o inténtalo más tarde."
                        }
                    }
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- MÉTODO updateUI ACTUALIZADO ---
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            Log.i(TAG, "Usuario ${currentUser.email} autenticado. Navegando a la pantalla de selección de diagnóstico.")
            // Cambiar la navegación de MainActivity a DiagnosisSelectionActivity
            val intent = Intent(this, DiagnosisSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpia el stack de actividades
            startActivity(intent)
            finish() // Finaliza LoginActivity para que el usuario no pueda volver con el botón "atrás"
        } else {
            // Si currentUser es null, el usuario no está autenticado y debe permanecer en LoginActivity.
            Log.i(TAG, "Usuario no autenticado.")
        }
    }
}
