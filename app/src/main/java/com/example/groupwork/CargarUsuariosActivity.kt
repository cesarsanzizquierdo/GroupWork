package com.example.groupwork

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CargarUsuariosActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnAgregar: Button
    private val usuarios = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cargar_usuarios)

        listView = findViewById(R.id.listViewUsuarios)
        btnAgregar = findViewById(R.id.btnAgregarUsuario)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, usuarios)
        listView.adapter = adapter

        btnAgregar.setOnClickListener {
            startActivity(Intent(this, AgregarUsuarioActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val nombreUsuario = usuarios[position]
            val intent = Intent(this, ChatsUsuarioActivity::class.java)
            intent.putExtra("nombreUsuario", nombreUsuario)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                usuarios.clear() // limpiar lista original
                for (doc in result) {
                    val nombre = doc.getString("nombre") ?: "Sin nombre"
                    usuarios.add(nombre)
                }
                adapter.notifyDataSetChanged()
            }
    }
}