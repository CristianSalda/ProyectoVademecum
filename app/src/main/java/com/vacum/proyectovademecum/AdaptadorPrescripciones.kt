package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdaptadorPrescripciones(
    private val prescripciones: List<NuevaPree.MedicamentoPrescrito>,
    private val onAgregarClick: () -> Unit,
    private val onItemClick: (NuevaPree.MedicamentoPrescrito) -> Unit,
    private val onDeleteClick: (NuevaPree.MedicamentoPrescrito) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var expandedPosition = -1

    companion object {
        private const val TYPE_BOTON = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_BOTON else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_BOTON) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_boton_agre, parent, false)
            BotonViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_prescripcion, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun getItemCount(): Int = prescripciones.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BotonViewHolder) {
            holder.bind()
        } else if (holder is ItemViewHolder) {
            val realPosition = position - 1
            if (realPosition in prescripciones.indices) {
                val prescripcion = prescripciones[realPosition]
                holder.bind(prescripcion, realPosition)
            }
        }
    }

    inner class BotonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            itemView.setOnClickListener {
                onAgregarClick()
            }
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombrePrescripcion)
        private val btnEliminar: ImageView = view.findViewById(R.id.btnEliminar)
        private val cardDosis: View = view.findViewById(R.id.cardDosis)
        private val tvCantidad: TextView = view.findViewById(R.id.tvCantidad)
        private val tvTiempo: TextView = view.findViewById(R.id.tvTiempo)

        fun bind(prescripcion: NuevaPree.MedicamentoPrescrito, position: Int) {
            tvNombre.text = prescripcion.nombre
            tvCantidad.text = prescripcion.cantidadDosis.toString()
            tvTiempo.text = "${prescripcion.tiempoEntreDosis} h"

            val isExpanded = position == expandedPosition
            cardDosis.visibility = if (isExpanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val realPos = currentPosition - 1
                if (expandedPosition == realPos) {
                    expandedPosition = -1
                } else {
                    val prev = expandedPosition
                    expandedPosition = realPos
                    notifyItemChanged(prev + 1)
                }
                notifyItemChanged(currentPosition)
            }

            itemView.setOnLongClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(prescripcion)
                }
                true
            }

            btnEliminar.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(prescripcion)
                }
            }
        }
    }
}