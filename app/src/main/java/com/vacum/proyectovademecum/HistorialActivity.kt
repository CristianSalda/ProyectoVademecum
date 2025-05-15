package com.vacum.proyectovademecum

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistorialAdapter
    private val listaHistorial = mutableListOf<HistorialBusqueda>()
    private val db = FirebaseFirestore.getInstance()
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        // Botón de regreso
        findViewById<ImageView>(R.id.atras).setOnClickListener { finish() }

        // Validar usuario autenticado
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerHistorial)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HistorialAdapter(listaHistorial) { item, position ->
            eliminarHistorial(item.id.orEmpty(), position)
        }

        recyclerView.adapter = adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        db.collection("usuarios")
            .document(userId!!)
            .collection("historial")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                listaHistorial.clear()
                for (document in snapshot.documents) {
                    val historial = document.toObject(HistorialBusqueda::class.java)
                    historial?.id = document.id
                    historial?.let { listaHistorial.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar historial", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarHistorial(id: String, position: Int) {
        db.collection("usuarios")
            .document(userId!!)
            .collection("historial")
            .document(id)
            .delete()
            .addOnSuccessListener {
                listaHistorial.removeAt(position)
                adapter.notifyItemRemoved(position)
                Toast.makeText(this, "Historial eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar historial", Toast.LENGTH_SHORT).show()
            }
    }
}
