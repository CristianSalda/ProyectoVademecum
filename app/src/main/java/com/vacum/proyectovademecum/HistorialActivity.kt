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
    private val lista = mutableListOf<HistorialBusqueda>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        // Configurar el botón de regreso
        val backButton = findViewById<ImageView>(R.id.atras)
        backButton.setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.recyclerHistorial)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistorialAdapter(lista)
        recyclerView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("historial_busquedas")
            .whereEqualTo("usuarioId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                lista.clear()
                lista.addAll(snapshot.toObjects(HistorialBusqueda::class.java))
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar historial", Toast.LENGTH_SHORT).show()
            }

    }
}
