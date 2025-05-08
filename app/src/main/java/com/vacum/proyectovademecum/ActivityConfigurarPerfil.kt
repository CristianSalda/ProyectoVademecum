package com.vacum.proyectovademecum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ActivityConfigurarPerfil : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvApellido: TextView
    private lateinit var tvFechaNacimiento: TextView
    private lateinit var etUsername: EditText
    private lateinit var etTipoPersona: AutoCompleteTextView
    private lateinit var etDescripcion: EditText
    private lateinit var btnGuardarCambios: Button
    private lateinit var btnEliminarCuenta: Button
    private lateinit var fotoPerfil: ImageView
    private val PICK_IMAGE_REQUEST = 1001
    private val storage = FirebaseStorage.getInstance()



    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_perfil)

        // Referencias
        tvNombre = findViewById(R.id.tvNombre)
        tvApellido = findViewById(R.id.tvApellido)
        tvFechaNacimiento = findViewById(R.id.tvFechaNacimiento)
        etUsername = findViewById(R.id.etUsername)
        etTipoPersona = findViewById(R.id.etTipoPersona)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta)

        val etCambiarContrasena: Button = findViewById(R.id.cambiarContrasena)
        etCambiarContrasena.setOnClickListener {
            val email = auth.currentUser?.email
            if (email != null) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Se envió un enlace de restablecimiento a $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al enviar el correo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "No se encontró un email asociado a tu cuenta", Toast.LENGTH_SHORT).show()
            }
        }
        fotoPerfil = findViewById(R.id.fotoPerfil)
        val agregarFotoText: TextView = findViewById(R.id.agregarFotoText)

        // Listener para cambiar la foto al hacer clic
        agregarFotoText.setOnClickListener { abrirSelectorDeImagen() }
        fotoPerfil.setOnClickListener { abrirSelectorDeImagen() }

        // AutoComplete para tipoPersona
        val opcionesTipo = listOf("Natural", "Especialista")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opcionesTipo)
        etTipoPersona.setAdapter(adapter)

        etTipoPersona.setOnClickListener {
            etTipoPersona.showDropDown()
        }

        cargarDatosUsuario()

        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        btnEliminarCuenta.setOnClickListener {
           startActivity(Intent(this, ActivityEliminarCuenta::class.java))
        }

        findViewById<ImageView>(R.id.atras).setOnClickListener {
            finish()
        }
    }
    private fun abrirSelectorDeImagen() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST)
    }
    private fun subirFotoAFirebase(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("fotos_perfil/$uid.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        guardarUrlEnFirestore(downloadUrl.toString())
                        Toast.makeText(this, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al obtener URL de descarga: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir la imagen: ${it.message}", Toast.LENGTH_LONG).show()
            }

    }
    private fun guardarUrlEnFirestore(url: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid)
            .update("fotoPerfilUrl", url)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data!!
            fotoPerfil.setImageURI(imageUri) // Vista previa inmediata
            subirFotoAFirebase(imageUri)
        }
    }

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->  // Aquí se define 'document'
                if (document.exists()) {
                    // 1. Primero cargar los datos normales
                    tvNombre.text = "Nombre: ${document.getString("nombre") ?: "-"}"
                    tvApellido.text = "Apellidos: ${document.getString("apellido") ?: "-"}"
                    tvFechaNacimiento.text = "Fecha nacimiento: ${document.getString("fechaNacimiento") ?: "-"}"
                    etUsername.setText(document.getString("usuario") ?: "")
                    etTipoPersona.setText(document.getString("tipoPersona") ?: "", false)
                    etDescripcion.setText(document.getString("descripcion") ?: "")

                    // 2. Luego cargar la foto (DENTRO de este bloque donde 'document' existe)
                    val fotoUrl = document.getString("fotoPerfilUrl")
                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(fotoUrl)
                            .into(fotoPerfil)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarCambios() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "usuario" to etUsername.text.toString().trim(),
            "tipoPersona" to etTipoPersona.text.toString().trim(),
            "descripcion" to etDescripcion.text.toString().trim()
        )

        db.collection("usuarios").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}