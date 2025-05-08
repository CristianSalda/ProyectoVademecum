package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActivityPerfil : AppCompatActivity() {

    private lateinit var tvInfoUsuario: TextView
    private lateinit var btnCerrarSesion: Button
    private lateinit var btnConfigurarCuenta: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Referencias
        tvInfoUsuario = findViewById(R.id.infoUsuario)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnConfigurarCuenta = findViewById(R.id.btnConfigurarCuenta)

        cargarInformacionUsuario()

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnConfigurarCuenta.setOnClickListener {
            val intent = Intent(this, ActivityConfigurarPerfil::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.atras).setOnClickListener {
            finish()
        }
    }

    private fun cargarInformacionUsuario() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombre = document.getString("nombre") ?: ""
                        val apellido = document.getString("apellido") ?: ""
                        val usuario = document.getString("usuario") ?: ""
                        val fechaNacimiento = document.getString("fechaNacimiento") ?: ""
                        val tipoPersona = document.getString("tipoPersona") ?: ""
                        val descripcion = document.getString("descripcion") ?: "Sin descripción"

                        val texto = """
                            Nombre: $nombre
                            Apellido: $apellido
                            Usuario: $usuario
                            Fecha de nacimiento: $fechaNacimiento
                            Tipo de persona: $tipoPersona
                            Descripción: $descripcion
                        """.trimIndent()

                        tvInfoUsuario.text = texto
                    } else {
                        tvInfoUsuario.text = "No se encontraron datos del usuario."
                    }
                }
                .addOnFailureListener {
                    tvInfoUsuario.text = "Error al obtener los datos del usuario."
                }
        }
    }
}
