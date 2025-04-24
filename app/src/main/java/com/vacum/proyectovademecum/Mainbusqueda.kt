package com.vacum.proyectovademecum

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class Mainbusqueda : AppCompatActivity() {

    val retrofitGoogle = Retrofit.Builder()
        .baseUrl("https://translation.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

    private lateinit var api: OpenFdaApi
    private lateinit var searchInput: EditText
    private lateinit var searchButton: LinearLayout
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainbusqueda)

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchBar)
        resultText = findViewById(R.id.textViewResults)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(OpenFdaApi::class.java)

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamento(query)
            }
        }

        // Configurar el botón de regreso
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }
    private fun buscarMedicamento(nombre: String) {
        lifecycleScope.launch {
            try {
                val traduccion = traductorApi.traducirTexto(
                    texto = nombre,
                    idiomaDestino = "en",
                    formato = "text",
                    apiKey = "Api_Key_aqui"
                )

                val nombreEnIngles = traduccion.body()
                    ?.data
                    ?.translations
                    ?.firstOrNull()
                    ?.translatedText
                    ?: nombre

                val response = api.getMedicamentos("openfda.substance_name:$nombreEnIngles", 1)
                val medicamento = response.body()?.results?.firstOrNull()

                if (response.isSuccessful && medicamento != null) {
                    val marca = medicamento.openfda?.brand_name?.joinToString() ?: "Desconocida"
                    val fabricante = medicamento.openfda?.manufacturer_name?.joinToString() ?: "Desconocido"
                    val proposito = medicamento.purpose?.joinToString() ?: "No especificado"
                    val uso = medicamento.indications_and_usage?.joinToString() ?: "No indicado"

                    val textoOriginal = """
                    Brand: $marca
                    Manufacturer: $fabricante
                    Purpose: $proposito
                    Usage: $uso
                """.trimIndent()

                    val traduccion = traductorApi.traducirTexto(
                        texto = textoOriginal,
                        idiomaDestino = "es",
                        formato = "text",
                        apiKey = "Api_Key_Aqui"
                    )

                    if (traduccion.isSuccessful) {
                        val textoTraducido = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText
                        resultText.text = textoTraducido ?: textoOriginal
                    } else {
                        resultText.text = textoOriginal
                    }

                } else {
                    resultText.text = "No se encontró información para '$nombre'."
                }

            } catch (e: Exception) {
                resultText.text = "Error: ${e.message}"
                Log.e("API_ERROR", e.toString())
            }
        }
    }
}
