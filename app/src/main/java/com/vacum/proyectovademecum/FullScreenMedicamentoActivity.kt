package com.vacum.proyectovademecum

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FullScreenMedicamentoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_full_screen_medicamento)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mostrarDetallesEnPantalla()

        val btnBack = findViewById<ImageView>(R.id.backButton)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun mostrarDetallesEnPantalla() {
        val tvDetalle = findViewById<TextView>(R.id.tvNombre)

        val nombre = intent.getStringExtra("BRAND_NAME") ?: "Nombre no disponible"

        val descripcion = buildString {
            append("Nombre: $nombre\n\n")

            intent.getStringExtra("MANUFACTURER")?.let {
                append("Fabricante: $it\n\n")
            }
            intent.getStringExtra("PURPOSE")?.let {
                append("Propósito: $it\n\n")
            }
            intent.getStringExtra("INDICATIONS")?.let {
                append("Indicaciones y uso: $it\n\n")
            }
            intent.getStringExtra("WARNINGS")?.let {
                append("Advertencias: $it\n\n")
            }
            intent.getStringExtra("WHEN_USING")?.let {
                append("Cuándo usar: $it\n\n")
            }
            intent.getStringExtra("ASK_DOCTOR")?.let {
                append("Consulte a su médico: $it\n\n")
            }
            intent.getStringExtra("STOP_USE")?.let {
                append("Deje de usar: $it")
            }
        }

        tvDetalle.text = descripcion
    }
}