package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MisResenasAdapter(
    private val listaResenas: List<Resena>,
    private val onItemClick: (Resena) -> Unit
) : RecyclerView.Adapter<MisResenasAdapter.ResenaViewHolder>() {

    inner class ResenaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreMedicamento: TextView = view.findViewById(R.id.tvMedicamento)
        val iconoEstrella: ImageView = view.findViewById(R.id.iconoEstrella)

        init {
            view.setOnClickListener {
                onItemClick(listaResenas[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResenaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mis_resenas, parent, false)
        return ResenaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResenaViewHolder, position: Int) {
        val resena = listaResenas[position]
        holder.tvNombreMedicamento.text = resena.medicamento
        // El ícono es fijo, ya está en el XML
    }

    override fun getItemCount(): Int = listaResenas.size
}
