package com.example.groupwork

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val mensajes: List<Mensaje>,
    private val usuarioActual: String
) : RecyclerView.Adapter<MessageAdapter.MensajeViewHolder>() {

    companion object {
        private const val TIPO_DERECHA = 1
        private const val TIPO_IZQUIERDA = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensajes[position].autor == usuarioActual) TIPO_DERECHA else TIPO_IZQUIERDA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val layoutId = if (viewType == TIPO_DERECHA)
            R.layout.item_mensaje_derecha
        else
            R.layout.item_mensaje_izquierda

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MensajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.tvMensaje.text = mensaje.contenido
        holder.tvAutor.text = mensaje.autor
    }

    override fun getItemCount(): Int = mensajes.size

    inner class MensajeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val tvAutor: TextView = view.findViewById(R.id.tvAutor)
    }
}
