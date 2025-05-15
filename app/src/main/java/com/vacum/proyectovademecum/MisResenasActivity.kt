package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MisResenasActivity : AppCompatActivity() {

    private lateinit var rvMisResenas: RecyclerView
    private lateinit var tvEstadoResenas: TextView
    private lateinit var btnCrearResena: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val listaResenas = mutableListOf<Resena>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_resenas)

        rvMisResenas = findViewById(R.id.rvMisResenas)
        tvEstadoResenas = findViewById(R.id.tvEstadoResenas)
        btnCrearResena = findViewById(R.id.btnCrearResena)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        rvMisResenas.layoutManager = LinearLayoutManager(this)

        btnCrearResena.setOnClickListener {
            val intent = Intent(this, AgregarResenaActivity::class.java)
            intent.putExtra("origen", "misResenas")
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.btnAtras).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarResenasDelUsuario()
    }

    private fun cargarResenasDelUsuario() {
        val user = auth.currentUser ?: return

        db.collection("resenas")
            .whereEqualTo("usuarioId", user.uid)
            .get()
            .addOnSuccessListener { documentos ->
                listaResenas.clear()

                if (documentos.isEmpty) {
                    tvEstadoResenas.text = "Actualmente no tiene ninguna reseña"
                    tvEstadoResenas.visibility = View.VISIBLE
                    rvMisResenas.visibility = View.GONE
                } else {
                    for (doc in documentos) {
                        val resena = doc.toObject(Resena::class.java)
                        resena.id = doc.id
                        listaResenas.add(resena)
                    }

                    tvEstadoResenas.text = "Estas son sus reseñas"
                    tvEstadoResenas.visibility = View.VISIBLE
                    rvMisResenas.visibility = View.VISIBLE

                    rvMisResenas.adapter = MisResenasAdapter(listaResenas) { resenaSeleccionada ->
                        val intent = Intent(this, EditarEliminarResenaActivity::class.java)
                        intent.putExtra("resenaId", resenaSeleccionada.id)
                        intent.putExtra("medicamento", resenaSeleccionada.medicamento)
                        intent.putExtra("descripcion", resenaSeleccionada.descripcion)
                        intent.putExtra("estrellas", resenaSeleccionada.estrellas)
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar las reseñas", Toast.LENGTH_SHORT).show()
            }
    }
}
