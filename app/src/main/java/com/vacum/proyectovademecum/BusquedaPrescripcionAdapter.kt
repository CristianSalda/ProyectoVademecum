package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class BusquedaPrescripcionAdapter(
    private var listaMedicamentos: List<Medicamento>,
    private val onMedicamentoSeleccionado: (Medicamento, Int, Int) -> Unit
) : RecyclerView.Adapter<BusquedaPrescripcionAdapter.ViewHolder>() {

    private var expandedPosition = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMedicamentoBusqueda)
        val cardDosis: View = itemView.findViewById(R.id.cardDosisBusqueda)
        val etCantidadProxima: EditText = itemView.findViewById(R.id.etCantidadProxima)
        val etTiempoProxima: EditText = itemView.findViewById(R.id.etTiempoProxima)
        val btnAgregarOrden: Button = itemView.findViewById(R.id.btnAgregarOrden)

        fun bind(medicamento: Medicamento, position: Int) {
            tvNombre.text = medicamento.openfda?.brand_name?.firstOrNull() ?: "Nombre no encontrado"

            val isExpanded = position == expandedPosition
            cardDosis.visibility = if (isExpanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val prev = expandedPosition
                expandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(prev)
                notifyItemChanged(position)
            }

            btnAgregarOrden.setOnClickListener {
                val cantidad = etCantidadProxima.text.toString().toIntOrNull() ?: 0
                val tiempoProxima = etTiempoProxima.text.toString().toIntOrNull() ?: 0
                if (cantidad > 0 && tiempoProxima > 0) {
                    onMedicamentoSeleccionado(medicamento, cantidad, tiempoProxima)
                    val prev = expandedPosition
                    expandedPosition = -1
                    notifyItemChanged(prev)
                    notifyItemChanged(adapterPosition)

                    etCantidadProxima.text.clear()
                    etTiempoProxima.text.clear()
                } else {
                    Toast.makeText(itemView.context, "Por favor, ingrese cantidad y tiempo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento_seleccionado, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listaMedicamentos[position], position)
    }

    override fun getItemCount(): Int = listaMedicamentos.size

    fun updateList(newList: List<Medicamento>) {
        listaMedicamentos = newList
        notifyDataSetChanged()
    }

}