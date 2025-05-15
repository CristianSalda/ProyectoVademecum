package com.vacum.proyectovademecum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ResenaAdapter(private val resenas: List<ResenaCompleta>) :
    RecyclerView.Adapter<ResenaAdapter.ResenaViewHolder>() {

    class ResenaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreUsuario: TextView = view.findViewById(R.id.tvNombreUsuario)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val descripcion: TextView = view.findViewById(R.id.tvDescripcion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResenaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resena, parent, false)
        return ResenaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResenaViewHolder, position: Int) {
        val resena = resenas[position]
        holder.nombreUsuario.text = resena.nombreUsuario
        holder.ratingBar.rating = resena.votacion.toFloat()
        holder.descripcion.text = resena.descripcion
    }

    override fun getItemCount(): Int = resenas.size
}
