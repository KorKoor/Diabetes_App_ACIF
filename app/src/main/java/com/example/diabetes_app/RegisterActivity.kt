package com.example.diabetes_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.diabetes_app.databinding.ActivityRegisterBinding

// Importa tu LoginActivity para la navegación
import com.example.diabetes_app.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    private companion object {
        private const val TAG = "RegisterActivity_DiabetesApp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Listener para el botón de Registrar
        binding.buttonPerformRegister.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        // Obtener los datos de los EditText
        val name = binding.editTextNameRegister.text.toString().trim()
        val email = binding.editTextEmailRegister.text.toString().trim()
        val password = binding.editTextPasswordRegister.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPasswordRegister.text.toString().trim()

        // --- Validaciones ---
        if (name.isEmpty()) {
            binding.editTextNameRegister.error = "Por favor, ingresa tu nombre."
            binding.editTextNameRegister.requestFocus()
            return
        }

        if (email.isEmpty()) {
            binding.editTextEmailRegister.error = "Por favor, ingresa tu correo electrónico."
            binding.editTextEmailRegister.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmailRegister.error = "Por favor, ingresa un correo electrónico válido."
            binding.editTextEmailRegister.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.editTextPasswordRegister.error = "Por favor, ingresa una contraseña."
            binding.editTextPasswordRegister.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.editTextPasswordRegister.error = "La contraseña debe tener al menos 6 caracteres."
            binding.editTextPasswordRegister.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.editTextConfirmPasswordRegister.error = "Por favor, confirma tu contraseña."
            binding.editTextConfirmPasswordRegister.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.editTextConfirmPasswordRegister.error = "Las contraseñas no coinciden."
            binding.editTextConfirmPasswordRegister.text?.clear()
            binding.editTextConfirmPasswordRegister.requestFocus()
            return
        }

        // --- Crear usuario en Firebase ---
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val firebaseUser: FirebaseUser? = auth.currentUser

                    // Actualizar el perfil del usuario con el nombre
                    if (firebaseUser != null && name.isNotEmpty()) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.d(TAG, "Perfil de usuario actualizado con nombre: $name")
                                } else {
                                    Log.w(TAG, "Fallo al actualizar perfil de usuario.", profileTask.exception)
                                }
                            }
                    }

                    // Envía el correo de verificación.
                    firebaseUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(
                                    baseContext,
                                    "Registro exitoso. Por favor, verifica tu correo electrónico para iniciar sesión.",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.d(TAG, "Correo de verificación enviado.")
                            } else {
                                Log.w(TAG, "sendEmailVerification:failure", verificationTask.exception)
                                Toast.makeText(
                                    baseContext,
                                    "Registro exitoso, pero falló el envío del correo de verificación.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            // Navega a LoginActivity, sin importar el resultado del envío del correo.
                            // Esto asegura que el usuario no pueda continuar hasta iniciar sesión
                            // y verificar su correo.
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                            "La contraseña proporcionada es demasiado débil."
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                            "El formato del correo electrónico no es válido."
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                            "Este correo electrónico ya está registrado. Intenta iniciar sesión."
                        else -> {
                            Log.e(TAG, "Error de registro no manejado: ${exception?.message}")
                            "Error en el registro. Inténtalo más tarde."
                        }
                    }
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }
}