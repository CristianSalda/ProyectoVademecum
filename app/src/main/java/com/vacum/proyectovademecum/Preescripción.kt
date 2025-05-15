package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class Preescripción : AppCompatActivity() {

    interface GoogleTranslateApi {
        @GET("language/translate/v2")
        suspend fun traducirTexto(
            @Query("q") texto: String,
            @Query("target") idiomaDestino: String,
            @Query("format") formato: String = "text",
            @Query("key") apiKey: String = NuevaPree.GOOGLE_TRANSLATE_API_KEY
        ): retrofit2.Response<NuevaPree.TranslationResponse>
    }

    interface OpenFdaApi {
        @GET("drug/label.json")
        suspend fun getMedicamentos(
            @Query("search") query: String,
            @Query("limit") limit: Int = 10
        ): retrofit2.Response<OpenFdaResponse>
    }

    private lateinit var busquedaMedicamentoAdapter: BusquedaPrescripcionAdapter
    private lateinit var traductorApi: GoogleTranslateApi
    private lateinit var openFdaApi: OpenFdaApi
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preescripcion)

        progressBar = findViewById(R.id.progressBar)
        traductorApi = Retrofit.Builder()
            .baseUrl(NuevaPree.GOOGLE_TRANSLATE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleTranslateApi::class.java)

        openFdaApi = Retrofit.Builder()
            .baseUrl(NuevaPree.OPEN_FDA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFdaApi::class.java)

        setupRecyclerViews()
        setupBusqueda()
        setupBotones()
    }

    private fun setupRecyclerViews() {
        busquedaMedicamentoAdapter = BusquedaPrescripcionAdapter(emptyList()) { medicamento, cantidad, tiempoProximaToma ->
            guardarPrescripcion(medicamento, cantidad, tiempoProximaToma)
        }
        findViewById<RecyclerView>(R.id.rvResultadosBusqueda).apply {
            layoutManager = LinearLayoutManager(this@Preescripción)
            adapter = busquedaMedicamentoAdapter
        }
    }

    private fun setupBusqueda() {
        findViewById<EditText>(R.id.searchEditText).apply {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length > 2) {
                        buscarMedicamentos(s.toString())
                    } else {
                        busquedaMedicamentoAdapter.updateList(emptyList())
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    buscarMedicamentos(text.toString())
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupBotones() {
        findViewById<ImageView>(R.id.imageView8).setOnClickListener {
            finish()
        }
    }

    private fun buscarMedicamentos(query: String) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                val traduccion = traductorApi.traducirTexto(
                    texto = query,
                    idiomaDestino = "en"
                )

                val queryEnIngles =
                    traduccion.body()?.data?.translations?.firstOrNull()?.translatedText ?: query

                val response = openFdaApi.getMedicamentos("openfda.brand_name:$queryEnIngles")

                if (response.isSuccessful && response.body()?.results != null) {
                    val medicamentos = response.body()!!.results!!
                    busquedaMedicamentoAdapter.updateList(medicamentos)
                } else {
                    Toast.makeText(
                        this@Preescripción,
                        "Error en la búsqueda",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error en buscarMedicamentos", e)
                Toast.makeText(this@Preescripción, "Error de conexión", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun guardarPrescripcion(
        medicamento: Medicamento,
        cantidad: Int,
        tiempoProximaToma: Int
    ) {
        val nombreOrdenEditText = findViewById<EditText>(R.id.etNombreOrden)
        val nombreOrden = nombreOrdenEditText.text.toString().trim()
        val nombreMedicamento = medicamento.openfda?.brand_name?.firstOrNull() ?: "Medicamento desconocido"

        if (nombreOrden.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa el nombre de la orden", Toast.LENGTH_SHORT).show()
            return
        }

        guardarPrescripcionEnFirestore(nombreOrden, nombreMedicamento, tiempoProximaToma, cantidad)
    }

    private fun guardarPrescripcionEnFirestore(
        nombreOrden: String,
        nombreMedicamento: String,
        tiempoEntreDosis: Int,
        cantidadDosis: Int
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val medicamentoPrescrito = hashMapOf(
            "nombre" to nombreMedicamento,
            "tiempoEntreDosis" to tiempoEntreDosis,
            "cantidadDosis" to cantidadDosis,
            "proximaToma" to System.currentTimeMillis() + tiempoEntreDosis * 60 * 60 * 1000L
        )

        val prescripcionData = hashMapOf(
            "nombreOrden" to nombreOrden,
            "medicamentos" to listOf(medicamentoPrescrito),
            "fechaCreacion" to FieldValue.serverTimestamp(),
            "usuarioId" to userId
        )

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .collection("prescripciones_virtuales")
            .add(prescripcionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Prescripción guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }
}