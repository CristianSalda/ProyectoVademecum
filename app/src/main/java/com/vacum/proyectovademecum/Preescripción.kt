package com.vacum.proyectovademecum

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.http.GET
import retrofit2.http.Query

class Preescripción : AppCompatActivity() {

    // Interfaces para las APIs
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
            @Query("limit") limit: Int = 10
        ): retrofit2.Response<OpenFdaResponse>
    }

    data class TranslationResponse(val data: TranslationData?)
    data class TranslationData(val translations: List<Translation>?)
    data class Translation(val translatedText: String?)

    data class OpenFdaResponse(val results: List<Medicamento>?)

    data class MedicamentoPrescrito(
        val id: String,
        val nombre: String,
        var tiempoEntreDosis: Int, // en horas
        var cantidadDosis: Int, // cantidad por toma
        val detalles: String = ""
    )

    // Variables de clase
    private lateinit var adapter: MedicamentoSeleccionadoAdapter
    private lateinit var searchAdapter: BusquedaMedicamentoAdapter
    private val medicamentosSeleccionados = mutableListOf<MedicamentoPrescrito>()
    private lateinit var traductorApi: GoogleTranslateApi
    private lateinit var openFdaApi: OpenFdaApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescripcion_virtual)

        // Configurar Retrofit para las APIs
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

        setupRecyclerViews()
        setupBusqueda()
        setupBotones()
    }

    private fun setupRecyclerViews() {
        // Adaptador para medicamentos seleccionados (arriba)
        adapter = MedicamentoSeleccionadoAdapter(medicamentosSeleccionados) { medicamento ->
            mostrarDialogoConfiguracionDosis(medicamento)
        }
        findViewById<RecyclerView>(R.id.rvMedicamentosSeleccionados).apply {
            layoutManager = LinearLayoutManager(this@PrescripcionVirtualActivity)
            adapter = this@PrescripcionVirtualActivity.adapter
        }

        // Adaptador para resultados de búsqueda (abajo)
        searchAdapter = BusquedaMedicamentoAdapter(emptyList()) { medicamento ->
            agregarMedicamentoSeleccionado(medicamento)
            findViewById<EditText>(R.id.etBusqueda).text.clear()
        }
        findViewById<RecyclerView>(R.id.rvResultadosBusqueda).apply {
            layoutManager = LinearLayoutManager(this@PrescripcionVirtualActivity)
            adapter = searchAdapter
        }
    }

    private fun setupBusqueda() {
        findViewById<EditText>(R.id.etBusqueda).apply {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length > 2) {
                        buscarMedicamentos(s.toString())
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
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarPrescripcionEnFirestore()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun buscarMedicamentos(query: String) {
        lifecycleScope.launch {
            try {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE

                // Traducir a inglés para la búsqueda en OpenFDA
                val traduccion = traductorApi.traducirTexto(
                    texto = query,
                    idiomaDestino = "en",
                    apiKey = "TU_API_KEY_GOOGLE"
                )

                val queryEnIngles = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText ?: query

                // Buscar en OpenFDA
                val response = openFdaApi.getMedicamentos("openfda.brand_name:$queryEnIngles")

                if (response.isSuccessful && response.body()?.results != null) {
                    val medicamentos = response.body()!!.results!!.mapNotNull { medicamento ->
                        medicamento.openfda?.brand_name?.firstOrNull()?.let { nombre ->
                            // Traducir detalles al español
                            val detalles = buildString {
                                medicamento.purpose?.firstOrNull()?.let {
                                    append("Propósito: $it\n")
                                }
                                medicamento.indications_and_usage?.firstOrNull()?.let {
                                    append("Indicaciones: $it\n")
                                }
                                medicamento.warnings?.firstOrNull()?.let {
                                    append("Advertencias: $it\n")
                                }
                            }

                            medicamento.copy(
                                openfda = medicamento.openfda.copy(
                                    brand_name = listOf(nombre)
                                )
                            )
                        }
                    }

                    searchAdapter.updateList(medicamentos)

                } else {
                    Toast.makeText(this@PrescripcionVirtualActivity,
                        "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error en buscarMedicamentos", e)
                Toast.makeText(this@PrescripcionVirtualActivity,
                    "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            }
        }
    }

    private fun agregarMedicamentoSeleccionado(medicamento: Medicamento) {
        val nombre = medicamento.openfda?.brand_name?.firstOrNull() ?: "Medicamento desconocido"

        // Traducir detalles al español
        lifecycleScope.launch {
            try {
                val detallesOriginal = buildString {
                    medicamento.purpose?.firstOrNull()?.let {
                        append("Purpose: $it\n")
                    }
                    medicamento.indications_and_usage?.firstOrNull()?.let {
                        append("Indications: $it\n")
                    }
                }

                val traduccion = traductorApi.traducirTexto(
                    texto = detallesOriginal,
                    idiomaDestino = "es",
                    apiKey = "TU_API_KEY_GOOGLE"
                )

                val detallesEsp = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText
                    ?: detallesOriginal

                val medicamentoPrescrito = MedicamentoPrescrito(
                    id = nombre.hashCode().toString(),
                    nombre = nombre,
                    tiempoEntreDosis = 0,
                    cantidadDosis = 0,
                    detalles = detallesEsp
                )

                medicamentosSeleccionados.add(medicamentoPrescrito)
                adapter.notifyItemInserted(medicamentosSeleccionados.size - 1)
            } catch (e: Exception) {
                Log.e("TRADUCCION", "Error al traducir detalles", e)
            }
        }
    }

    private fun mostrarDialogoConfiguracionDosis(medicamento: MedicamentoPrescrito) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialogo_config_dosis, null)

        dialogView.findViewById<EditText>(R.id.etTiempo).setText(medicamento.tiempoEntreDosis.toString())
        dialogView.findViewById<EditText>(R.id.etCantidad).setText(medicamento.cantidadDosis.toString())

        AlertDialog.Builder(this)
            .setTitle("Configurar dosis para ${medicamento.nombre}")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val tiempo = dialogView.findViewById<EditText>(R.id.etTiempo).text.toString().toIntOrNull() ?: 0
                val cantidad = dialogView.findViewById<EditText>(R.id.etCantidad).text.toString().toIntOrNull() ?: 0

                medicamento.tiempoEntreDosis = tiempo
                medicamento.cantidadDosis = cantidad
                adapter.notifyDataSetChanged()

                programarAlarma(medicamento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarPrescripcionEnFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        if (medicamentosSeleccionados.isEmpty()) {
            Toast.makeText(this, "No hay medicamentos para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        val prescripcionData = hashMapOf(
            "medicamentos" to medicamentosSeleccionados.map {
                hashMapOf(
                    "nombre" to it.nombre,
                    "tiempoEntreDosis" to it.tiempoEntreDosis,
                    "cantidadDosis" to it.cantidadDosis,
                    "detalles" to it.detalles
                )
            },
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

    private fun programarAlarma(medicamento: MedicamentoPrescrito) {
        val intent = Intent(this, AlarmaReceiver::class.java).apply {
            putExtra("medicamento_nombre", medicamento.nombre)
            putExtra("cantidad_dosis", medicamento.cantidadDosis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicamento.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervaloMillis = medicamento.tiempoEntreDosis * 60 * 60 * 1000L

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervaloMillis,
            intervaloMillis,
            pendingIntent
        )
    }
}