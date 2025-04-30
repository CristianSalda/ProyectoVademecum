package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicamentoAdapter(
    private val medicamentos: MutableList<MedicamentoFavorito>,
    private val onDeleteClick: (MedicamentoFavorito) -> Unit
) : RecyclerView.Adapter<MedicamentoAdapter.MedicamentoViewHolder>() {

    inner class MedicamentoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.nombreMedicamento)
        val descripcion: TextView = view.findViewById(R.id.descripcionMedicamento)
        val eliminar: ImageView = view.findViewById(R.id.botonEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorito, parent, false)
        return MedicamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        val med = medicamentos[position]
        holder.nombre.text = med.nombre
        holder.descripcion.text = med.descripcion

        holder.eliminar.setOnClickListener {
            onDeleteClick(med)
        }
    }

    override fun getItemCount(): Int = medicamentos.size
}
