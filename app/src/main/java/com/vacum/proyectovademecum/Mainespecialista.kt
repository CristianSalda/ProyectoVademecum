package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity


class Mainespecialista : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainespecialista)

        val searchBar = findViewById<LinearLayout>(R.id.searchBar)

        searchBar.setOnClickListener {
            val intent = Intent(this, Mainbusqueda::class.java)
            startActivity(intent)
        }

        val notas = findViewById<ImageView>(R.id.imageViewNotas)

        notas.setOnClickListener {
            val intent = Intent(this, NotasActivity::class.java)
            startActivity(intent)
        }
        val preescripcion = findViewById<ImageView>(R.id.imgPreescripcion)
        preescripcion.setOnClickListener {
            val intent = Intent(this, NuevaPree::class.java)
            startActivity(intent)
        }
    }
}