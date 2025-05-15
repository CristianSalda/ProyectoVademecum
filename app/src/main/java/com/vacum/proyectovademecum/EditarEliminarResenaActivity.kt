package com.vacum.proyectovademecum

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditarEliminarResenaActivity : AppCompatActivity() {

    private lateinit var tvMedicamento: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var btnEditar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnAtras: ImageView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var resena: Resena

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_eliminar_resena)

        // Referencias UI
        tvMedicamento = findViewById(R.id.tvMedicamento)
        tvDescripcion = findViewById(R.id.etDescripcionEdit)
        ratingBar = findViewById(R.id.ratingBar)
        btnEditar = findViewById(R.id.btnEditar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnAtras = findViewById(R.id.atras)

        // Resena recibida
        val resenaId = intent.getStringExtra("resenaId")
        val medicamento = intent.getStringExtra("medicamento")
        val descripcion = intent.getStringExtra("descripcion")
        val estrellas = intent.getIntExtra("estrellas", 0)

        if (resenaId == null || medicamento == null) {
            Toast.makeText(this, "Error al cargar reseña", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        resena = Resena().apply {
            id = resenaId
            this.medicamento = medicamento
            this.descripcion = descripcion
            this.estrellas = estrellas
        }


        // Mostrar datos
        tvMedicamento.text = resena.medicamento
        tvDescripcion.text = resena.descripcion ?: "Sin descripción"
        ratingBar.rating = resena.estrellas.toFloat()

        btnEditar.setOnClickListener {
            val intent = Intent(this, FormularioEditarResenaActivity::class.java)
            intent.putExtra("resena", resena)
            startActivityForResult(intent, 100) // <<-- importante
        }


        btnEliminar.setOnClickListener {
            mostrarConfirmacionEliminacion()
        }

        btnAtras.setOnClickListener {
            finish()
        }
    }

    private fun mostrarConfirmacionEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar esta reseña?")
            .setPositiveButton("Sí") { _, _ -> eliminarResena() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarResena() {
        val id = resena.id
        if (id.isNullOrEmpty()) {
            Toast.makeText(this, "ID no válido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("resenas").document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reseña eliminada", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK) // 👈 le dice a la activity anterior que hubo un cambio
                finish()             // 👈 cierra esta activity
            }

            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Puedes recargar desde Firestore aquí si quieres la reseña actualizada
            finish() // Cierra y que MisResenasActivity recargue
        }
    }

}
