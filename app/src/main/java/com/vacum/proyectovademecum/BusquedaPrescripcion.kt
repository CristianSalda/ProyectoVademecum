package com.vacum.proyectovademecum

class BusquedaPrescripcion (
    private var medicamentos: List<Medicamento>,
    private val onItemClick: (Medicamento) -> Unit
) : RecyclerView.Adapter<BusquedaMedicamentoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(medicamento: Medicamento) {
            itemView.findViewById<TextView>(R.id.tvNombreMedicamento).text =
                medicamento.openfda?.brand_name?.firstOrNull() ?: "Medicamento"
        }
    }

    fun updateList(newList: List<Medicamento>) {
        medicamentos = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resultado_busqueda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(medicamentos[position])
        holder.itemView.setOnClickListener { onItemClick(medicamentos[position]) }
    }

    override fun getItemCount(): Int = medicamentos.size
}