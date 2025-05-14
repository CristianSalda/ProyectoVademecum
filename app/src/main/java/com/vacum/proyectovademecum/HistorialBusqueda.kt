package com.vacum.proyectovademecum

import com.google.firebase.Timestamp


data class HistorialBusqueda(
    val fecha: Timestamp = Timestamp.now(),
    val nombre: String = "",
    val usuarioId: String = ""
)