package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class Mainbusqueda : AppCompatActivity() {


    val retrofitGoogle = Retrofit.Builder()
        .baseUrl("https://translation.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

    private lateinit var api: OpenFdaApi
    private lateinit var searchInput: EditText
    private lateinit var searchButton: LinearLayout
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainbusqueda)

        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchBar)
        resultText = findViewById(R.id.textViewResults)

        val perfil = findViewById<ImageView>(R.id.accountIcon)

        perfil.setOnClickListener {
            val intent = Intent(this, ActivityPerfil::class.java)
            startActivity(intent)
        }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(OpenFdaApi::class.java)

        val searchIcon = findViewById<ImageView>(R.id.btnSearch)
        searchIcon.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamento(query)
            }
        }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    buscarMedicamento(query)
                }
                true
            } else {
                false
            }
        }


        // Configurar el botón de regreso
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val guardarBtn = findViewById<FloatingActionButton>(R.id.btnGuardarFavorito)

        guardarBtn.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nombre = searchInput.text.toString().trim()
            val descripcion = resultText.text.toString().trim()

            if (nombre.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medicamento = MedicamentoFavorito(
                nombre = nombre,
                descripcion = descripcion,
                usuarioId = userId
            )

            FirebaseFirestore.getInstance().collection("medicamentos_favs")
                .add(medicamento)
                .addOnSuccessListener {
                    Toast.makeText(this, "Medicamento guardado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }

    }
    private fun buscarMedicamento(nombre: String) {
        lifecycleScope.launch {
            try {
                val traduccion = traductorApi.traducirTexto(
                    texto = nombre,
                    idiomaDestino = "en",
                    formato = "text",
                    apiKey = "Nuestra API jijiji"
                )

                val nombreEnIngles = traduccion.body()
                    ?.data
                    ?.translations
                    ?.firstOrNull()
                    ?.translatedText
                    ?: nombre

                val response = api.getMedicamentos("openfda.substance_name:$nombreEnIngles", 10)
                val medicamento = response.body()?.results?.firstOrNull()

                if (response.isSuccessful && medicamento != null) {
                    val marca = medicamento.openfda?.brand_name?.joinToString() ?: "Desconocida"
                    val fabricante = medicamento.openfda?.manufacturer_name?.joinToString() ?: "Desconocido"
                    val proposito = medicamento.purpose?.joinToString() ?: "No especificado"
                    val uso = medicamento.indications_and_usage?.joinToString() ?: "No indicado"

                    val textoOriginal = """
                    Brand: $marca
                    Manufacturer: $fabricante
                    Purpose: $proposito
                    Usage: $uso
                """.trimIndent()

                    val traduccion = traductorApi.traducirTexto(
                        texto = textoOriginal,
                        idiomaDestino = "es",
                        formato = "text",
                        apiKey = "Nuestra API jijiji"
                    )

                    if (traduccion.isSuccessful) {
                        val textoTraducido = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText
                        resultText.text = textoTraducido ?: textoOriginal
                    } else {
                        resultText.text = textoOriginal
                    }

                } else {
                    resultText.text = "No se encontró información para '$nombre'."
                }
            } catch (e: Exception) {
                resultText.text = "Error: ${e.message}"
                Log.e("API_ERROR", e.toString())
            }
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val historial = HistorialBusqueda(nombre = nombre, usuarioId = userId)
                FirebaseFirestore.getInstance().collection("historial_busquedas")
                    .add(historial)
                    .addOnSuccessListener {
                        Log.d("Historial", "Búsqueda guardada")
                    }
                    .addOnFailureListener {
                        Log.e("Historial", "Error al guardar historial", it)
                    }
            }

        }
    }
}
