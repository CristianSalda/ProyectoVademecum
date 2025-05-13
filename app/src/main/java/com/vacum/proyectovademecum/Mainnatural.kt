package com.vacum.proyectovademecum
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Mainnatural : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_natural)

        val logoutButton = findViewById<ImageView>(R.id.btnLogout)

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

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
        val configcuenta = findViewById<LinearLayout>(R.id.ConfigCuenta)

        configcuenta.setOnClickListener {
            val intent = Intent(this, ActivityConfigurarPerfil::class.java)
            startActivity(intent)
        }
        val historialBtn = findViewById<LinearLayout>(R.id.itemHistorial)
        historialBtn.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
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
    }
}
