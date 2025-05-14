package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialAdapter(
    private val lista: MutableList<HistorialBusqueda>,
    private val onEliminarClick: (HistorialBusqueda, Int) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.nombreMedicamento)
        val descripcion: TextView = view.findViewById(R.id.descripcionMedicamento)
        val btnEliminar: ImageView = view.findViewById(R.id.btnEliminar)

        fun bind(item: HistorialBusqueda, position: Int) {
            nombre.text = item.nombre
            val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(item.fecha))
            descripcion.text = fechaFormateada

            btnEliminar.setOnClickListener {
                onEliminarClick(item, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position], position)
    }

    override fun getItemCount() = lista.size

    fun eliminarItem(pos: Int) {
        lista.removeAt(pos)
        notifyItemRemoved(pos)
    }
}

