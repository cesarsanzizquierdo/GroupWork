package com.example.groupwork

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*

class MensajesChatActivity : AppCompatActivity() {

    private lateinit var tvNombreContacto: TextView
    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: Button

    private lateinit var adapter: MessageAdapter
    private val listaMensajes = mutableListOf<Mensaje>()

    private lateinit var db: FirebaseFirestore
    private lateinit var chatId: String
    private lateinit var nombreUsuario: String
    private lateinit var nombreContacto: String

    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mensajes_chat)

        tvNombreContacto = findViewById(R.id.tvNombreContacto)
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)

        db = FirebaseFirestore.getInstance()

        chatId = intent.getStringExtra("chatId") ?: return
        nombreUsuario = intent.getStringExtra("nombreUsuario") ?: "Anónimo"

        // Inicializa RecyclerView
        adapter = MessageAdapter(listaMensajes, nombreUsuario)
        recyclerMensajes.layoutManager = LinearLayoutManager(this)
        recyclerMensajes.adapter = adapter

        obtenerNombreContacto()

        btnEnviar.setOnClickListener {
            val contenido = etMensaje.text.toString().trim()
            if (contenido.isNotEmpty()) {
                enviarMensaje(contenido)
                etMensaje.text.clear()
            }
        }

        escucharMensajes()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }

    private fun enviarMensaje(contenido: String) {
        val mensaje = hashMapOf(
            "autor" to nombreUsuario,
            "contenido" to contenido,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .add(mensaje)
    }

    private fun escucharMensajes() {
        listener = db.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                listaMensajes.clear()
                for (doc in snapshots) {
                    val mensaje = doc.toObject(Mensaje::class.java)
                    listaMensajes.add(mensaje)
                }
                adapter.notifyDataSetChanged()
                recyclerMensajes.scrollToPosition(listaMensajes.size - 1)
            }
    }

    private fun obtenerNombreContacto() {
        // Obtener el nombre del otro usuario en el chat
        db.collection("chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { doc ->
                val participantes = doc.get("participantes") as? ArrayList<String> ?: arrayListOf()
                nombreContacto = participantes.firstOrNull { it != nombreUsuario } as? String ?: "Contacto"
                tvNombreContacto.text = "Chat con $nombreContacto"
            }
    }
}