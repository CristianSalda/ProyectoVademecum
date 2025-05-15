package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vacum.proyectovademecum.NuevaPree.Companion.GOOGLE_TRANSLATE_API_KEY
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class ActivityBuscarPorTipo : AppCompatActivity() {

    interface GoogleTranslateApi {
        @GET("language/translate/v2")
        suspend fun traducirTexto(
            @Query("q") texto: String,
            @Query("target") idiomaDestino: String,
            @Query("format") formato: String = "text",
            @Query("key") apiKey: String = GOOGLE_TRANSLATE_API_KEY
        ): retrofit2.Response<TranslationResponse>
    }

    interface OpenFdaApi {
        @GET("drug/label.json")
        suspend fun getMedicamentos(
            @Query("search") search: String,
            @Query("limit") limit: Int = 200
        ): retrofit2.Response<OpenFdaResponse>
    }

    data class TranslationResponse(val data: TranslationData?)
    data class TranslationData(val translations: List<Translation>?)
    data class Translation(val translatedText: String?)

    data class OpenFdaResponse(val results: List<Medicamento>?)

    private lateinit var recyclerViewTipos: RecyclerView
    private lateinit var buscarPorTipoAdapter: BuscarPorTipoAdapter
    private lateinit var openFdaApi: OpenFdaApi
    private lateinit var traductorApi: GoogleTranslateApi
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_por_tipo)

        progressBar = findViewById(R.id.progressBar)

        val retrofitFda = Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        openFdaApi = retrofitFda.create(OpenFdaApi::class.java)

        val retrofitGoogle = Retrofit.Builder()
            .baseUrl("https://translation.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

        recyclerViewTipos = findViewById(R.id.recyclerViewTipos)
        recyclerViewTipos.layoutManager = GridLayoutManager(this, 2)

        buscarPorTipoAdapter = BuscarPorTipoAdapter(emptyList()) { tipo ->
            mostrarMedicamentosPorTipo(tipo)
        }
        recyclerViewTipos.adapter = buscarPorTipoAdapter

        cargarTiposDeMedicamento()

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.accountIcon).setOnClickListener {
            startActivity(Intent(this, ActivityPerfil::class.java))
        }
    }

    private fun cargarTiposDeMedicamento() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val query = "_exists_:purpose"
                val response = openFdaApi.getMedicamentos(search = query, limit = 200)

                if (response.isSuccessful && response.body()?.results != null) {
                    val allPurposesEnglish = response.body()!!.results!!
                        .filter { !it.purpose.isNullOrEmpty() }
                        .flatMap { it.purpose!! }
                        .distinct()
                        .take(100)

                    val translatedPurposes = mutableSetOf<String>()

                    allPurposesEnglish.forEach { englishPurpose ->
                        try {
                            val translationResponse = traductorApi.traducirTexto(
                                texto = englishPurpose,
                                idiomaDestino = "es"
                            )
                            val translatedText = translationResponse.body()?.data?.translations?.firstOrNull()?.translatedText
                            if (!translatedText.isNullOrEmpty()) {
                                translatedPurposes.add(translatedText)
                            } else {
                                translatedPurposes.add(englishPurpose) // Fallback
                            }
                        } catch (e: Exception) {
                            Log.e("Translation Error", "Error al traducir propósito '$englishPurpose': ${e.message}")
                            translatedPurposes.add(englishPurpose) // Fallback en caso de error
                        }
                    }
                    buscarPorTipoAdapter.updateList(translatedPurposes.toList())
                } else {
                    Toast.makeText(this@ActivityBuscarPorTipo, "Error al cargar los tipos de medicamento", Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "Error obteniendo medicamentos para propósitos: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error de conexión al cargar tipos: ${e.message}")
                Toast.makeText(this@ActivityBuscarPorTipo, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun mostrarMedicamentosPorTipo(tipo: String) {
        val intent = Intent(this, FullScreenBuscarPorTipoActivity::class.java)
        intent.putExtra("tipoMedicamento", tipo)
        startActivity(intent)
    }
}