package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class Mainbusqueda : AppCompatActivity() {

    interface GoogleTranslateApi {
        @GET("language/translate/v2")
        suspend fun traducirTexto(
            @Query("q") texto: String,
            @Query("target") idiomaDestino: String,
            @Query("format") formato: String = "text",
            @Query("key") apiKey: String
        ): retrofit2.Response<TranslationResponse>
    }

    interface OpenFdaApi {
        @GET("drug/label.json")
        suspend fun getMedicamentos(
            @Query("search") query: String,
            @Query("limit") limit: Int
        ): retrofit2.Response<OpenFdaResponse>
    }

    data class TranslationResponse(val data: TranslationData?)
    data class TranslationData(val translations: List<Translation>?)
    data class Translation(val translatedText: String?)

    data class OpenFdaResponse(val results: List<Medicamento>?)

    private lateinit var adapter: MainBusquedaAdapter
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var traductorApi: GoogleTranslateApi
    private lateinit var openFdaApi: OpenFdaApi
    private lateinit var resultText: TextView
    private var primerMedicamentoMostrado: Medicamento? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainbusqueda)

        val retrofitGoogle = Retrofit.Builder()
            .baseUrl("https://translation.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

        val retrofitFda = Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        openFdaApi = retrofitFda.create(OpenFdaApi::class.java)

        searchInput = findViewById(R.id.searchInput)
        recyclerView = findViewById(R.id.recyclerView)
        resultText = findViewById(R.id.textViewResults)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = MainBusquedaAdapter(emptyList(), { medicamento ->
            showFullScreenMedicamento(medicamento)
        }, userId)

        recyclerView.adapter = adapter

        val query = intent.getStringExtra("query")
        if (!query.isNullOrEmpty()) {
            searchInput.setText(query)
            buscarMedicamento(query)
        }

        findViewById<ImageView>(R.id.btnSearch).setOnClickListener {
            performSearch()
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        findViewById<ImageView>(R.id.accountIcon).setOnClickListener {
            startActivity(Intent(this, ActivityPerfil::class.java))
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

    }

    private fun performSearch() {
        val query = searchInput.text.toString().trim()
        if (query.isNotEmpty()) {
            buscarMedicamento(query)
        } else {
            Toast.makeText(this, "Ingrese un término de búsqueda", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buscarMedicamento(nombre: String) {
        lifecycleScope.launch {
            try {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE

                val traduccion = traductorApi.traducirTexto(
                    texto = nombre,
                    idiomaDestino = "en",
                    apiKey = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
                )

                val nombreEnIngles = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText ?: nombre

                val response = openFdaApi.getMedicamentos("openfda.substance_name:$nombreEnIngles", 10)

                if (response.isSuccessful && response.body()?.results != null) {
                    val medicamentos = response.body()!!.results!!.mapNotNull { medicamento ->
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

                    adapter.updateList(medicamentos)

                    if (medicamentos.isNotEmpty()) {
                        primerMedicamentoMostrado = medicamentos.first()

                        val textoOriginal = buildString {
                            medicamentos.first().openfda?.brand_name?.firstOrNull()?.let {
                                append("Brand: $it\n")
                            }
                            medicamentos.first().openfda?.manufacturer_name?.firstOrNull()?.let {
                                append("Manufacturer: $it\n")
                            }
                            medicamentos.first().purpose?.firstOrNull()?.let {
                                append("Purpose: $it\n")
                            }
                            medicamentos.first().indications_and_usage?.firstOrNull()?.let {
                                append("Usage: $it\n")
                            }
                        }

                        val traduccionEsp = traductorApi.traducirTexto(
                            texto = textoOriginal,
                            idiomaDestino = "es",
                            apiKey = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
                        )

                        resultText.text = if (traduccionEsp.isSuccessful) {
                            traduccionEsp.body()?.data?.translations?.firstOrNull()?.translatedText ?: textoOriginal
                        } else {
                            textoOriginal
                        }
                    } else {
                        resultText.text = "No se encontraron resultados para '$nombre'"
                        Toast.makeText(this@Mainbusqueda, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                    }

                    guardarEnHistorial(nombre)

                } else {
                    resultText.text = "Error en la búsqueda"
                    Toast.makeText(this@Mainbusqueda, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error en buscarMedicamento", e)
                resultText.text = "Error de conexión: ${e.message}"
                Toast.makeText(this@Mainbusqueda, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            }
        }
    }

    private fun guardarEnHistorial(nombre: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val historial = HistorialBusqueda(
            nombre = nombre,
            fecha = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .collection("historial")
            .add(historial)
            .addOnFailureListener { e ->
                Log.e("Historial", "Error al guardar historial", e)
            }
    }

    private fun showFullScreenMedicamento(medicamento: Medicamento) {
        val intent = Intent(this, FullScreenMedicamentoActivity::class.java).apply {
            medicamento.openfda?.brand_name?.firstOrNull()?.let {
                putExtra("BRAND_NAME", it)
            }
            medicamento.openfda?.manufacturer_name?.firstOrNull()?.let {
                putExtra("MANUFACTURER", it)
            }
            medicamento.purpose?.firstOrNull()?.let {
                putExtra("PURPOSE", it)
            }
            medicamento.indications_and_usage?.firstOrNull()?.let {
                putExtra("INDICATIONS", it)
            }
            medicamento.warnings?.firstOrNull()?.let {
                putExtra("WARNINGS", it)
            }
            medicamento.when_using?.firstOrNull()?.let {
                putExtra("WHEN_USING", it)
            }
            medicamento.ask_a_doctor?.firstOrNull()?.let {
                putExtra("ASK_DOCTOR", it)
            }
            medicamento.stop_use?.firstOrNull()?.let {
                putExtra("STOP_USE", it)
            }
        }
        startActivity(intent)
    }
}