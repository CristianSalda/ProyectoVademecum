package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

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
        codigoGenerado = Random.nextInt(100000, 999999).toString()
        val email = auth.currentUser?.email ?: return

        val asunto = "Código de verificación - Eliminación de cuenta"
        val mensaje = "Tu código de verificación es: $codigoGenerado"

        // Simulación de envío - usa tu propio backend o integración con servicios de email
        // Aquí podrías usar funciones de Cloud Functions o servicios externos

        Toast.makeText(this, "Código enviado a $email", Toast.LENGTH_LONG).show()
    }

    private fun eliminarCuenta() {
        val uid = auth.currentUser?.uid ?: return

        // Eliminar en Firestore
        db.collection("usuarios").document(uid)
            .delete()
            .addOnSuccessListener {
                // Eliminar del Auth
                auth.currentUser?.delete()
                    ?.addOnSuccessListener {
                        startActivity(Intent(this, CuentaEliminadaActivity::class.java))
                        finish()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar cuenta de autenticación", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar datos", Toast.LENGTH_LONG).show()
            }
    }
}
