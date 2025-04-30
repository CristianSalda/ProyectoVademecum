package com.vacum.proyectovademecum
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class Mainnatural : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_natural)

        val searchBar = findViewById<LinearLayout>(R.id.searchBar)
         searchBar.setOnClickListener {
            val intent = Intent(this, Mainbusqueda::class.java)
            startActivity(intent)
        }

        val favoritos = findViewById<LinearLayout>(R.id.favoritos)

        favoritos.setOnClickListener {
            val intent = Intent(this, Activity_guardados::class.java)
            startActivity(intent)
        }

        //val preescripcion = findViewById<ImageView>(R.id.imgPreescripcion)
        //searchBar.setOnClickListener {
        //    val intent = Intent(this, NuevaPree::class.java)
        //    startActivity(intent)
        //}
    }
}
