package com.vacum.proyectovademecum

import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AgregarMedica : AppCompatActivity() {

    private val medicamentosSeleccionados = mutableListOf<String>()



    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView

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
        setContentView(R.layout.activity_agregar_medica)
        val btnCarrito = findViewById<ImageView>(R.id.btnCarrito)
        val imageViewAgregar = findViewById<ImageView>(R.id.imageView8)

        imageViewAgregar.setOnClickListener {
            val intent = Intent(this, NuevaPree::class.java)
            startActivity(intent)
        }

        btnCarrito.setOnClickListener {
            if (medicamentosSeleccionados.isNotEmpty()) {
                val intent = Intent(this, MedicamentosGuardadosActivity::class.java)
                intent.putStringArrayListExtra("listaMedicamentos", ArrayList(medicamentosSeleccionados))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No has agregado medicamentos aún", Toast.LENGTH_SHORT).show()
            }
            Log.d("MedicamentosGuardados", "Lista recibida: $medicamentosSeleccionados")

        }


        // Inicializa Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamentos(query)
            }
        }
    }

    private fun mostrarDetalleMedicamento(textoCompleto: String) {
        AlertDialog.Builder(this)
            .setTitle("Detalles del Medicamento")
            .setMessage(textoCompleto)
            .setPositiveButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun extraerCampo(texto: String, etiqueta: String): String {
        val regex = Regex("$etiqueta\\s*(.*)")
        val match = regex.find(texto)
        return match?.groups?.get(1)?.value?.trim() ?: "Desconocido"
    }




    private fun configurarAdapter(medicamentos: List<String>) {
        recyclerView.adapter = MedicamentoAdapter(
            medicamentos,
            onItemClick = { textoCompleto ->
                mostrarDetalleMedicamento(textoCompleto)
            },
            onAgregarClick = { textoCompleto ->
                val marca = extraerCampo(textoCompleto, "Marca:")
                val fabricante = extraerCampo(textoCompleto, "Fabricante:")
                val resumen = "$marca - $fabricante"

                medicamentosSeleccionados.add(resumen)
                guardarEnFirebase(resumen)  // ✅ solo guardas marca y fabricante
                Toast.makeText(this, "Agregado al carrito (${medicamentosSeleccionados.size})", Toast.LENGTH_SHORT).show()
            }

        )
    }


    private fun guardarEnFirebase(medicamentoInfo: String) {
        val user = auth.currentUser
        if (user != null) {
            val medicamento = hashMapOf(
                "info" to medicamentoInfo,
                "fecha" to FieldValue.serverTimestamp(),
                "usuarioId" to user.uid
            )

            firestore.collection("medicamentos_guardados")
                .add(medicamento)
                .addOnSuccessListener {
                    Toast.makeText(this, "Medicamento guardado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            Toast.makeText(this, "Debes iniciar sesión para guardar", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showLoading(show: Boolean) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        if (show) {
            progressBar.visibility = View.VISIBLE
            progressBar.animate().alpha(1f).setDuration(300).start()
        } else {
            progressBar.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { progressBar.visibility = View.GONE }
                .start()
        }

        searchButton.isEnabled = !show
        searchInput.isEnabled = !show
    }

    private fun buscarMedicamentos(nombre: String) {
        // Mostrar progreso (opcional)
        showLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Traducción del término de búsqueda
                val nombreEnIngles = withContext(Dispatchers.IO) {
                    traductorApi.traducirTexto(
                        texto = nombre,
                        idiomaDestino = "en",
                        formato = "text",
                        apiKey = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
                    ).body()?.data?.translations?.firstOrNull()?.translatedText ?: nombre
                }

                // 2. Búsqueda en FDA
                val response = withContext(Dispatchers.IO) {
                    apiFda.getMedicamentos("openfda.substance_name:$nombreEnIngles", 10)
                }

                if (response.isSuccessful) {
                    val resultados = response.body()?.results ?: emptyList()

                    if (resultados.isNotEmpty()) {
                        // 3. Procesar resultados (en bloque para minimizar traducciones)
                        val listaTextos = resultados.map { med ->
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

                        // 4. Traducir TODOS los resultados juntos (más eficiente)
                        val resultadosTraducidos = listaTextos.map { textoIndividual ->
                            withContext(Dispatchers.IO) {
                                traductorApi.traducirTexto(
                                    texto = textoIndividual,
                                    idiomaDestino = "es",
                                    formato = "text",
                                    apiKey = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
                                ).body()?.data?.translations?.firstOrNull()?.translatedText ?: textoIndividual
                            }
                        }

                        // 5. Actualizar UI
                        runOnUiThread {
                            configurarAdapter(resultadosTraducidos)
                        }
                    } else {
                        runOnUiThread {
                            recyclerView.adapter = MedicamentoAdapter(
                                listOf("No se encontraron resultados para '$nombre'"),
                                onItemClick = { mostrarDetalleMedicamento(it) },
                                onAgregarClick = { guardarEnFirebase(it) }
                            )
                        }
                    }
                } else {
                    runOnUiThread {
                        recyclerView.adapter = MedicamentoAdapter(
                            listOf("Error en la API: ${response.message()}"),
                            onItemClick = { mostrarDetalleMedicamento(it) },
                            onAgregarClick = { guardarEnFirebase(it) }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AgregarMedica", "Error: ${e.message}", e)
                runOnUiThread {
                    recyclerView.adapter = MedicamentoAdapter(
                        listOf("Error: ${e.localizedMessage ?: "Error desconocido"}"),
                        onItemClick = { mostrarDetalleMedicamento(it) },
                        onAgregarClick = { guardarEnFirebase(it) }
                    )
                    Toast.makeText(this@AgregarMedica, "Error en la búsqueda", Toast.LENGTH_SHORT)
                        .show()
                }
            } finally {
                runOnUiThread {
                    showLoading(false)
                }
            }
        }
    }
}
