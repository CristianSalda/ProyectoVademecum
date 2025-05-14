package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MainBusquedaAdapter(
    private var medicamentos: List<Medicamento>,
    private val onShowFullScreen: (Medicamento) -> Unit,
    private val userId: String
) : RecyclerView.Adapter<MainBusquedaAdapter.MedicamentoViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class MedicamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvDescripcionCompleta: TextView = itemView.findViewById(R.id.tvDescripcionCompleta)
        private val btnFavorito: ImageButton = itemView.findViewById(R.id.btnFavorito)

        fun bind(medicamento: Medicamento) {
            val isExpanded = expandedPositions.contains(adapterPosition)

            val nombre = medicamento.openfda?.brand_name?.firstOrNull() ?: "Nombre no disponible"

            tvNombre.text = nombre

            val descripcion = buildString {
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

            tvDescripcionCompleta.text = descripcion
            tvDescripcionCompleta.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnFavorito.visibility = if (isExpanded) View.VISIBLE else View.GONE

            btnFavorito.setOnClickListener {
                guardarEnFavoritos(nombre, descripcion)
            }

            itemView.setOnClickListener {
                if (isExpanded) {
                    onShowFullScreen(medicamento)
                } else {
                    expandedPositions.add(adapterPosition)
                    notifyItemChanged(adapterPosition)
                }
            }
        }

        private fun guardarEnFavoritos(nombre: String, descripcion: String) {
            if (userId.isEmpty()) return

            val favorito = MedicamentoFavorito(nombre, descripcion, userId)

            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .collection("favoritos")
                .add(favorito)
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Guardado en favoritos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(itemView.context, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun updateList(newList: List<Medicamento>) {
        medicamentos = newList
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false)
        return MedicamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        holder.bind(medicamentos[position])
    }

    override fun getItemCount() = medicamentos.size
}
