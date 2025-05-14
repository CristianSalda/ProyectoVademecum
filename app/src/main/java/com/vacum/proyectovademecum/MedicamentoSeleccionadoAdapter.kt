package com.vacum.proyectovademecum

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicamentoSeleccionadoAdapter(
    private val medicamentos: List<MedicamentoPrescrito>,
    private val onItemClick: (MedicamentoPrescrito) -> Unit
) : RecyclerView.Adapter<MedicamentoSeleccionadoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(medicamento: MedicamentoPrescrito) {
            itemView.findViewById<TextView>(R.id.tvNombreMedicamento).text = medicamento.nombre
            itemView.findViewById<TextView>(R.id.tvDosisInfo).text =
                "Cada ${medicamento.tiempoEntreDosis} horas - ${medicamento.cantidadDosis} unidad(es)"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento_seleccionado, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(medicamentos[position])
        holder.itemView.setOnClickListener { onItemClick(medicamentos[position]) }
    }

    override fun getItemCount(): Int = medicamentos.size
}