package com.vacum.proyectovademecum

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.UUID

class NuevaPree : AppCompatActivity() {

    // Constantes
    companion object {
        private const val REQUEST_BUSQUEDA_MEDICAMENTO = 1
        private const val GOOGLE_TRANSLATE_BASE_URL = "https://translation.googleapis.com/"
        private const val OPEN_FDA_BASE_URL = "https://api.fda.gov/"
        private const val GOOGLE_TRANSLATE_API_KEY = "YOUR_GOOGLE_TRANSLATE_API_KEY" // Reemplaza con tu API Key
        private const val OPEN_FDA_API_KEY = "YOUR_OPEN_FDA_API_KEY" // Opcional, algunas llamadas no requieren API Key
        private const val TAG = "NuevaPree"
    }

    // Interfaces para las APIs
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
            @Query("search") query: String,
            @Query("limit") limit: Int = 10
        ): retrofit2.Response<OpenFdaResponse>
    }

    // Modelos de datos
    data class TranslationResponse(val data: TranslationData?)
    data class TranslationData(val translations: List<Translation>?)
    data class Translation(val translatedText: String?)

    data class MedicamentoPrescrito(
        var id: String = UUID.randomUUID().toString(),
        val nombre: String,
        var tiempoEntreDosis: Int = 0,
        var cantidadDosis: Int = 0,
        val detalles: String = "",
        var proximaToma: Long = 0
    )

    // Variables de clase
    private lateinit var adapter: AdaptadorPrescripciones
    private val prescripciones = mutableListOf<MedicamentoPrescrito>()
    private lateinit var traductorApi: GoogleTranslateApi
    private lateinit var openFdaApi: OpenFdaApi
    private lateinit var emptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_pree)

        // Configurar APIs
        val retrofitGoogle = Retrofit.Builder()
            .baseUrl(GOOGLE_TRANSLATE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

        val retrofitFda = Retrofit.Builder()
            .baseUrl(OPEN_FDA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        openFdaApi = retrofitFda.create(OpenFdaApi::class.java)

        // Configurar vistas
        emptyMessage = findViewById(R.id.emptyMessage)
        setupRecyclerView()
        setupBotones()
        cargarPrescripciones()
    }

    private fun setupRecyclerView() {
        adapter = AdaptadorPrescripciones(
            prescripciones,
            onAgregarClick = { iniciarBusquedaMedicamento() },
            onItemClick = { prescripcion -> mostrarDialogoConfiguracionDosis(prescripcion) },
            onDeleteClick = { prescripcion -> eliminarPrescripcion(prescripcion) }
        )

        findViewById<RecyclerView>(R.id.recyclerPrescripciones).apply {
            layoutManager = LinearLayoutManager(this@NuevaPree)
            adapter = this@NuevaPree.adapter
        }
    }


    private fun setupBotones() {
        findViewById<View>(R.id.imageView8).setOnClickListener {
            finish()
        }
    }

    private fun cargarPrescripciones() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .collection("prescripciones_virtuales")
            .get()
            .addOnSuccessListener { result ->
                prescripciones.clear()
                for (document in result) {
                    val medicamentos = document.get("medicamentos") as? List<Map<String, Any>> ?: continue
                    for (medData in medicamentos) {
                        val prescripcion = MedicamentoPrescrito(
                            id = document.id,
                            nombre = medData["nombre"] as? String ?: "",
                            tiempoEntreDosis = (medData["tiempoEntreDosis"] as? Long)?.toInt() ?: 0,
                            cantidadDosis = (medData["cantidadDosis"] as? Long)?.toInt() ?: 0,
                            detalles = medData["detalles"] as? String ?: "",
                            proximaToma = (medData["proximaToma"] as? Long) ?: 0
                        )
                        prescripciones.add(prescripcion)
                    }
                }
                actualizarVistaLista()
                prescripciones.forEach { programarAlarma(it) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar las prescripciones desde Firestore", e)
            }
    }

    private fun actualizarVistaLista() {
        if (prescripciones.isEmpty()) {
            emptyMessage.visibility = View.VISIBLE
        } else {
            emptyMessage.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
    }

    private fun iniciarBusquedaMedicamento() {
        val intent = Intent(this, Preescripción::class.java)
        startActivityForResult(intent, REQUEST_BUSQUEDA_MEDICAMENTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BUSQUEDA_MEDICAMENTO && resultCode == RESULT_OK) {
            val nombreMedicamento = data?.getStringExtra("nombreMedicamento")
            if (!nombreMedicamento.isNullOrEmpty()) {
                val nuevaPrescripcion = MedicamentoPrescrito(nombre = nombreMedicamento)
                prescripciones.add(nuevaPrescripcion)
                guardarPrescripcionEnFirestore()
                actualizarVistaLista()
            }
        }
    }

    private fun mostrarDialogoConfiguracionDosis(prescripcion: MedicamentoPrescrito) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_config_dosis, null)

        dialogView.findViewById<EditText>(R.id.etTiempo).setText(prescripcion.tiempoEntreDosis.toString())
        dialogView.findViewById<EditText>(R.id.etCantidad).setText(prescripcion.cantidadDosis.toString())

        AlertDialog.Builder(this)
            .setTitle("Configurar ${prescripcion.nombre}")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val tiempo = dialogView.findViewById<EditText>(R.id.etTiempo).text.toString().toIntOrNull() ?: 0
                val cantidad = dialogView.findViewById<EditText>(R.id.etCantidad).text.toString().toIntOrNull() ?: 0

                prescripcion.tiempoEntreDosis = tiempo
                prescripcion.cantidadDosis = cantidad
                prescripcion.proximaToma = System.currentTimeMillis() + tiempo * 60 * 60 * 1000L

                guardarPrescripcionEnFirestore()
                programarAlarma(prescripcion)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPrescripcion(prescripcion: MedicamentoPrescrito) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .collection("prescripciones_virtuales")
            .document(prescripcion.id)
            .delete()
            .addOnSuccessListener {
                prescripciones.remove(prescripcion)
                actualizarVistaLista()
                cancelarAlarma(prescripcion)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar la prescripción de Firestore", e)
            }
    }

    private fun guardarPrescripcionEnFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val prescripcionData = hashMapOf(
            "medicamentos" to prescripciones.map {
                hashMapOf(
                    "nombre" to it.nombre,
                    "tiempoEntreDosis" to it.tiempoEntreDosis,
                    "cantidadDosis" to it.cantidadDosis,
                    "detalles" to it.detalles,
                    "proximaToma" to it.proximaToma
                )
            },
            "usuarioId" to userId,
            "fechaActualizacion" to FieldValue.serverTimestamp()
        )

        // Si ya existe una prescripción (aunque tengamos varios medicamentos dentro),
        // podríamos querer actualizar el documento existente en lugar de crear uno nuevo
        if (prescripciones.isNotEmpty() && prescripciones.firstOrNull()?.id?.isNotEmpty() == true) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .collection("prescripciones_virtuales")
                .document(prescripciones.first().id) // Asumimos que todos los medicamentos pertenecen a la misma "prescripción" lógica
                .set(prescripcionData)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar la prescripción en Firestore", e)
                }
        } else {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .collection("prescripciones_virtuales")
                .add(prescripcionData)
                .addOnSuccessListener { documentReference ->
                    prescripciones.forEach { it.id = documentReference.id }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al agregar la prescripción a Firestore", e)
                }
        }
    }

    private fun programarAlarma(prescripcion: MedicamentoPrescrito) {
        val intent = Intent(this, AlarmaReceiver::class.java).apply {
            putExtra("medicamento_nombre", prescripcion.nombre)
            putExtra("cantidad_dosis", prescripcion.cantidadDosis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            prescripcion.hashCode(), // Usar un ID único para cada alarma
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervaloMillis = prescripcion.tiempoEntreDosis * 60 * 60 * 1000L

        // Programar la alarma para que se active a la hora de la próxima toma y luego se repita
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            prescripcion.proximaToma,
            intervaloMillis,
            pendingIntent
        )
        Log.d(TAG, "Alarma programada para ${prescripcion.nombre} a las ${java.util.Date(prescripcion.proximaToma)}")
    }

    private fun cancelarAlarma(prescripcion: MedicamentoPrescrito) {
        val intent = Intent(this, AlarmaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            prescripcion.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarma cancelada para ${prescripcion.nombre}")
    }
}