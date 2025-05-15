package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.vacum.proyectovademecum.adapters.ResenaResumenAdapter

class ResenasInicioActivity : AppCompatActivity() {

    private lateinit var rvMejores: RecyclerView
    private lateinit var rvPeores: RecyclerView
    private lateinit var rvResultados: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var btnCerrarResultados: Button
    private lateinit var progressBarBusqueda: ProgressBar

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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
        setContentView(R.layout.activity_resenas_inicio)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        rvMejores = findViewById(R.id.rvMejores)
        rvPeores = findViewById(R.id.rvPeores)
        rvResultados = findViewById(R.id.rvResultados)
        btnCerrarResultados = findViewById(R.id.btnCerrarResultados)
        progressBarBusqueda = findViewById(R.id.progressBarBusqueda)

        rvMejores.layoutManager = LinearLayoutManager(this)
        rvPeores.layoutManager = LinearLayoutManager(this)
        rvResultados.layoutManager = LinearLayoutManager(this)

        rvResultados.visibility = View.GONE
        btnCerrarResultados.visibility = View.GONE
        progressBarBusqueda.visibility = View.GONE

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamentos(query)
            }
        }

        btnCerrarResultados.setOnClickListener {
            rvResultados.adapter = null
            rvResultados.visibility = View.GONE
            btnCerrarResultados.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnMisResenas).setOnClickListener {
            startActivity(Intent(this, MisResenasActivity::class.java))
        }

        val btnAtras = findViewById<ImageView>(R.id.btnAtras)
        btnAtras.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val tipo = doc.getString("tipoPersona")?.lowercase()

                        when (tipo) {
                            "natural" -> {
                                startActivity(Intent(this, Mainnatural::class.java))
                                finish()
                            }
                            "especialista" -> {
                                startActivity(Intent(this, Mainespecialista::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "Tipo de usuario no reconocido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "No se pudo verificar el tipo de usuario", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        }


        findViewById<TextView>(R.id.verMasMejores).setOnClickListener {
            val intent = Intent(this, ListaResenasActivity::class.java)
            intent.putExtra("tipo", "mejores")
            startActivity(intent)
        }

        findViewById<TextView>(R.id.verMasPeores).setOnClickListener {
            val intent = Intent(this, ListaResenasActivity::class.java)
            intent.putExtra("tipo", "peores")
            startActivity(intent)
        }


        cargarMejoresResenas()
        cargarPeoresResenas()
    }

    private fun buscarMedicamentos(nombre: String) {
        progressBarBusqueda.visibility = View.VISIBLE
        rvResultados.visibility = View.GONE
        btnCerrarResultados.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val nombreEnIngles = withContext(Dispatchers.IO) {
                    traductorApi.traducirTexto(
                        texto = nombre,
                        idiomaDestino = "en",
                        formato = "text",
                        apiKey = "TU_API_KEY_GOOGLE_TRANSLATE"
                    ).body()?.data?.translations?.firstOrNull()?.translatedText ?: nombre
                }

                val response = withContext(Dispatchers.IO) {
                    apiFda.getMedicamentos("openfda.substance_name:$nombreEnIngles", 10)
                }

                if (response.isSuccessful) {
                    val resultados = response.body()?.results ?: emptyList()

                    if (resultados.isNotEmpty()) {
                        val listaTextos = resultados.map { med ->
                            """
                            Marca: ${med.openfda?.brand_name?.firstOrNull() ?: "Desconocida"}
                            Fabricante: ${med.openfda?.manufacturer_name?.firstOrNull() ?: "Desconocido"}
                            💊 Uso💊: ${med.indications_and_usage?.firstOrNull() ?: "No indicado"}
                            ⚠️ Advertencias ⚠️: ${med.warnings?.firstOrNull() ?: "No especificado"}
                            ❗ Precauciones ❗: ${med.when_using?.firstOrNull() ?: "No especificado"}
                            ❓ Consultar si... ❓: ${med.ask_a_doctor?.firstOrNull() ?: "No indicado"}
                            🚫 Dejar de usar si... 🚫: ${med.stop_use?.firstOrNull() ?: "No indicado"}
                            """.trimIndent()
                        }

                        val traducidos = listaTextos.map { texto ->
                            withContext(Dispatchers.IO) {
                                traductorApi.traducirTexto(
                                    texto = texto,
                                    idiomaDestino = "es",
                                    formato = "text",
                                    apiKey = "TU_API_KEY_GOOGLE_TRANSLATE"
                                ).body()?.data?.translations?.firstOrNull()?.translatedText ?: texto
                            }
                        }

                        withContext(Dispatchers.Main) {
                            progressBarBusqueda.visibility = View.GONE
                            configurarAdapter(traducidos)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            progressBarBusqueda.visibility = View.GONE
                            mostrarError("No se encontraron resultados para '$nombre'")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBarBusqueda.visibility = View.GONE
                        mostrarError("Error: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarBusqueda.visibility = View.GONE
                    mostrarError("Error: ${e.message}")
                }
            }
        }
    }

    private fun configurarAdapter(lista: List<String>) {
        rvResultados.adapter = MedicamentoAdapterPreescripcion(
            lista,
            onItemClick = { texto ->
                val nombre = extraerNombreDeMarca(texto)
                val intent = Intent(this, ActivityMostrarResenas::class.java)
                intent.putExtra("nombreMedicamento", nombre)
                startActivity(intent)
            },
            onAgregarClick = { /* no se usa aquí */ }
        )

        rvResultados.visibility = View.VISIBLE
        btnCerrarResultados.visibility = View.VISIBLE
    }

    private fun mostrarError(mensaje: String) {
        rvResultados.adapter = MedicamentoAdapterPreescripcion(
            listOf(mensaje),
            onItemClick = {},
            onAgregarClick = {}
        )
        rvResultados.visibility = View.VISIBLE
        btnCerrarResultados.visibility = View.VISIBLE
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun extraerNombreDeMarca(texto: String): String {
        val regex = Regex("Marca: (.+)")
        val match = regex.find(texto)
        return match?.groupValues?.get(1)?.split("\n")?.firstOrNull() ?: "Desconocido"
    }

    private fun cargarMejoresResenas() {
        db.collection("resenas")
            .orderBy("estrellas", Query.Direction.DESCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { documents ->
                val lista = documents.mapNotNull { it.toObject(Resena::class.java) }
                rvMejores.adapter = ResenaResumenAdapter(lista) { resena ->
                    val intent = Intent(this, ListaResenasActivity::class.java)
                    intent.putExtra("tipo", "mejores")
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar mejores reseñas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarPeoresResenas() {
        db.collection("resenas")
            .orderBy("estrellas", Query.Direction.ASCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { documents ->
                val lista = documents.mapNotNull { it.toObject(Resena::class.java) }
                rvPeores.adapter = ResenaResumenAdapter(lista) { resena ->
                    val intent = Intent(this, ListaResenasActivity::class.java)
                    intent.putExtra("tipo", "peores")
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar peores reseñas", Toast.LENGTH_SHORT).show()
            }
    }
}
