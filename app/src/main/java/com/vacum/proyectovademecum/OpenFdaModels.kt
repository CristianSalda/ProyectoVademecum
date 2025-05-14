package com.vacum.proyectovademecum


data class OpenFdaResponse(val results: List<Medicamento>)


    data class Medicamento (
        val openfda: OpenFdaInfo?,
        val purpose: List<String>?,
        val indications_and_usage: List<String>?,
        val warnings: List<String>?,
        val when_using: List<String>?,
        val ask_a_doctor: List<String>?,
        val stop_use: List<String>?
    ) {
    }

data class OpenFdaInfo(
        val brand_name: List<String>?,
        val manufacturer_name: List<String>?,
        val substance_name: List<String>?
    )
