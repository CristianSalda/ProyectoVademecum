package com.vacum.proyectovademecum

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val listaTextosTraducidos = mutableListOf<String>()
    val retrofitGoogle = Retrofit.Builder()
        .baseUrl("https://translation.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

    private lateinit var api: OpenFdaApi
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MedicamentoAdapter(
            listaTextosTraducidos,
            onItemClick = TODO(),
            onAgregarClick = TODO()
        )
        super.onCreate(savedInstanceState)

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
    }

    private fun buscarMedicamento(nombre: String) {
        lifecycleScope.launch {
            try {
                val traduccion = traductorApi.traducirTexto(
                    texto = nombre,
                    idiomaDestino = "en",
                    formato = "text",
                    apiKey = "AIzaSyB7KFKhaWH1NCWPrdpFoqzNijGfqWMb1ck"
                )

                val nombreEnIngles = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText ?: nombre
                val response = api.getMedicamentos("openfda.substance_name:$nombreEnIngles", 5)
                val medicamentos = response.body()?.results

                if (response.isSuccessful && medicamentos != null && medicamentos.isNotEmpty()) {
                    val listaTextosTraducidos = mutableListOf<String>()

                    for (med in medicamentos) {
                        val textoOriginal = """
                        Marca: ${med.openfda?.brand_name?.joinToString() ?: "Desconocida"}
                        Fabricante: ${med.openfda?.manufacturer_name?.joinToString() ?: "Desconocido"}
                        Uso: ${med.indications_and_usage?.joinToString() ?: "No indicado"}
                    """.trimIndent()

                        val traduccionTexto = traductorApi.traducirTexto(
                            texto = textoOriginal,
                            idiomaDestino = "es",
                            formato = "text",
                            apiKey = "AIzaSyB7KFKhaWH1NCWPrdpFoqzNijGfqWMb1ck"
                        )

                        val textoTraducido = traduccionTexto.body()?.data?.translations?.firstOrNull()?.translatedText
                            ?: textoOriginal

                        listaTextosTraducidos.add(textoTraducido)
                    }
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

                    recyclerView.adapter = MedicamentoAdapter(
                        listaTextosTraducidos,
                        onItemClick = TODO(),
                        onAgregarClick = TODO()
                    )
                } else {
                    // Manejar sin resultados
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}")
            }
        }
    }

}

