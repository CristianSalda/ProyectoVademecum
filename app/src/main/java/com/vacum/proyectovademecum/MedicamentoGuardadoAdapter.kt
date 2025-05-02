package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicamentoGuardadoAdapter(
    private val medicamentos: MutableList<String>,
    private val onEliminarClick: (String) -> Unit
) : RecyclerView.Adapter<MedicamentoGuardadoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textoMedicamento: TextView = itemView.findViewById(R.id.textMedicamento)
        val btnEliminar: ImageView = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento_guardado, parent, false)
        return ViewHolder(view)
    }

    fun extraerCampo(texto: String, clave: String): String {
        return texto.lineSequence()
            .firstOrNull { it.trim().startsWith(clave) }
            ?.substringAfter(clave)
            ?.trim() ?: "Desconocido"
    }

    override fun getItemCount(): Int = medicamentos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medicamento = medicamentos[position]

        // 👈 AQUÍ VA EL CÓDIGO:
        val partes = medicamento.split(" - ")
        val marca = partes.getOrNull(0) ?: "Desconocido"
        val fabricante = partes.getOrNull(1) ?: "Desconocido"

        holder.textoMedicamento.text = "$marca - $fabricante"

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(medicamento)
        }

        // ✅ nueva función
        fun eliminarMedicamento(medicamento: String) {
            val index = medicamentos.indexOf(medicamento)
            if (index != -1) {
                medicamentos.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }
}


