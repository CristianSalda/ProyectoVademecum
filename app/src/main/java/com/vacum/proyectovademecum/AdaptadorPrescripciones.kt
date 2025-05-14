package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class AdaptadorPrescripciones(
    private val listaElementos: MutableList<Pair<String, NuevaPree.MedicamentoPrescrito>>,
    private val onItemClick: (Pair<String, NuevaPree.MedicamentoPrescrito>) -> Unit,
    private val onDeleteClick: (Pair<String, NuevaPree.MedicamentoPrescrito>) -> Unit
) : RecyclerView.Adapter<AdaptadorPrescripciones.ItemViewHolder>() {

    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescripcion, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = listaElementos.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val (nombreOrden, medicamento) = listaElementos[position]
        val isExpanded = position == expandedPosition
        holder.bind(nombreOrden, medicamento, isExpanded)
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombreOrden: TextView = view.findViewById(R.id.tvNombrePrescripcion)
        private val btnEliminar: ImageView = view.findViewById(R.id.btnEliminar)
        private val cardDosis: View = view.findViewById(R.id.cardDosis)
        private val tvCantidad: TextView = view.findViewById(R.id.tvCantidad)
        private val tvTiempo: TextView = view.findViewById(R.id.tvTiempo)

        fun bind(nombreOrden: String, medicamento: NuevaPree.MedicamentoPrescrito, isExpanded: Boolean) {
            tvNombreOrden.text = nombreOrden
            tvCantidad.text = medicamento.cantidadDosis.toString()

            val tiempoRestanteMillis = medicamento.proximaToma - System.currentTimeMillis()
            val horasRestantes = TimeUnit.MILLISECONDS.toHours(tiempoRestanteMillis)
            val minutosRestantes = TimeUnit.MILLISECONDS.toMinutes(tiempoRestanteMillis) % 60

            if (tiempoRestanteMillis <= 0) {
                tvTiempo.text = "Tomar ahora"
            } else {
                tvTiempo.text = String.format("%02d:%02d", horasRestantes, minutosRestantes)
            }

            cardDosis.visibility = if (isExpanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (expandedPosition == currentPosition) {
                        expandedPosition = -1
                    } else {
                        val prev = expandedPosition
                        expandedPosition = currentPosition
                        notifyItemChanged(prev)
                    }
                    notifyItemChanged(currentPosition)
                }
            }

            btnEliminar.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(Pair(nombreOrden, medicamento))
                }
            }
        }
    }

    fun actualizarTiempoRestante() {
        notifyItemRangeChanged(0, itemCount)
    }
}