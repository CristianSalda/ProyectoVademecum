package com.vacum.proyectovademecum.helpers

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.vacum.proyectovademecum.GoogleTranslateApi
import com.vacum.proyectovademecum.MedicamentoAdapterPreescripcion
import com.vacum.proyectovademecum.OpenFdaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MedicamentoSearchHelper(
    private val context: Context,
    private val scope: LifecycleCoroutineScope,
    private val recyclerView: RecyclerView,
    private val onItemClick: (String) -> Unit
) {
    private val retrofitTranslate = Retrofit.Builder()
        .baseUrl("https://translation.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val traductorApi = retrofitTranslate.create(GoogleTranslateApi::class.java)

    private val retrofitFda = Retrofit.Builder()
        .baseUrl("https://api.fda.gov/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiFda = retrofitFda.create(OpenFdaApi::class.java)

    suspend fun buscar(nombre: String) {
        try {
            val nombreEnIngles = withContext(Dispatchers.IO) {
                traductorApi.traducirTexto(
                    texto = nombre,
                    idiomaDestino = "en",
                    formato = "text",
                    apiKey = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
                ).body()?.data?.translations?.firstOrNull()?.translatedText ?: nombre
            }

            val response = withContext(Dispatchers.IO) {
                apiFda.getMedicamentos("openfda.substance_name:$nombreEnIngles", 10)
            }

            if (response.isSuccessful) {
                val resultados = response.body()?.results ?: emptyList()

                if (resultados.isEmpty()) {
                    showList(listOf("No se encontraron resultados para '$nombre'"))
                    return
                }

                val textos = resultados.map { med ->
                    """
                        Marca: ${med.openfda?.brand_name?.firstOrNull() ?: "Desconocida"}
                        Fabricante: ${med.openfda?.manufacturer_name?.firstOrNull() ?: "Desconocido"}
                        💊 Uso💊: ${med.indications_and_usage?.firstOrNull() ?: "No indicado"}
                        ⚠️Advertencias ⚠️: ${med.warnings?.firstOrNull() ?: "No especificado"}
                        ❗Cuando usar con precaución❗: ${med.when_using?.firstOrNull() ?: "No especificado"}
                        ❓Consultar al médico si...❓: ${med.ask_a_doctor?.firstOrNull() ?: "No indicado"}
                        🚫Dejar de usar si...🚫: ${med.stop_use?.firstOrNull() ?: "No indicado"}
                    """.trimIndent()
                }

                val traducidos = textos.map {
                    withContext(Dispatchers.IO) {
                        traductorApi.traducirTexto(
                            texto = it,
                            idiomaDestino = "es",
                            formato = "text",
                            apiKey = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
                        ).body()?.data?.translations?.firstOrNull()?.translatedText ?: it
                    }
                }

                showList(traducidos)
            } else {
                showList(listOf("Error en la API: ${response.message()}"))
            }
        } catch (e: Exception) {
            showList(listOf("Error: ${e.localizedMessage ?: "desconocido"}"))
            Toast.makeText(context, "Error al buscar medicamentos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showList(resultados: List<String>) {
        recyclerView.adapter = MedicamentoAdapterPreescripcion(
            resultados,
            onItemClick = onItemClick,
            onAgregarClick = {} // En reseñas no necesitas agregar
        )
    }
}
