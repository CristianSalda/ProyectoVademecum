package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AgregarResenaActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var descripcion: EditText
    private lateinit var ratingBar: RatingBar
    private lateinit var btnPublicar: Button
    private lateinit var tvMedicamentoSeleccionado: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCerrarResultados: Button
    private lateinit var btnEliminarMedicamento: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var medicamentoSeleccionado: String? = null
    private var origen: String? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_resena)

        origen = intent.getStringExtra("origen")

        // Inicialización de vistas
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)
        descripcion = findViewById(R.id.etDescripcion)
        ratingBar = findViewById(R.id.ratingBar)
        btnPublicar = findViewById(R.id.btnPublicar)
        progressBar = findViewById(R.id.progressBar)
        tvMedicamentoSeleccionado = findViewById(R.id.tvMedicamentoSeleccionado)
        btnCerrarResultados = findViewById(R.id.btnCerrarResultados)
        btnEliminarMedicamento = findViewById(R.id.btnEliminarMedicamento)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.visibility = View.GONE
        btnCerrarResultados.visibility = View.GONE
        tvMedicamentoSeleccionado.visibility = View.GONE
        btnEliminarMedicamento.visibility = View.GONE

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamentos(query)
            }
        }

        btnCerrarResultados.setOnClickListener {
            recyclerView.adapter = null
            recyclerView.visibility = View.GONE
            btnCerrarResultados.visibility = View.GONE
        }

        btnEliminarMedicamento.setOnClickListener {
            medicamentoSeleccionado = null
            tvMedicamentoSeleccionado.text = ""
            tvMedicamentoSeleccionado.visibility = View.GONE
            btnEliminarMedicamento.visibility = View.GONE
        }

        btnPublicar.setOnClickListener {
            guardarResena()
        }

        findViewById<ImageView>(R.id.btnAtras).setOnClickListener {
            volverAOrigen()
        }

        // Si se recibió medicamento preseleccionado desde otra activity
        intent.getStringExtra("nombreMedicamento")?.let {
            medicamentoSeleccionado = it
            tvMedicamentoSeleccionado.text = "Medicamento seleccionado:\n$it"
            tvMedicamentoSeleccionado.visibility = View.VISIBLE
            btnEliminarMedicamento.visibility = View.VISIBLE
        }
    }

    private fun volverAOrigen() {
        when (origen) {
            "misResenas" -> startActivity(Intent(this, MisResenasActivity::class.java))
            "mostrarResenas" -> startActivity(Intent(this, ActivityMostrarResenas::class.java))
            else -> finish()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        searchButton.isEnabled = !show
        searchInput.isEnabled = !show
    }

    private fun buscarMedicamentos(nombre: String) {
        showLoading(true)
        recyclerView.visibility = View.GONE
        btnCerrarResultados.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val nombreEnIngles = withContext(Dispatchers.IO) {
                    traductorApi.traducirTexto(
                        texto = nombre,
                        idiomaDestino = "en",
                        formato = "text",
                        apiKey = "TU_API_KEY"
                    ).body()?.data?.translations?.firstOrNull()?.translatedText ?: nombre
                }

                val response = withContext(Dispatchers.IO) {
                    apiFda.getMedicamentos("openfda.substance_name:$nombreEnIngles", 10)
                }

                if (response.isSuccessful) {
                    val resultados = response.body()?.results ?: emptyList()

                    val listaTraducida = resultados.map { med ->
                        val texto = """
                            Marca: ${med.openfda?.brand_name?.firstOrNull() ?: "Desconocida"}
                            Fabricante: ${med.openfda?.manufacturer_name?.firstOrNull() ?: "Desconocido"}
                        """.trimIndent()

                        withContext(Dispatchers.IO) {
                            traductorApi.traducirTexto(
                                texto = texto,
                                idiomaDestino = "es",
                                formato = "text",
                                apiKey = "TU_API_KEY"
                            ).body()?.data?.translations?.firstOrNull()?.translatedText ?: texto
                        }
                    }

                    withContext(Dispatchers.Main) {
                        configurarAdapter(listaTraducida)
                        recyclerView.visibility = View.VISIBLE
                        btnCerrarResultados.visibility = View.VISIBLE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AgregarResenaActivity, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AgregarResena", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AgregarResenaActivity, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private fun configurarAdapter(medicamentos: List<String>) {
        recyclerView.adapter = MedicamentoAdapterPreescripcion(
            medicamentos,
            onItemClick = { mostrarDetalles(it) },
            onAgregarClick = { texto ->
                if (medicamentoSeleccionado != null) {
                    Toast.makeText(this, "Ya tienes un medicamento seleccionado", Toast.LENGTH_SHORT).show()
                } else {
                    val marca = extraerCampo(texto, "Marca:")
                    val fabricante = extraerCampo(texto, "Fabricante:")
                    val resumen = "$marca - $fabricante"

                    medicamentoSeleccionado = resumen
                    tvMedicamentoSeleccionado.text = "Seleccionado: $resumen"
                    tvMedicamentoSeleccionado.visibility = View.VISIBLE
                    btnEliminarMedicamento.visibility = View.VISIBLE
                    Toast.makeText(this, "Seleccionado: $resumen", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun mostrarDetalles(texto: String) {
        AlertDialog.Builder(this)
            .setTitle("Detalles del Medicamento")
            .setMessage(texto)
            .setPositiveButton("Cerrar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun extraerCampo(texto: String, etiqueta: String): String {
        val regex = Regex("$etiqueta\\s*(.*)")
        return regex.find(texto)?.groups?.get(1)?.value?.trim() ?: "Desconocido"
    }

    private fun guardarResena() {
        val user = auth.currentUser ?: return
        val medicamento = medicamentoSeleccionado
        val estrellas = ratingBar.rating.toInt()
        val descripcionTexto = descripcion.text.toString().trim()

        if (medicamento.isNullOrEmpty()) {
            Toast.makeText(this, "Debe seleccionar un medicamento", Toast.LENGTH_SHORT).show()
            return
        }

        if (estrellas == 0) {
            Toast.makeText(this, "Seleccione una cantidad de estrellas", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevaResena = hashMapOf(
            "medicamento" to medicamento,
            "estrellas" to estrellas,
            "descripcion" to descripcionTexto,
            "usuarioId" to user.uid
        )

        firestore.collection("resenas")
            .add(nuevaResena)
            .addOnSuccessListener {
                Toast.makeText(this, "Reseña publicada correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar reseña", Toast.LENGTH_SHORT).show()
            }
    }
}
