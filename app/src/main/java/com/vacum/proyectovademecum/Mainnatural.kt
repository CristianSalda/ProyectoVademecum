package com.vacum.proyectovademecum
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.vacum.proyectovademecum.Mainespecialista

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

        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.setOnClickListener {
            try {
                val intent = Intent(this@Mainnatural, Mainbusqueda::class.java)
                intent.putExtra("query", searchInput.text.toString())
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar búsqueda", Toast.LENGTH_SHORT).show()
            }
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

        val buscar_por_tipo = findViewById<LinearLayout>(R.id.busquedaPorTiposNatural)

        buscar_por_tipo.setOnClickListener{
            val intent = Intent(this, ActivityBuscarPorTipo::class.java)
            startActivity(intent)
        }

    }
}
