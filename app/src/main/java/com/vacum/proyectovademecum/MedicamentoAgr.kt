package com.vacum.proyectovademecum

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
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


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicamento_agr)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        val imageViewAgregar = findViewById<ImageView>(R.id.imageView8)

        imageViewAgregar.setOnClickListener {
            val intent = Intent(this, AgregarMedica::class.java)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ Lista vacía al inicio
        val medicamentosGuardados = mutableListOf<String>()

        // ✅ Configurar adapter con la lista vacía
        val adapter = MedicamentoGuardadoAdapter(medicamentosGuardados) { eliminado ->
            eliminarMedicamentoDeFirestore(eliminado)


        }


        recyclerView.adapter = adapter

        // ✅ Cargar desde Firestore y llenar la lista
        cargarMedicamentosGuardados(medicamentosGuardados, adapter)
    }




    private fun cargarMedicamentosGuardados(
        medicamentos: MutableList<String>,
        adapter: MedicamentoGuardadoAdapter
    ) {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("medicamentos_guardados")
                .whereEqualTo("usuarioId", user.uid)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("MedicamentosGuardados", "Documentos obtenidos: ${documents.size()}")

                    medicamentos.clear()
                    medicamentos.addAll(documents.map { doc ->
                        Log.d("MedicamentosGuardados", "Documento info: ${doc.getString("info")}")
                        doc.getString("info") ?: ""
                    })

                    adapter.notifyDataSetChanged()
                }
        }
    }


    private fun eliminarMedicamentoDeFirestore(medicamento: String) {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("medicamentos_guardados")
                .whereEqualTo("usuarioId", user.uid)
                .whereEqualTo("info", medicamento)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        firestore.collection("medicamentos_guardados").document(document.id)
                            .delete()
                            .addOnSuccessListener {
                            }
                    }
                }
        }
    }


}
