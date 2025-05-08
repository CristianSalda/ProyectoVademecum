package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Activity_guardados : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicamentoAdapter
    private val listaMedicamentos = mutableListOf<MedicamentoFavorito>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardados)

        // Botón atrás
        val btnatras = findViewById<ImageView>(R.id.atras)
        btnatras.setOnClickListener { finish() }

        val perfil = findViewById<ImageView>(R.id.usuario)

        perfil.setOnClickListener {
            val intent = Intent(this, ActivityPerfil::class.java)
            startActivity(intent)
        }

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerFavoritos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MedicamentoAdapter(listaMedicamentos) { medicamento ->
            eliminarMedicamento(medicamento)
        }
        recyclerView.adapter = adapter

        cargarMedicamentosFavoritos()
    }

    private fun cargarMedicamentosFavoritos() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("medicamentos_favs")
            .whereEqualTo("usuarioId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                listaMedicamentos.clear()
                listaMedicamentos.addAll(snapshot.toObjects(MedicamentoFavorito::class.java))
                adapter.notifyDataSetChanged()
            }
    }

    private fun eliminarMedicamento(med: MedicamentoFavorito) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("medicamentos_favs")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("nombre", med.nombre)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    db.collection("medicamentos_favs").document(doc.id).delete()
                }
                Toast.makeText(this, "Medicamento eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }
}
