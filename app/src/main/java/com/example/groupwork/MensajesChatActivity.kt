package com.example.groupwork

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*

class MensajesChatActivity : AppCompatActivity() {

    private lateinit var tvNombreContacto: TextView
    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: Button
    private lateinit var btnAgregarParticipante: ImageButton // Nueva referencia al botón

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

        // Inicializamos las vistas
        tvNombreContacto = findViewById(R.id.tvNombreContacto)
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        btnAgregarParticipante = findViewById(R.id.btnAgregarParticipante) // Referencia al botón

        db = FirebaseFirestore.getInstance()

        chatId = intent.getStringExtra("chatId") ?: return
        nombreUsuario = intent.getStringExtra("nombreUsuario") ?: "Anónimo"

        // Inicializa RecyclerView
        adapter = MessageAdapter(listaMensajes, nombreUsuario)
        recyclerMensajes.layoutManager = LinearLayoutManager(this)
        recyclerMensajes.adapter = adapter

        obtenerNombreContacto()

        // Listener para enviar mensaje
        btnEnviar.setOnClickListener {
            val contenido = etMensaje.text.toString().trim()
            if (contenido.isNotEmpty()) {
                enviarMensaje(contenido)
                etMensaje.text.clear()
            }
        }

        // Listener para agregar un nuevo participante
        btnAgregarParticipante.setOnClickListener {
            mostrarDialogoAgregarParticipante()
        }

        // Escuchar nuevos mensajes
        escucharMensajes()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }

    // Método para enviar un mensaje
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

    // Método para escuchar mensajes nuevos
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

    // Método para obtener el nombre del contacto en el chat
    private fun obtenerNombreContacto() {
        db.collection("chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { doc ->
                val titulo = doc.getString("titulo") ?: "Chat"
                tvNombreContacto.text = titulo
            }
    }

    // Mostrar un diálogo para agregar un participante
    private fun mostrarDialogoAgregarParticipante() {
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { chatDoc ->
            val miembrosActuales = chatDoc.get("participantes") as? List<String> ?: listOf()

            db.collection("usuarios").get()
                .addOnSuccessListener { usuariosSnapshot ->
                    val usuariosDisponibles = mutableListOf<String>()
                    for (doc in usuariosSnapshot) {
                        val nombre = doc.getString("nombre") ?: continue
                        if (nombre !in miembrosActuales) {
                            usuariosDisponibles.add(nombre)
                        }
                    }

                    if (usuariosDisponibles.isEmpty()) {
                        Toast.makeText(this, "No hay usuarios disponibles", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val usuariosArray = usuariosDisponibles.toTypedArray()

                    AlertDialog.Builder(this)
                        .setTitle("Selecciona un usuario para agregar")
                        .setItems(usuariosArray) { _, which ->
                            val usuarioSeleccionado = usuariosArray[which]
                            añadirUsuarioAlChat(usuarioSeleccionado)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar chat", Toast.LENGTH_SHORT).show()
        }
    }

    // Añadir un usuario al chat
    private fun añadirUsuarioAlChat(usuario: String) {
        val chatRef = db.collection("chats").document(chatId)

        // Cambiamos "miembros" por "participantes"
        chatRef.update("participantes", FieldValue.arrayUnion(usuario))
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario añadido correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar usuario: $e", Toast.LENGTH_SHORT).show()
            }
    }

}
