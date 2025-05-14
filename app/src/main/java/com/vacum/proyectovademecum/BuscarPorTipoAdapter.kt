package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BuscarPorTipoAdapter(
    private var tipos: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<BuscarPorTipoAdapter.TipoViewHolder>() {

    inner class TipoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTipo: TextView = itemView.findViewById(R.id.medicamentoName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(tipos[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cuadrado, parent, false)
        return TipoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipoViewHolder, position: Int) {
        holder.tvTipo.text = tipos[position]
    }

    override fun getItemCount(): Int = tipos.size

    fun updateList(newList: List<String>) {
        tipos = newList
        notifyDataSetChanged()
    }
}