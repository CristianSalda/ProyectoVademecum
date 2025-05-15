package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vacum.proyectovademecum.adapters.ResenaResumenAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ListaResenasActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var btnCerrarResultados: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView

    private lateinit var db: FirebaseFirestore
    private var tipo: String = "mejores"

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
        setContentView(R.layout.activity_lista_resenas)

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        btnCerrarResultados = findViewById(R.id.btnCerrarResultados)
        progressBar = findViewById(R.id.progressBarBusqueda)
        recyclerView = findViewById(R.id.rvListaResenas)

        recyclerView.layoutManager = LinearLayoutManager(this)
        db = FirebaseFirestore.getInstance()
        tipo = intent.getStringExtra("tipo") ?: "mejores"

        findViewById<ImageView>(R.id.btnAtras).setOnClickListener {
            finish()
        }

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamentos(query)
            }
        }

        btnCerrarResultados.setOnClickListener {
            recyclerView.adapter = null
            btnCerrarResultados.visibility = View.GONE
        }

        cargarResenas()
    }

    private fun cargarResenas() {
        val orden = if (tipo == "peores") Query.Direction.ASCENDING else Query.Direction.DESCENDING

        db.collection("resenas")
            .orderBy("estrellas", orden)
            .get()
            .addOnSuccessListener { documentos ->
                val lista = documentos.mapNotNull { it.toObject(Resena::class.java) }
                if (lista.isEmpty()) {
                    Toast.makeText(this, "No hay reseñas para mostrar", Toast.LENGTH_SHORT).show()
                }
                recyclerView.adapter = ResenaResumenAdapter(lista) {
                    // Acción opcional al tocar reseña
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar reseñas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buscarMedicamentos(nombre: String) {
        progressBar.visibility = View.VISIBLE
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
                            Uso: ${med.indications_and_usage?.firstOrNull() ?: "No indicado"}
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
                        recyclerView.adapter = MedicamentoAdapterPreescripcion(
                            listaTraducida,
                            onItemClick = { texto ->
                                val nombreMed = extraerNombreDeMarca(texto)
                                val intent = Intent(this@ListaResenasActivity, ActivityMostrarResenas::class.java)
                                intent.putExtra("nombreMedicamento", nombreMed)
                                startActivity(intent)
                            },
                            onAgregarClick = { /* No aplica aquí */ }
                        )
                        btnCerrarResultados.visibility = View.VISIBLE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ListaResenasActivity, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ListaResenasActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun extraerNombreDeMarca(texto: String): String {
        val regex = Regex("Marca: (.+)")
        return regex.find(texto)?.groupValues?.get(1)?.split("\n")?.firstOrNull() ?: "Desconocido"
    }
}
