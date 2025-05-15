package com.vacum.proyectovademecum

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


class Mainespecialista : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainespecialista)


        val configcuenta = findViewById<LinearLayout>(R.id.ConfigCuenta)

        configcuenta.setOnClickListener {
            val intent = Intent(this, ActivityConfigurarPerfil::class.java)
            startActivity(intent)
        }


        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.setOnClickListener {
            try {
                val intent = Intent(this@Mainespecialista, Mainbusqueda::class.java)
                intent.putExtra("query", searchInput.text.toString())
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar búsqueda", Toast.LENGTH_SHORT).show()
            }
        }

        val notas = findViewById<LinearLayout>(R.id.LinearLayoutNotas)

        notas.setOnClickListener {
            val intent = Intent(this, NotasActivity::class.java)
            startActivity(intent)
        }

        val favoritos = findViewById<LinearLayout>(R.id.favoritos)

        favoritos.setOnClickListener {
            val intent = Intent(this, Activity_guardados::class.java)
            startActivity(intent)
        }
        val historialBtn = findViewById<LinearLayout>(R.id.itemHistorial)
        historialBtn.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }
        val Resenas = findViewById<LinearLayout>(R.id.Resenas)
        Resenas.setOnClickListener {
            val intent = Intent(this, ResenasInicioActivity::class.java)
            startActivity(intent)
        }

        val preescripcion = findViewById<ImageView>(R.id.imgPreescripcion)
        preescripcion.setOnClickListener {
            val intent = Intent(this, NuevaPree::class.java)
            startActivity(intent)
        }

        val perfil = findViewById<ImageView>(R.id.accountIcon)

        perfil.setOnClickListener {
            val intent = Intent(this, ActivityPerfil::class.java)
            startActivity(intent)
        }

        val buscar_por_tipo = findViewById<LinearLayout>(R.id.busquedaPorTipos)

        buscar_por_tipo.setOnClickListener{
            val intent = Intent(this, ActivityBuscarPorTipo::class.java)
            startActivity(intent)
        }



    }
}