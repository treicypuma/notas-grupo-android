package com.grupo.notas

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotasScreen()
                }
            }
        }
    }
}

@Composable
fun NotasScreen() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var estado by remember {
        mutableStateOf(
            auth.currentUser?.let { "Sesión: ${it.email}" } ?: "Sin sesión"
        )
    }
    var lista by remember { mutableStateOf("") }

    fun msg(t: String) = Toast.makeText(context, t, Toast.LENGTH_LONG).show()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Notas del grupo", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mín. 6)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Registro
            Button(onClick = {
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        val uid = result.user!!.uid
                        db.collection("usuarios").document(uid).set(
                            mapOf(
                                "email" to email.trim(),
                                "fechaRegistro" to FieldValue.serverTimestamp()
                            )
                        )
                        estado = "Sesión: ${result.user?.email}"
                        msg("Registrado correctamente")
                    }
                    .addOnFailureListener { msg(it.message ?: "Error al registrar") }
            }) { Text("Registrarse") }

            // Login
            Button(onClick = {
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        estado = "Sesión: ${result.user?.email}"
                        msg("Sesión iniciada")
                    }
                    .addOnFailureListener { msg(it.message ?: "Error al iniciar sesión") }
            }) { Text("Iniciar sesión") }
        }

        Text(estado, fontWeight = FontWeight.Bold)

        Divider()

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = contenido,
            onValueChange = { contenido = it },
            label = { Text("Contenido") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Guardar nota en usuarios/{uid}/notas/{notaId}
            Button(onClick = {
                val user = auth.currentUser
                if (user == null) {
                    msg("Inicia sesión primero")
                } else {
                    val nota = hashMapOf(
                        "titulo" to titulo,
                        "contenido" to contenido,
                        "autorUid" to user.uid,
                        "fechaCreacion" to FieldValue.serverTimestamp(),
                        "compartida" to false
                    )
                    db.collection("usuarios").document(user.uid)
                        .collection("notas").add(nota)
                        .addOnSuccessListener {
                            titulo = ""
                            contenido = ""
                            msg("Nota guardada")
                        }
                        .addOnFailureListener { msg(it.message ?: "Error al guardar") }
                }
            }) { Text("Guardar nota") }

            // Leer notas
            Button(onClick = {
                val user = auth.currentUser
                if (user == null) {
                    msg("Inicia sesión primero")
                } else {
                    db.collection("usuarios").document(user.uid)
                        .collection("notas").get()
                        .addOnSuccessListener { snap ->
                            lista = if (snap.isEmpty) {
                                "No hay notas todavía"
                            } else {
                                snap.joinToString("\n\n") {
                                    "• ${it.getString("titulo")}: ${it.getString("contenido")}"
                                }
                            }
                        }
                        .addOnFailureListener { msg(it.message ?: "Error al leer") }
                }
            }) { Text("Leer mis notas") }
        }

        Text(lista)
    }
}