package com.example.groupwork
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ChatsUsuarioActivity : AppCompatActivity() {

    private lateinit var tvTitulo: TextView
    private lateinit var listViewChats: ListView
    private lateinit var btnAgregarChat: Button
    private lateinit var adapter: ArrayAdapter<String>
    private val chats = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_usuario)

        tvTitulo = findViewById(R.id.tvTitulo)
        listViewChats = findViewById(R.id.listViewChats)
        btnAgregarChat = findViewById(R.id.btnAgregarChat)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chats)
        listViewChats.adapter = adapter

        val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: "Desconocido"
        tvTitulo.text = "Chats de $nombreUsuario"

        btnAgregarChat.setOnClickListener {
            mostrarDialogoAgregarChat(nombreUsuario)
        }

        cargarChats(nombreUsuario)
    }

    private fun cargarChats(nombreUsuario: String) {
        chats.clear()
        db.collection("chats")
            .whereArrayContains("participantes", nombreUsuario)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val titulo = doc.getString("titulo") ?: "Chat sin título"
                    chats.add(titulo)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar chats", Toast.LENGTH_SHORT).show()
            }
        listViewChats.setOnItemClickListener { _, _, position, _ ->
            val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: return@setOnItemClickListener
            val titulo = chats[position]

            // Obtener el ID del chat (más robusto: deberías usar un mapa de ID → título)
            db.collection("chats")
                .whereEqualTo("titulo", titulo)
                .get()
                .addOnSuccessListener { result ->
                    val chatId = result.documents.firstOrNull()?.id ?: return@addOnSuccessListener

                    val intent = Intent(this, MensajesChatActivity::class.java)
                    intent.putExtra("chatId", chatId)
                    intent.putExtra("nombreUsuario", nombreUsuario)
                    startActivity(intent)
                }
        }
    }

    private fun mostrarDialogoAgregarChat(nombreUsuario: String) {
        val usuariosDisponibles = mutableListOf<String>()

        db.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val nombre = doc.getString("nombre") ?: continue
                    if (nombre != nombreUsuario) {
                        usuariosDisponibles.add(nombre)
                    }
                }

                if (usuariosDisponibles.isEmpty()) {
                    Toast.makeText(this, "No hay otros usuarios disponibles", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val usuariosArray = usuariosDisponibles.toTypedArray()
                var seleccionado: String? = null

                AlertDialog.Builder(this)
                    .setTitle("Selecciona un usuario")
                    .setSingleChoiceItems(usuariosArray, -1) { _, which ->
                        seleccionado = usuariosArray[which]
                    }
                    .setPositiveButton("Siguiente") { _, _ ->
                        if (seleccionado != null) {
                            pedirNombreChat(nombreUsuario, seleccionado!!)
                        } else {
                            Toast.makeText(this, "Debes seleccionar un usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pedirNombreChat(usuario1: String, usuario2: String) {
        val input = EditText(this)
        input.hint = "Nombre del chat"

        AlertDialog.Builder(this)
            .setTitle("Nombre del chat")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val titulo = input.text.toString().trim()
                if (titulo.isNotEmpty()) {
                    val nuevoChat = hashMapOf(
                        "titulo" to titulo,
                        "participantes" to listOf(usuario1, usuario2),
                        "ultimoMensaje" to "",
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("chats")
                        .add(nuevoChat)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Chat creado", Toast.LENGTH_SHORT).show()
                            cargarChats(usuario1)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al crear el chat", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
