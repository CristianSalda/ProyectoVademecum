package com.vacum.proyectovademecum

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout


class NuevaPree : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nueva_pree)
        val imageViewAgregar = findViewById<ImageView>(R.id.imageView8)

        imageViewAgregar.setOnClickListener {
            val intent = Intent(this, Mainnatural::class.java)
            startActivity(intent)
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnAgregar = findViewById<LinearLayout>(R.id.addPrescriptionButton)

        btnAgregar.setOnClickListener {
            val intent = Intent(this, AgregarMedica::class.java)
            startActivity(intent)
        }

    }
}