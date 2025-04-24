package com.vacum.proyectovademecum

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class MedicamentosGuardadosActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicamento_agr)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.recyclerViewGuardados)
        recyclerView.layoutManager = LinearLayoutManager(this)

        cargarMedicamentosGuardados()
    }

    private fun cargarMedicamentosGuardados() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("medicamentos_guardados")
                .whereEqualTo("usuarioId", user.uid)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val medicamentos = documents.map { doc ->
                        doc.getString("info") ?: ""
                    }
                    recyclerView.adapter = MedicamentoAdapter(medicamentos,
                        onItemClick = { mostrarDetalle(it) },
                        onAgregarClick = {} // Vacío porque ya está guardado
                    )
                }
        }
    }

    private fun mostrarDetalle(texto: String) {
        AlertDialog.Builder(this)
            .setTitle("Medicamento guardado")
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
