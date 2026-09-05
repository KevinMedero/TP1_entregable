package com.example.tp1_entregable.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.tp1_entregable.R
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

// Modelo de datos para representar un contacto de emergencia
data class Contactos_emergencia(
    val id: String = "",
    val nombre: String = "",
    val numero: String = ""
)

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    // Lista reactiva que se actualizará cuando lleguen datos de Firebase
    val contactosEmergenciaList = remember { mutableStateListOf<Contactos_emergencia>() }

    // DisposableEffect para remover el listener cuando la pantalla se destruya
    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("contactos_emergencia")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    contactosEmergenciaList.clear()
                    for (doc in snapshot.documents) {
                        val contacto = Contactos_emergencia(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "Sin nombre",
                            numero = doc.getString("numero") ?: "Sin número"
                        )
                        contactosEmergenciaList.add(contacto)
                    }
                }
            }

        // Evita fugas de memoria y consumo innecesario de lecturas en Firestore en segundo plano
        onDispose {
            listenerRegistration.remove()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Números de Emergencia",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (contactosEmergenciaList.isEmpty()) {
            Text(text = "Cargando contactos de emergencia o la colección está vacía...")
        } else {
            LazyColumn {
                items(contactosEmergenciaList) { contacto ->
                    ContactoItem(contacto = contacto)
                }
            }
        }
    }
}

@Composable
fun ContactoItem(contacto: Contactos_emergencia) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contacto.nombre,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Número: ${contacto.numero}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón para iniciar la llamada
            IconButton(
                onClick = {
                    // Al presionar el botón de teléfono, se abre el marcador del sistema con el número cargado desde Firestore
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${contacto.numero}")
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = "Llamar a ${contacto.nombre}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}