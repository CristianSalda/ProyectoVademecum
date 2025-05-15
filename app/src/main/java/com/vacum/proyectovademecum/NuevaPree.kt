package com.vacum.proyectovademecum

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.UUID

class NuevaPree : AppCompatActivity() {

    companion object {
        private const val REQUEST_NUEVA_ORDEN = 100
        internal const val GOOGLE_TRANSLATE_BASE_URL = "https://translation.googleapis.com/"
        internal const val OPEN_FDA_BASE_URL = "https://api.fda.gov/"
        internal const val GOOGLE_TRANSLATE_API_KEY = "AIzaSyBflXNFxBkYxK10GmF6m1j-gvKi0mmuCEk"
        internal const val TAG = "NuevaPree"
    }

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

    data class TranslationResponse(val data: TranslationData?)
    data class TranslationData(val translations: List<Translation>?)
    data class Translation(val translatedText: String?)

    data class OpenFdaResponse(val results: List<OpenFdaDrugLabel>?)
    data class OpenFdaDrugLabel(val openfda: OpenFdaInfo?, val purpose: List<String>?)
    data class OpenFdaInfo(val brand_name: List<String>?, val generic_name: List<String>?)

    data class MedicamentoPrescrito(
        var id: String = UUID.randomUUID().toString(),
        val nombre: String,
        var tiempoEntreDosis: Int = 0,
        var cantidadDosis: Int = 0,
        val detalles: String = "",
        var proximaToma: Long = 0
    )

