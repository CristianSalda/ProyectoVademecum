package com.vacum.proyectovademecum

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Resena(
    var medicamento: String = "",
    var descripcion: String? = null,
    var estrellas: Int = 0,
    var usuarioId: String = "",
    var id: String? = null
) : Parcelable

