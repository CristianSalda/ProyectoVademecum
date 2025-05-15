package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainBusquedaAdapter(
    private var medicamentos: List<Medicamento>,
    private val onShowFullScreen: (Medicamento) -> Unit,
    private val userId: String,
    private val googleTranslateApiKey: String
) : RecyclerView.Adapter<MainBusquedaAdapter.MedicamentoViewHolder>() {

    private var expandedPosition: Int = RecyclerView.NO_POSITION
    private val firestore = FirebaseFirestore.getInstance()
    private var favoritosListener: ListenerRegistration? = null
    private var listaDeFavoritos = mutableSetOf<String>()
    private val traductorApi: Mainbusqueda.GoogleTranslateApi

    init {
        val retrofitGoogle = Retrofit.Builder()
            .baseUrl("https://translation.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        traductorApi = retrofitGoogle.create(Mainbusqueda.GoogleTranslateApi::class.java)

        if (userId.isNotEmpty()) {
            cargarFavoritosIniciales()
        }
    }

    private fun cargarFavoritosIniciales() {
        favoritosListener = firestore.collection("usuarios")
            .document(userId)
            .collection("favoritos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { document ->
                    val nombreFavorito = document.getString("nombre")
                    nombreFavorito?.let { listaDeFavoritos.add(it) }
                }
                notifyDataSetChanged()
            }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        favoritosListener?.remove()
    }

    inner class MedicamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreCollapsed: TextView = itemView.findViewById(R.id.tvNombreCollapsed)
        private val btnFavoritoCollapsed: ImageButton = itemView.findViewById(R.id.btnFavoritoCollapsed)
        private val layoutExpanded: LinearLayout = itemView.findViewById(R.id.layoutExpanded)
        private val tvDescripcionCompleta: TextView = itemView.findViewById(R.id.tvDescripcionCompleta)
        private val btnFavoritoExpanded: ImageButton = itemView.findViewById(R.id.btnFavoritoExpanded)

        fun bind(medicamento: Medicamento, position: Int) {
            val isExpanded = position == expandedPosition

            val nombre = medicamento.openfda?.brand_name?.firstOrNull() ?: "Nombre no disponible"
            val estaEnFavoritos = listaDeFavoritos.contains(nombre)

            tvNombreCollapsed.text = nombre
            setFavIcon(btnFavoritoCollapsed, estaEnFavoritos)

            btnFavoritoCollapsed.setOnClickListener {
                toggleFavorito(nombre, obtenerDescripcion(medicamento), estaEnFavoritos)
            }

            layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            setFavIcon(btnFavoritoExpanded, estaEnFavoritos)

            btnFavoritoExpanded.setOnClickListener {
                toggleFavorito(nombre, obtenerDescripcion(medicamento), estaEnFavoritos)
            }

            itemView.setOnClickListener {
                if (isExpanded) {
                    onShowFullScreen(medicamento)
                    expandedPosition = RecyclerView.NO_POSITION
                    notifyItemChanged(position)
                } else {
                    val previousExpandedPosition = expandedPosition
                    expandedPosition = if (position == expandedPosition) RecyclerView.NO_POSITION else position
                    notifyItemChanged(previousExpandedPosition)
                    notifyItemChanged(expandedPosition)
                    if (expandedPosition == position) {

                        CoroutineScope(Dispatchers.Main).launch {
                            val descripcionOriginal = obtenerDescripcion(medicamento)
                            val descripcionTraducida = traducirTexto(descripcionOriginal)
                            tvDescripcionCompleta.text = descripcionTraducida
                        }
                    } else {
                        tvDescripcionCompleta.text = ""
                    }
                }
            }
        }

        private fun setFavIcon(button: ImageButton, isFavorite: Boolean) {
            val icon = if (isFavorite) {
                R.drawable.bookmark_remove_24dp_007ac1_fill0_wght400_grad0_opsz24
            } else {
                R.drawable.bookmark_24dp_007ac1_fill0_wght400_grad0_opsz24
            }
            button.setImageDrawable(ContextCompat.getDrawable(itemView.context, icon))
        }

        private fun obtenerDescripcion(medicamento: Medicamento): String {
            return buildString {
                medicamento.openfda?.manufacturer_name?.firstOrNull()?.let {
                    append("Fabricante: $it\n\n")
                }
                medicamento.purpose?.firstOrNull()?.let {
                    append("Propósito: $it\n\n")
                }
                medicamento.indications_and_usage?.firstOrNull()?.let {
                    append("Indicaciones y uso: $it\n\n")
                }
                medicamento.warnings?.firstOrNull()?.let {
                    append("Advertencias: $it\n\n")
                }
                medicamento.when_using?.firstOrNull()?.let {
                    append("Cuándo usar: $it\n\n")
                }
                medicamento.ask_a_doctor?.firstOrNull()?.let {
                    append("Consulte a su médico: $it\n\n")
                }
                medicamento.stop_use?.firstOrNull()?.let {
                    append("Deje de usar: $it")
                }
            }
        }

        private suspend fun traducirTexto(texto: String): String {
            return try {
                val response = traductorApi.traducirTexto(
                    texto = texto,
                    idiomaDestino = "es",
                    apiKey = googleTranslateApiKey
                )
                if (response.isSuccessful) {
                    response.body()?.data?.translations?.firstOrNull()?.translatedText ?: texto
                } else {
                    texto
                }
            } catch (e: Exception) {
                texto
            }
        }

        private fun toggleFavorito(nombre: String, descripcion: String, esFavorito: Boolean) {
            if (userId.isEmpty()) return

            if (esFavorito) {
                removerDeFavoritos(nombre)
            } else {
                guardarEnFavoritos(nombre, descripcion)
            }
        }

        private fun guardarEnFavoritos(nombre: String, descripcion: String) {
            val favorito = MedicamentoFavorito(nombre, descripcion, userId)

            firestore.collection("usuarios")
                .document(userId)
                .collection("favoritos")
                .add(favorito)
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Guardado en favoritos", Toast.LENGTH_SHORT).show()
                    listaDeFavoritos.add(nombre)
                    notifyItemChanged(adapterPosition)
                }
                .addOnFailureListener {
                    Toast.makeText(itemView.context, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }

        private fun removerDeFavoritos(nombre: String) {
            firestore.collection("usuarios")
                .document(userId)
                .collection("favoritos")
                .whereEqualTo("nombre", nombre)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    querySnapshot.forEach { document ->
                        document.reference.delete()
                            .addOnSuccessListener {
                                Toast.makeText(itemView.context, "Removido de favoritos", Toast.LENGTH_SHORT).show()
                                listaDeFavoritos.remove(nombre)
                                notifyItemChanged(adapterPosition)
                            }
                            .addOnFailureListener {
                                Toast.makeText(itemView.context, "Error al remover", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(itemView.context, "Error al verificar favoritos", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun updateList(newList: List<Medicamento>) {
        medicamentos = newList
        expandedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento_busqueda, parent, false)
        return MedicamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        holder.bind(medicamentos[position], position)
    }

    override fun getItemCount() = medicamentos.size
}