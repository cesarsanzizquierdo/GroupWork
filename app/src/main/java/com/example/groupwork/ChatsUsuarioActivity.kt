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
    private lateinit var nombreUsuario: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_usuario)

        tvTitulo = findViewById(R.id.tvTitulo)
        listViewChats = findViewById(R.id.listViewChats)
        btnAgregarChat = findViewById(R.id.btnAgregarChat)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chats)
        listViewChats.adapter = adapter

        nombreUsuario = intent.getStringExtra("nombreUsuario") ?: "Desconocido"
        tvTitulo.text = "Chats de $nombreUsuario"

        btnAgregarChat.setOnClickListener {
            mostrarDialogoAgregarChat()
        }

        cargarChats()
    }

    private fun cargarChats() {
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
            val titulo = chats[position]
            db.collection("chats")
                .whereEqualTo("titulo", titulo)
                .whereArrayContains("participantes", nombreUsuario)
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

    private fun mostrarDialogoAgregarChat() {
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
                val seleccionados = mutableListOf<String>()
                val seleccionadosBool = BooleanArray(usuariosArray.size)

                AlertDialog.Builder(this)
                    .setTitle("Selecciona usuarios")
                    .setMultiChoiceItems(usuariosArray, seleccionadosBool) { _, which, isChecked ->
                        if (isChecked) {
                            seleccionados.add(usuariosArray[which])
                        } else {
                            seleccionados.remove(usuariosArray[which])
                        }
                    }
                    .setPositiveButton("Siguiente") { _, _ ->
                        if (seleccionados.isNotEmpty()) {
                            seleccionados.add(nombreUsuario)
                            pedirNombreChat(seleccionados)
                        } else {
                            Toast.makeText(this, "Debes seleccionar al menos un usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pedirNombreChat(participantes: List<String>) {
        val input = EditText(this)
        input.hint = "Nombre del chat"

        AlertDialog.Builder(this)
            .setTitle("Nombre del chat grupal")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val titulo = input.text.toString().trim()
                if (titulo.isNotEmpty()) {
                    val nuevoChat = hashMapOf(
                        "titulo" to titulo,
                        "participantes" to participantes,
                        "ultimoMensaje" to "",
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("chats")
                        .add(nuevoChat)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Chat creado", Toast.LENGTH_SHORT).show()
                            cargarChats()
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
