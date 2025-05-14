package com.vacum.proyectovademecum

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vacum.proyectovademecum.NuevaPree.Companion.GOOGLE_TRANSLATE_API_KEY
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class FullScreenBuscarPorTipoActivity : AppCompatActivity() {

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
            @Query("limit") limit: Int = 100
        ): retrofit2.Response<OpenFdaResponse>
    }

    data class TranslationResponse(val data: TranslationData?)
    data class TranslationData(val translations: List<Translation>?)
    data class Translation(val translatedText: String?)

    data class OpenFdaResponse(val results: List<Medicamento>?)

    private lateinit var recyclerViewMedicamentos: RecyclerView
    private lateinit var adapter: FullScreenBuscarPorTipoAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var openFdaApi: OpenFdaApi
    private lateinit var traductorApi: GoogleTranslateApi
    private lateinit var tipoMedicamento: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_buscar_por_tipo)

        progressBar = findViewById(R.id.progressBar)
        recyclerViewMedicamentos = findViewById(R.id.recyclerViewFullScreen)
        recyclerViewMedicamentos.layoutManager = LinearLayoutManager(this)

        adapter = FullScreenBuscarPorTipoAdapter(emptyList())
        recyclerViewMedicamentos.adapter = adapter

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofitFda = Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        openFdaApi = retrofitFda.create(OpenFdaApi::class.java)

        val retrofitGoogle = Retrofit.Builder()
            .baseUrl("https://translation.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

        tipoMedicamento = intent.getStringExtra("tipoMedicamento") ?: ""
        findViewById<TextView>(R.id.toolbarTitle).text = "Medicamentos del tipo: $tipoMedicamento"

        if (tipoMedicamento.isNotEmpty()) {
            buscarMedicamentosPorTipo(tipoMedicamento)
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }

    private fun buscarMedicamentosPorTipo(tipo: String) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                // Traducimos el tipo (que está en español) a inglés para la búsqueda
                val traduccion = traductorApi.traducirTexto(
                    texto = tipo,
                    idiomaDestino = "en"
                )
                val tipoEnIngles = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText ?: tipo
                val tipoEnInglesLower = tipoEnIngles.trim().lowercase()

                // Intentando buscar solo por el valor del propósito (sin especificar el campo)
                val query = "\"${tipoEnInglesLower}\""
                Log.d("FullScreenTipo", "Buscando solo por valor (inglés): $query")
                val response = openFdaApi.getMedicamentos(search = query)

                if (response.isSuccessful && response.body()?.results != null) {
                    val medicamentosFiltrados = response.body()!!.results!!.filter { medicamento ->
                        medicamento.purpose?.any { it.trim().lowercase() == tipoEnInglesLower } == true
                    }.mapNotNull { medicamento ->
                        medicamento.openfda?.brand_name?.firstOrNull()?.let {
                            Medicamento(
                                openfda = medicamento.openfda,
                                purpose = medicamento.purpose,
                                indications_and_usage = medicamento.indications_and_usage,
                                warnings = medicamento.warnings,
                                when_using = medicamento.when_using,
                                ask_a_doctor = medicamento.ask_a_doctor,
                                stop_use = medicamento.stop_use
                            )
                        }
                    }
                    val translatedMedicamentos = awaitTranslation(medicamentosFiltrados)
                    adapter.updateList(translatedMedicamentos)
                } else {
                    Toast.makeText(
                        this@FullScreenBuscarPorTipoActivity,
                        "No se encontraron medicamentos para '$tipoMedicamento'",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("API_ERROR", "Error buscando por tipo (solo valor - inglés): ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al buscar medicamentos por tipo: ${e.message}")
                Toast.makeText(this@FullScreenBuscarPorTipoActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun awaitTranslation(medicamentos: List<Medicamento>): List<Medicamento> {
        return medicamentos.map { medicamento ->
            val translatedPurpose = medicamento.purpose?.joinToString(" ")?.let { translateText(it, "es") }
            val translatedIndications = medicamento.indications_and_usage?.joinToString(" ")?.let { translateText(it, "es") }
            medicamento.copy(
                purpose = translatedPurpose?.let { listOf(it) },
                indications_and_usage = translatedIndications?.let { listOf(it) }
            )
        }
    }

    private suspend fun translateText(text: String, targetLanguage: String): String? {
        return try {
            if (text.isEmpty()) return ""
            val response = traductorApi.traducirTexto(texto = text, idiomaDestino = targetLanguage)
            response.body()?.data?.translations?.firstOrNull()?.translatedText
        } catch (e: Exception) {
            Log.e("Translation Error", "Error traduciendo '$text': ${e.message}")
            null
        }
    }
}