    private lateinit var adapter: AdaptadorPrescripciones
    private val listaElementos = mutableListOf<Pair<String, MedicamentoPrescrito>>()
    private lateinit var recyclerPrescripciones: RecyclerView
    private lateinit var botonAgregarCard: CardView
    private lateinit var firestoreListener: ListenerRegistration
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            adapter.actualizarTiempoRestante()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_pree)

        val retrofitGoogle = Retrofit.Builder()
            .baseUrl(GOOGLE_TRANSLATE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

        val retrofitFda = Retrofit.Builder()
            .baseUrl(OPEN_FDA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val openFdaApi = retrofitFda.create(OpenFdaApi::class.java)

        recyclerPrescripciones = findViewById(R.id.recyclerPrescripciones)
        botonAgregarCard = findViewById(R.id.botonAgregarCard)

        setupRecyclerView()
        setupBotones()
    }

    override fun onStart() {
        super.onStart()
        cargarPrescripcionesEnTiempoReal()
        handler.post(updateTimeRunnable)
    }

    override fun onStop() {
        super.onStop()
        firestoreListener.remove()
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun setupRecyclerView() {
        adapter = AdaptadorPrescripciones(
            listaElementos,
            onItemClick = { (nombreOrden, medicamento) ->
                mostrarDetallesOrden(nombreOrden, medicamento)
            },
            onDeleteClick = { (nombreOrden, medicamento) ->
                eliminarPrescripcion(nombreOrden, medicamento)
            }
        )

        recyclerPrescripciones.apply {
            layoutManager = LinearLayoutManager(this@NuevaPree)
            adapter = this@NuevaPree.adapter
        }
    }

    private fun setupBotones() {
        findViewById<View>(R.id.imageView8).setOnClickListener {
            finish()
        }

        botonAgregarCard.setOnClickListener {
            val intent = Intent(this, Preescripción::class.java)
            startActivityForResult(intent, REQUEST_NUEVA_ORDEN)
        }
    }

    private fun cargarPrescripcionesEnTiempoReal() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()
        val prescripcionesRef = db
            .collection("usuarios")
            .document(userId)
            .collection("prescripciones_virtuales")

        firestoreListener = prescripcionesRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                listaElementos.clear()
                for (doc in snapshot) {
                    val nombreOrden = doc.getString("nombreOrden") ?: "Sin nombre"
                    val medicamentosData = doc.get("medicamentos") as? List<Map<String, Any>>
                    medicamentosData?.firstOrNull()?.let { medData ->
                        val prescripcion = MedicamentoPrescrito(
                            id = doc.id + "_" + (medData["nombre"] as? String ?: UUID.randomUUID().toString()),
                            nombre = medData["nombre"] as? String ?: "",
                            tiempoEntreDosis = (medData["tiempoEntreDosis"] as? Long)?.toInt() ?: 0,
                            cantidadDosis = (medData["cantidadDosis"] as? Long)?.toInt() ?: 0,
                            detalles = medData["detalles"] as? String ?: "",
                            proximaToma = (medData["proximaToma"] as? Long) ?: 0
                        )
                        listaElementos.add(Pair(nombreOrden, prescripcion))
                        programarAlarma(prescripcion)
                    }
                }
                adapter.notifyDataSetChanged()
                actualizarVistaLista()
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    private fun actualizarVistaLista() {
        val emptyMessageTextView = findViewById<TextView>(R.id.emptyMessage)
        emptyMessageTextView.visibility = if (listaElementos.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NUEVA_ORDEN && resultCode == RESULT_OK) {
            val nombreOrden = data?.getStringExtra("nombreOrden")
            val medicamentoNombre = data?.getStringExtra("nombreMedicamento")
            val tiempoDosis = data?.getIntExtra("tiempoEntreDosis", 0) ?: 0
            val cantidadDosis = data?.getIntExtra("cantidadDosis", 0) ?: 0

            if (!nombreOrden.isNullOrEmpty() && !medicamentoNombre.isNullOrEmpty()) {
                val nuevaPrescripcion = MedicamentoPrescrito(
                    nombre = medicamentoNombre,
                    tiempoEntreDosis = tiempoDosis,
                    cantidadDosis = cantidadDosis,
                    proximaToma = System.currentTimeMillis() + tiempoDosis * 60 * 60 * 1000L
                )
                agregarNuevaOrden(nombreOrden, nuevaPrescripcion)
            }
        }
    }

    private fun agregarNuevaOrden(nombreOrden: String, medicamento: MedicamentoPrescrito) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ordenData = hashMapOf(
            "nombreOrden" to nombreOrden,
            "medicamentos" to listOf(
                hashMapOf(
                    "nombre" to medicamento.nombre,
                    "tiempoEntreDosis" to medicamento.tiempoEntreDosis,
                    "cantidadDosis" to medicamento.cantidadDosis,
                    "proximaToma" to medicamento.proximaToma
                )
            ),
            "fechaCreacion" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "usuarioId" to userId
        )

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .collection("prescripciones_virtuales")
            .add(ordenData)
            .addOnSuccessListener { documentReference ->
                medicamento.id = documentReference.id + "_" + medicamento.nombre
                programarAlarma(medicamento)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar la orden en Firestore", e)
            }
    }

    private fun mostrarDetallesOrden(nombreOrden: String, medicamento: MedicamentoPrescrito) {
        AlertDialog.Builder(this)
            .setTitle(nombreOrden)
            .setMessage(
                "Medicamento: ${medicamento.nombre}\n" +
                        "Cantidad por dosis: ${medicamento.cantidadDosis}\n" +
                        "Tiempo entre dosis: ${medicamento.tiempoEntreDosis} horas\n" +
                        "Próxima toma: ${formatTime(medicamento.proximaToma)}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "No programada"
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun eliminarPrescripcion(nombreOrden: String, medicamento: MedicamentoPrescrito) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .collection("prescripciones_virtuales")
            .whereEqualTo("nombreOrden", nombreOrden)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    document.reference.delete()
                        .addOnSuccessListener {
                            cancelarAlarma(medicamento)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al eliminar la orden de Firestore", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al buscar la orden en Firestore", e)
            }
    }

    private fun programarAlarma(prescripcion: MedicamentoPrescrito) {
        val intent = Intent(this, AlarmaReceiver::class.java).apply {
            putExtra("medicamento_nombre", prescripcion.nombre)
            putExtra("cantidad_dosis", prescripcion.cantidadDosis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            prescripcion.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervaloMillis = prescripcion.tiempoEntreDosis * 60 * 60 * 1000L

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

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarma cancelada para ${prescripcion.nombre}")
    }

}