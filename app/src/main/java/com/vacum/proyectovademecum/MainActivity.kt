package com.vacum.proyectovademecum

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnEmergency = findViewById<Button>(R.id.btnEmergency)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, ActivityLogin::class.java))
            finish() // Esto cerrará la pantalla de login
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, ActivityRegister::class.java))
            finish() // Esto cerrará la pantalla de login
        }


        btnEmergency.setOnClickListener {
            // Navegar a EmergencyActivity (aún por crear)
        }
    }
}