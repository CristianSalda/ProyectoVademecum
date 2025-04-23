package com.vacum.proyectovademecum

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class NotasActivity : AppCompatActivity() {
    private lateinit var recyclerNotas: RecyclerView
    private lateinit var adapter: NotaAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val notasList = mutableListOf<Nota>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notas_activity)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerNotas = findViewById(R.id.recyclerNotas)

        // Configurar RecyclerView
        adapter = NotaAdapter(notasList,
            onNotaClick = { nota -> showNoteDialog(nota) },
            onNotaDelete = { nota -> deleteNote(nota) }
        )
        recyclerNotas.layoutManager = LinearLayoutManager(this)
        recyclerNotas.adapter = adapter

        // Botón agregar nota
        findViewById<Button>(R.id.btnAgregarNota).setOnClickListener {
            showNoteDialog(null)
        }

        loadNotes()
    }

    private fun loadNotes() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("usuarios/$userId/notas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error cargando notas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val notes = snapshot?.toObjects(Nota::class.java)
                notes?.let {
                    notasList.clear()
                    notasList.addAll(it)
                    adapter.notifyDataSetChanged()
                    // Mostrar/ocultar texto "No hay notas"
                    findViewById<TextView>(R.id.textNoHayNotas).visibility =
                        if (it.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun showNoteDialog(nota: Nota?) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (nota == null) "Nueva nota" else "Editar nota")
            .setView(R.layout.dialog_note) // Necesitarás crear este layout
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val editTitulo = dialog.findViewById<EditText>(R.id.editTitulo)!!
            val editContenido = dialog.findViewById<EditText>(R.id.editContenido)!!

            nota?.let {
                editTitulo.setText(it.titulo)
                editContenido.setText(it.contenido)
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val titulo = editTitulo.text.toString().trim()
                val contenido = editContenido.text.toString().trim()

                if (titulo.isEmpty() || contenido.isEmpty()) {
                    Toast.makeText(this, "¡Completa todos los campos!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                saveNote(nota?.id, titulo, contenido)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun saveNote(noteId: String?, titulo: String, contenido: String) {
        val userId = auth.currentUser?.uid ?: return
        val notaRef = if (noteId == null) {
            db.collection("usuarios/$userId/notas").document()
        } else {
            db.collection("usuarios/$userId/notas").document(noteId)
        }

        val nota = Nota(
            id = notaRef.id,
            titulo = titulo,
            contenido = contenido,
            usuarioId = userId
        )

        notaRef.set(nota).addOnFailureListener {
            Toast.makeText(this, "Error guardando nota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteNote(nota: Nota) {
        val userId = auth.currentUser?.uid ?: return
        AlertDialog.Builder(this)
            .setTitle("Eliminar nota")
            .setMessage("¿Seguro que deseas eliminar esta nota?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("usuarios/$userId/notas").document(nota.id)
                    .delete()
                    .addOnFailureListener {
                        Toast.makeText(this, "Error eliminando nota", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
