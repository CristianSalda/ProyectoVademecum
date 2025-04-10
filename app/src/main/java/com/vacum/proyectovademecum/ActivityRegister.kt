package com.vacum.proyectovademecum

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ActivityRegister : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        FirebaseApp.initializeApp(this)

        val nombreEditText = findViewById<EditText>(R.id.nombreEditText)
        val apellidoEditText = findViewById<EditText>(R.id.apellidoEditText)
        val correoEditText = findViewById<EditText>(R.id.emailEditText)
        val usuarioEditText = findViewById<EditText>(R.id.usernameEditText)
        val contrasenaEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmarEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val fechaNacimientoEditText = findViewById<EditText>(R.id.fechaNacimientoEditText)
        val tipoPersonaField = findViewById<AutoCompleteTextView>(R.id.tipoPersonaAutoComplete)
        val botonRegistro = findViewById<Button>(R.id.registerButton)


        fechaNacimientoEditText.setOnClickListener {
            val calendario = Calendar.getInstance()
            val year = calendario.get(Calendar.YEAR)
            val month = calendario.get(Calendar.MONTH)
            val day = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val fecha = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                fechaNacimientoEditText.setText(fecha)
            }, year, month, day)

            datePicker.show()
        }

        botonRegistro.setOnClickListener {
            val nombre = nombreEditText.text.toString().trim()
            val apellido = apellidoEditText.text.toString().trim()
            val correo = correoEditText.text.toString().trim()
            val usuario = usuarioEditText.text.toString().trim()
            val contrasena = contrasenaEditText.text.toString().trim()
            val confirmar = confirmarEditText.text.toString().trim()
            val fechaNacimiento = fechaNacimientoEditText.text.toString().trim()
            val tipoPersona = tipoPersonaField.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || usuario.isEmpty()
                || contrasena.isEmpty() || confirmar.isEmpty() || fechaNacimiento.isEmpty()
                || tipoPersona.isEmpty()
            ) {
                Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Correo inválido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena != confirmar) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (tipoPersona.lowercase() != "natural" && tipoPersona.lowercase() != "especialista") {
                Toast.makeText(this, "Tipo de persona inválido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Firebase.firestore.collection("usuarios")
                .whereEqualTo("usuario", usuario)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(this, "El nombre de usuario ya existe.", Toast.LENGTH_SHORT).show()
                    } else {
                        crearUsuarioEnFirebase(correo, contrasena, mapOf(
                            "nombre" to nombre,
                            "apellido" to apellido,
                            "usuario" to usuario,
                            "fechaNacimiento" to fechaNacimiento,
                            "tipoPersona" to tipoPersona
                        ))
                    }
                }

        }
        val opciones = listOf("Natural", "Especialista")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, opciones)
        tipoPersonaField.setAdapter(adapter)

        tipoPersonaField.setOnClickListener {
            tipoPersonaField.showDropDown()
        }
    }

    private fun crearUsuarioEnFirebase(email: String, password: String, datos: Map<String, String>) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: return@addOnCompleteListener

                    // Guardar en Firestore
                    Firebase.firestore.collection("usuarios").document(uid)
                        .set(datos)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Documento guardado en Firestore")
                            Toast.makeText(this@ActivityRegister, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@ActivityRegister, ActivityLogin::class.java)
                            startActivity(intent)
                            finish()
                        }

                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error al guardar en Firestore: ${e.message}")
                            Toast.makeText(this@ActivityRegister, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this@ActivityRegister, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

}
