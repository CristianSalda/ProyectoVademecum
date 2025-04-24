package com.vacum.proyectovademecum

data class TraduccionResponse(
    val data: Data
) {
    data class Data(
        val translations: List<Translation>
    )

    data class Translation(
        val translatedText: String
    )
}
