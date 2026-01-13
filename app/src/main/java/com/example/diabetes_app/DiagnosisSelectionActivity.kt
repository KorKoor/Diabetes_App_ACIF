package com.example.diabetes_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diabetes_app.databinding.ActivityDiagnosisSelectionBinding
import android.util.Log

class DiagnosisSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiagnosisSelectionBinding
    private val handler = Handler(Looper.getMainLooper())

    private companion object {
        private const val TAG = "DiagnosisSelection"
        private const val INITIAL_LOAD_DELAY_MS = 1000L // 1 segundo de carga inicial
        private const val SELECTION_LOAD_DELAY_MS = 800L // 0.8 segundos de carga al seleccionar
        // Definimos la única opción disponible
        private const val DIABETES_DIAGNOSIS = "Diabetes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialmente mostrar la capa de carga
        showLoading(true)
        binding.mainContent.visibility = View.GONE

        // Simular carga inicial
        handler.postDelayed({
            showLoading(false)
            binding.mainContent.visibility = View.VISIBLE
            setupListeners()
        }, INITIAL_LOAD_DELAY_MS)
    }

    private fun setupListeners() {
        // Al hacer clic en la tarjeta de Diabetes, disparamos la misma acción que el botón
        binding.cardDiabetes.setOnClickListener {
            handleDiagnosisSelection(DIABETES_DIAGNOSIS)
        }

        // El nuevo botón de confirmación será el principal disparador
        binding.btnConfirmSelection.setOnClickListener {
            handleDiagnosisSelection(DIABETES_DIAGNOSIS)
        }

        // NOTA: Se ha eliminado la referencia a binding.cardNone
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Deshabilitar la interacción con el contenido principal mientras carga
        // Utilizamos el botón de confirmación para deshabilitar las interacciones principales
        binding.btnConfirmSelection.isEnabled = !isLoading
        binding.cardDiabetes.isEnabled = !isLoading
    }

    private fun handleDiagnosisSelection(diagnosis: String) {
        Log.d(TAG, "Diagnóstico seleccionado: $diagnosis")
        Toast.makeText(this, "Cargando configuración para: $diagnosis...", Toast.LENGTH_SHORT).show()

        // Mostrar la animación de carga al seleccionar
        showLoading(true)

        // Retardo para simular la transición y la configuración de la selección
        handler.postDelayed({
            navigateToMain(diagnosis)
        }, SELECTION_LOAD_DELAY_MS)
    }

    private fun navigateToMain(diagnosis: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_DIAGNOSIS", diagnosis)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
