package com.vacum.proyectovademecum

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar

class ActivityConfigurarPerfil : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etFechaNacimiento: EditText

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
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        etUsername = findViewById(R.id.etUsername)
        etTipoPersona = findViewById(R.id.etTipoPersona)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta)
        etFechaNacimiento.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val fechaFormateada = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                etFechaNacimiento.setText(fechaFormateada)
            }, year, month, day).show()
        }



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
            setResult(RESULT_OK)
            finish()

        }

        btnEliminarCuenta.setOnClickListener {
           startActivity(Intent(this, ActivityEliminarCuenta::class.java))
        }

        val btnAtras = findViewById<ImageView>(R.id.atras)
        btnAtras.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val tipo = doc.getString("tipoPersona")?.lowercase()

                        when (tipo) {
                            "natural" -> {
                                startActivity(Intent(this, Mainnatural::class.java))
                                finish()
                            }
                            "especialista" -> {
                                startActivity(Intent(this, Mainespecialista::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "Tipo de usuario no reconocido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "No se pudo verificar el tipo de usuario", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
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
                    etNombre.setText(document.getString("nombre") ?: "")
                    etApellido.setText(document.getString("apellido") ?: "")
                    etFechaNacimiento.setText(document.getString("fechaNacimiento") ?: "")
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
            "nombre" to etNombre.text.toString().trim(),
            "apellido" to etApellido.text.toString().trim(),
            "fechaNacimiento" to etFechaNacimiento.text.toString().trim(),
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