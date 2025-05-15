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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.vacum.proyectovademecum.adapters.ResenaResumenAdapter

class FormularioEditarResenaActivity : AppCompatActivity() {

    private lateinit var tvMedicamentoEscogido: TextView
    private lateinit var medicamentoSeleccionadoLayout: LinearLayout
    private lateinit var etDescripcion: EditText
    private lateinit var ratingBar: RatingBar
    private lateinit var btnGuardar: Button
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultadosLayout: LinearLayout
    private lateinit var btnCerrarResultados: Button
    private lateinit var btnEliminarMedicamento: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCancelar:Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var resena: Resena
    private var medicamentoSeleccionado: String? = null

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
        setContentView(R.layout.activity_formulario_editar_resena)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Vistas
        tvMedicamentoEscogido = findViewById(R.id.tvMedicamentoSeleccionado)
        medicamentoSeleccionadoLayout = findViewById(R.id.medicamentoSeleccionadoLayout)
        etDescripcion = findViewById(R.id.etDescripcionEdit)
        ratingBar = findViewById(R.id.ratingBar)
        btnGuardar = findViewById(R.id.btnGuardar)
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        resultadosLayout = findViewById(R.id.resultadosLayout)
        btnCerrarResultados = findViewById(R.id.btnCerrarResultados)
        btnEliminarMedicamento = findViewById(R.id.btnEliminarMedicamento)
        progressBar = findViewById(R.id.progressBar)
        btnCancelar = findViewById(R.id.btnCancelar)

        resena = intent.getParcelableExtra("resena") ?: run {
            Toast.makeText(this, "Error al cargar reseña", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mostrar datos actuales
        medicamentoSeleccionado = resena.medicamento
        tvMedicamentoEscogido.text = resena.medicamento
        medicamentoSeleccionadoLayout.visibility = View.VISIBLE
        btnEliminarMedicamento.visibility = View.VISIBLE
        etDescripcion.setText(resena.descripcion)
        ratingBar.rating = resena.estrellas.toFloat()

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamento(query)
            }
        }

        btnCerrarResultados.setOnClickListener {
            resultadosLayout.removeAllViews()
            btnCerrarResultados.visibility = View.GONE
        }

        btnEliminarMedicamento.setOnClickListener {
            medicamentoSeleccionado = null
            tvMedicamentoEscogido.text = ""
            medicamentoSeleccionadoLayout.visibility = View.GONE
        }

        btnGuardar.setOnClickListener {
            guardarResena()
        }

        findViewById<ImageView>(R.id.atras).setOnClickListener {
            finish()
        }
        btnCancelar.setOnClickListener {
            val intent = Intent(this, EditarEliminarResenaActivity::class.java)

            // Reenviamos los datos de la reseña actual para que la otra activity los reciba de nuevo
            intent.putExtra("resenaId", resena.id)
            intent.putExtra("medicamento", resena.medicamento)
            intent.putExtra("descripcion", resena.descripcion)
            intent.putExtra("estrellas", resena.estrellas)

            startActivity(intent)
            finish()
        }


    }

    private fun buscarMedicamento(nombre: String) {
        progressBar.visibility = View.VISIBLE
        resultadosLayout.removeAllViews()
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
                            val recycler = RecyclerView(this@FormularioEditarResenaActivity).apply {
                                layoutManager = LinearLayoutManager(this@FormularioEditarResenaActivity)
                                adapter = MedicamentoAdapterPreescripcion(
                                    traducidos,
                                    onItemClick = { texto ->
                                        val nombre = extraerNombreDeMarca(texto)
                                        medicamentoSeleccionado = nombre
                                        tvMedicamentoEscogido.text = nombre
                                        medicamentoSeleccionadoLayout.visibility = View.VISIBLE
                                        btnEliminarMedicamento.visibility = View.VISIBLE
                                        resultadosLayout.removeAllViews()
                                        btnCerrarResultados.visibility = View.GONE
                                    },
                                    onAgregarClick = { texto ->
                                        val nombre = extraerNombreDeMarca(texto)
                                        medicamentoSeleccionado = nombre
                                        tvMedicamentoEscogido.text = nombre
                                        medicamentoSeleccionadoLayout.visibility = View.VISIBLE
                                        btnEliminarMedicamento.visibility = View.VISIBLE
                                        resultadosLayout.removeAllViews()
                                        btnCerrarResultados.visibility = View.GONE}
                                )
                            }
                            resultadosLayout.addView(recycler)
                            btnCerrarResultados.visibility = View.VISIBLE
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            mostrarError("No se encontraron resultados")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        mostrarError("Error: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarError("Error: ${e.message}")
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    private fun extraerNombreDeMarca(texto: String): String {
        val regex = Regex("Marca: (.+)")
        val match = regex.find(texto)
        return match?.groupValues?.get(1)?.split("\n")?.firstOrNull() ?: "Desconocido"
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
    private fun guardarResena() {
        val id = resena.id
        if (id.isNullOrEmpty()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val medicamento = medicamentoSeleccionado?.trim()
        val descripcion = etDescripcion.text.toString().trim()
        val estrellas = ratingBar.rating.toInt()

        if (medicamento.isNullOrEmpty()) {
            Toast.makeText(this, "Seleccione un medicamento", Toast.LENGTH_SHORT).show()
            return
        }

        if (descripcion.isEmpty()) {
            Toast.makeText(this, "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedResena = Resena(
            id = id,
            medicamento = medicamento,
            descripcion = descripcion,
            estrellas = estrellas,
            usuarioId = user.uid
        )

        db.collection("resenas").document(id)
            .set(updatedResena)
            .addOnSuccessListener {
                Toast.makeText(this, "Reseña actualizada", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar reseña", Toast.LENGTH_SHORT).show()
            }
    }
}
