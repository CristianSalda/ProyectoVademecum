package com.vacum.proyectovademecum

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions


class ActivityEliminarCuenta : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var inputCodigo: EditText
    private lateinit var btnEliminar: Button
    private lateinit var btnCancelar: Button
    private lateinit var reenviarCodigo: TextView

    private var codigoGenerado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eliminar_cuenta)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        inputCodigo = findViewById(R.id.inputCodigoVerificacion)
        btnEliminar = findViewById(R.id.btnEliminarCuentaConfirmado)
        btnCancelar = findViewById(R.id.btnCancelar)
        reenviarCodigo = findViewById(R.id.reenviarCodigoText)

        generarYEnviarCodigo()

        btnEliminar.setOnClickListener {
            val codigoIngresado = inputCodigo.text.toString().trim()
            if (codigoIngresado == codigoGenerado) {
                eliminarCuenta()
            } else {
                Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        reenviarCodigo.setOnClickListener {
            generarYEnviarCodigo()
            Toast.makeText(this, "Código reenviado", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.atras).setOnClickListener {
            finish()
        }
    }

    private fun generarYEnviarCodigo() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "El usuario no tiene un email asociado", Toast.LENGTH_LONG).show()
            return
        }

        codigoGenerado = Random.nextInt(100000, 999999).toString()

        val data = hashMapOf(
            "email" to email,
            "codigo" to codigoGenerado
        )

        Firebase.functions
            .getHttpsCallable("enviarCodigoVerificacion")
            .call(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Código enviado a $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar código: ${e.message}", Toast.LENGTH_LONG).show()
            }

    }


    private fun eliminarCuenta() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: run {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Mostrar progreso
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Eliminando cuenta...")
            setCancelable(false)
            show()
        }

        // 2. Eliminar datos secundarios primero (opcional)
        eliminarDatosSecundarios(uid, {
            // 3. Eliminar documento principal
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .delete()
                .addOnSuccessListener {
                    // 4. Eliminar usuario de Auth
                    user.delete()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            // 5. Redirigir y cerrar sesión
                            FirebaseAuth.getInstance().signOut()
                            startActivity(Intent(this@ActivityEliminarCuenta, CuentaEliminadaActivity::class.java))
                            finishAffinity()
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Log.e("ELIMINAR", "Error Auth: ${e.message}")
                            Toast.makeText(
                                this,
                                "Error al eliminar autenticación. Intenta cerrar sesión y volver a ingresar.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Log.e("ELIMINAR", "Error Firestore: ${e.message}")
                    Toast.makeText(
                        this,
                        "Error al eliminar datos. Verifica tu conexión.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }, { error ->
            progressDialog.dismiss()
            Toast.makeText(this, "Error al limpiar datos: $error", Toast.LENGTH_LONG).show()
        })
    }

    private fun eliminarDatosSecundarios(uid: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        // Opcional: Eliminar subcolecciones u otros datos relacionados
        val batch = FirebaseFirestore.getInstance().batch()

        // Ejemplo: Eliminar medicamentos favoritos
        FirebaseFirestore.getInstance().collection("medicamentos_favs")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Error desconocido") }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Error al obtener datos") }
    }
}
