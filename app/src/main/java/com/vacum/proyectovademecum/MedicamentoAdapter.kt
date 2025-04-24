package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MedicamentoAdapter(
    private val medicamentos: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onAgregarClick: (String) -> Unit // Nuevo callback para el botón +
) : RecyclerView.Adapter<MedicamentoAdapter.MedicamentoViewHolder>() {

    class MedicamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val texto: TextView = itemView.findViewById(R.id.textoMedicamento)
        val btnAgregar: ImageButton = itemView.findViewById(R.id.btnAgregar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false)
        return MedicamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        val textoCompleto = medicamentos[position]
        val primeraLinea = textoCompleto.lines().firstOrNull() ?: textoCompleto

        holder.texto.text = primeraLinea

        // Click en el item
        holder.itemView.setOnClickListener {
            onItemClick(textoCompleto)
        }

        // Click en el botón +
        holder.btnAgregar.setOnClickListener {
            onAgregarClick(textoCompleto)
        }
    }

    override fun getItemCount(): Int = medicamentos.size
}