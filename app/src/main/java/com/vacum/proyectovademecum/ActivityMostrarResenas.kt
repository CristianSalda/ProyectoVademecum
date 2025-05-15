package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ActivityMostrarResenas : AppCompatActivity() {

    private lateinit var rvResenas: RecyclerView
    private lateinit var adapter: ResenaAdapter
    private val db = FirebaseFirestore.getInstance()
    private val listaResenas = mutableListOf<ResenaCompleta>()
    private lateinit var btnCrearResena: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mostrar_resenas)

        rvResenas = findViewById(R.id.rvResenas)
        rvResenas.layoutManager = LinearLayoutManager(this)
        adapter = ResenaAdapter(listaResenas)
        rvResenas.adapter = adapter

        val nombreMedicamento = intent.getStringExtra("nombreMedicamento") ?: return



        btnCrearResena = findViewById(R.id.btnAgregarResena)
        btnCrearResena.setOnClickListener {
            val intent = Intent(this, AgregarResenaActivity::class.java)
            intent.putExtra("origen", "mostrarResenas")
            intent.putExtra("nombreMedicamento", nombreMedicamento)
            startActivity(intent)
        }


        obtenerResenas(nombreMedicamento)
    }

    private fun obtenerResenas(medicamento: String) {
        listaResenas.clear()

        val cargarResenas = { query: com.google.firebase.firestore.Query ->
            query.get()
                .addOnSuccessListener { documentos ->
                    for (doc in documentos) {
                        val resena = doc.toObject(Resena::class.java)
                        val usuarioId = resena.usuarioId

                        db.collection("usuarios").document(usuarioId).get()
                            .addOnSuccessListener { usuarioDoc ->
                                val nombreUsuario = usuarioDoc.getString("nombre") ?: "Usuario"
                                val resenaCompleta = ResenaCompleta(
                                    nombreUsuario = nombreUsuario,
                                    votacion = resena.estrellas,
                                    descripcion = resena.descripcion ?: "Sin descripción"
                                )
                                listaResenas.add(resenaCompleta)
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar reseñas", Toast.LENGTH_SHORT).show()
                }
        }

        // Cargar 3 mejores
        val mejores = db.collection("resenas")
            .whereEqualTo("medicamento", medicamento)
            .orderBy("estrellas", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(3)

        // Cargar 3 peores
        val peores = db.collection("resenas")
            .whereEqualTo("medicamento", medicamento)
            .orderBy("estrellas", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limit(3)

        cargarResenas(mejores)
        cargarResenas(peores)
    }

}
