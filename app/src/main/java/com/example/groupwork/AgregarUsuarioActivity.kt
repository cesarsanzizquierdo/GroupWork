package com.example.groupwork

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AgregarUsuarioActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var btnGuardar: Button
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_usuario)

        etNombre = findViewById(R.id.etNombre)
        btnGuardar = findViewById(R.id.btnGuardar)

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()

            if (nombre.isNotEmpty()) {
                val nuevoUsuario = hashMapOf("nombre" to nombre)
                db.collection("usuarios")
                    .add(nuevoUsuario)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Usuario agregado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Introduce un nombre", Toast.LENGTH_SHORT).show()
            }
        }
    }
}