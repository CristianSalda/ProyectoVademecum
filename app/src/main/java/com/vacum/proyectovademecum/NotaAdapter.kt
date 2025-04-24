package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotaAdapter(
    private val notas: MutableList<Nota>,
    private val onNotaClick: (Nota) -> Unit,
    private val onNotaDelete: (Nota) -> Unit
) : RecyclerView.Adapter<NotaAdapter.NotaViewHolder>() {

    class NotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.txtTituloNota)
        val tvContenido: TextView = view.findViewById(R.id.txtContenidoNota)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]
        holder.tvTitulo.text = nota.titulo
        holder.tvContenido.text = nota.contenido

        holder.itemView.setOnClickListener { onNotaClick(nota) }
        holder.btnEliminar.setOnClickListener { onNotaDelete(nota) }
    }

    override fun getItemCount() = notas.size

    fun updateList(newList: List<Nota>) {
        notas.clear()
        notas.addAll(newList)
        notifyDataSetChanged()
    }
}