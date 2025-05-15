package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ActivityPerfil : AppCompatActivity() {

    private lateinit var tvInfoUsuario: TextView
    private lateinit var btnCerrarSesion: Button
    private lateinit var btnConfigurarCuenta: Button
    private lateinit var fotoPerfil: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        tvInfoUsuario = findViewById(R.id.infoUsuario)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnConfigurarCuenta = findViewById(R.id.btnConfigurarCuenta)
        fotoPerfil = findViewById(R.id.fotoPerfil)

        cargarInformacionUsuario()
        cargarFotoDePerfil()

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnConfigurarCuenta.setOnClickListener {
            val intent = Intent(this, ActivityConfigurarPerfil::class.java)
            startActivityForResult(intent, 123)
        }

        findViewById<ImageView>(R.id.atras).setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        when (doc.getString("tipoPersona")?.lowercase()) {
                            "natural" -> {
                                startActivity(Intent(this, Mainnatural::class.java))
                                finish()
                            }
                            "especialista" -> {
                                startActivity(Intent(this, Mainespecialista::class.java))
                                finish()
                            }
                            else -> Toast.makeText(this, "Tipo de usuario no reconocido", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al verificar tipo de usuario", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarInformacionUsuario() {
        val uid = auth.currentUser?.uid ?: return

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

    private fun cargarFotoDePerfil() {
        val uid = auth.currentUser?.uid ?: return

        val imagenRef = storageRef.child("perfil/$uid.jpg")

        imagenRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.cuenta)
                    .error(R.drawable.cuenta)
                    .into(fotoPerfil)
            }
            .addOnFailureListener {
                // No hacemos nada, se queda la imagen por defecto
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            cargarInformacionUsuario()
            cargarFotoDePerfil()
        }
    }
}
