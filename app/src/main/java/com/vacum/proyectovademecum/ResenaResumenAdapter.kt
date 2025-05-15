package com.vacum.proyectovademecum.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vacum.proyectovademecum.R
import com.vacum.proyectovademecum.Resena

class ResenaResumenAdapter(
    private val lista: List<Resena>,
    private val onClick: (Resena) -> Unit
) : RecyclerView.Adapter<ResenaResumenAdapter.ResenaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResenaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resena_resumida, parent, false)
        return ResenaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResenaViewHolder, position: Int) {
        val resena = lista[position]
        holder.bind(resena)
        holder.itemView.setOnClickListener { onClick(resena) }
    }

    override fun getItemCount(): Int = lista.size

    inner class ResenaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMedicamento)
        private val contenedorEstrellas: LinearLayout = itemView.findViewById(R.id.contenedorEstrellas)

        fun bind(resena: Resena) {
            tvNombre.text = resena.medicamento
            contenedorEstrellas.removeAllViews()

            repeat(resena.estrellas) {
                val estrella = ImageView(itemView.context).apply {
                    setImageResource(R.drawable.estrella)
                    layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                        setMargins(4, 0, 4, 0)
                    }
                }
                contenedorEstrellas.addView(estrella)
            }
        }
    }
}
