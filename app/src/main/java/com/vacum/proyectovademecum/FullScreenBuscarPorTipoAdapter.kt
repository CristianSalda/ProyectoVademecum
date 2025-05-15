package com.vacum.proyectovademecum

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FullScreenBuscarPorTipoAdapter(
    private var medicamentos: List<Medicamento>
) : RecyclerView.Adapter<FullScreenBuscarPorTipoAdapter.MedicamentoViewHolder>() {

    private var expandedPosition: Int = RecyclerView.NO_POSITION
    private val firestore = FirebaseFirestore.getInstance()
    private var favoritosListener: ListenerRegistration? = null
    private var listaDeFavoritos = mutableSetOf<String>()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
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
                    Log.e("Firestore", "Error al cargar favoritos: $error")
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
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCollapsed)
        private val layoutDescripcion: LinearLayout = itemView.findViewById(R.id.layoutExpanded)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionCompleta)
        private val btnFavorito: ImageButton = itemView.findViewById(R.id.btnFavorito)

        fun bind(medicamento: Medicamento, position: Int) {
            val isExpanded = position == expandedPosition
            val nombre = medicamento.openfda?.brand_name?.firstOrNull() ?: "Nombre no disponible"
            val descripcion = obtenerDescripcion(medicamento)
            val estaEnFavoritos = listaDeFavoritos.contains(nombre)

            tvNombre.text = nombre
            tvDescripcion.text = descripcion
            layoutDescripcion.visibility = if (isExpanded) View.VISIBLE else View.GONE
            setFavIcon(btnFavorito, estaEnFavoritos)

            tvNombre.setOnClickListener {
                val previousExpandedPosition = expandedPosition
                expandedPosition = if (position == expandedPosition) RecyclerView.NO_POSITION else position
                notifyItemChanged(previousExpandedPosition)
                notifyItemChanged(position)
            }

            btnFavorito.setOnClickListener {
                if (estaEnFavoritos) {
                    removerDeFavoritos(nombre)
                } else {
                    guardarEnFavoritos(nombre, descripcion)
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

        private fun guardarEnFavoritos(nombre: String, descripcion: String) {
            if (userId.isEmpty()) return

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
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error al guardar favorito: ${e.message}")
                }
        }

        private fun removerDeFavoritos(nombre: String) {
            if (userId.isEmpty()) return

            firestore.collection("usuarios")
                .document(userId)
                .collection("favoritos")
                .whereEqualTo("nombre", nombre)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Log.w("Firestore", "No se encontró el medicamento '$nombre' en favoritos para eliminar")
                        return@addOnSuccessListener
                    }
                    querySnapshot.forEach { document ->
                        document.reference.delete()
                            .addOnSuccessListener {
                                Toast.makeText(itemView.context, "Removido de favoritos", Toast.LENGTH_SHORT).show()
                                listaDeFavoritos.remove(nombre)
                                notifyItemChanged(adapterPosition)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(itemView.context, "Error al remover: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("Firestore", "Error al eliminar favorito: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Error al verificar favoritos: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error al verificar favoritos: ${e.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tipo_medicamento, parent, false)
        return MedicamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        holder.bind(medicamentos[position], position)
    }

    override fun getItemCount(): Int = medicamentos.size

    fun updateList(newList: List<Medicamento>) {
        medicamentos = newList
        expandedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }
}