package com.vacum.proyectovademecum

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActivityLogin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountText: TextView
    private lateinit var btnRegisterSegment: TextView
    private lateinit var sliderView: View



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Enlazar componentes
        emailField = findViewById(R.id.emailEditText)
        passwordField = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        sliderView = findViewById(R.id.slider)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)


        // Listener del botón
        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            loginUser(email, password)
        }
        createAccountText = findViewById(R.id.createAccountText)
        btnRegisterSegment = findViewById(R.id.btnRegister)

        btnRegisterSegment.setOnClickListener {
            val animator = ObjectAnimator.ofFloat(sliderView, "translationX", sliderView.width.toFloat())
            animator.duration = 300
            animator.start()

            val intent = Intent(this, ActivityRegister::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val animator = ObjectAnimator.ofFloat(sliderView, "translationX", 0f)
            animator.duration = 300
            animator.start()
        }


        // Redirige al registro al hacer clic en "¿No tienes cuenta? Crea una"
        createAccountText.setOnClickListener {
            startActivity(Intent(this, ActivityRegister::class.java))
            finish() // Esto cerrará la pantalla de login
        }

        // Redirige al registro al hacer clic en el botón deslizable "Registro"
        btnRegisterSegment.setOnClickListener {
            startActivity(Intent(this, ActivityRegister::class.java))
            finish() // Esto cerrará la pantalla de login
        }

    }

    private fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    val db = FirebaseFirestore.getInstance()

                    if (uid != null) {
                        db.collection("usuarios").document(uid).get()
                            .addOnSuccessListener { document ->
                                val tipoPersona = document.getString("tipoPersona")
                                when (tipoPersona) {
                                    "Especialista" -> {
                                        startActivity(Intent(this, Mainespecialista::class.java))
                                        finish()
                                    }
                                    "Natural" -> {
                                        startActivity(Intent(this, Mainnatural::class.java))
                                        finish()
                                    }
                                    else -> {
                                        Toast.makeText(this, "Tipo de usuario no válido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al verificar el tipo de persona", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            }
    }




}
