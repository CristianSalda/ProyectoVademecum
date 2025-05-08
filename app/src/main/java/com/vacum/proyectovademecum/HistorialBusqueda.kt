package com.vacum.proyectovademecum

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

data class HistorialBusqueda(
    val fecha: Long = 0L,
    val nombre: String = "",
    val usuarioId: String = ""
)