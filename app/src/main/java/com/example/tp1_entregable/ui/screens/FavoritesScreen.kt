package com.example.tp1_entregable.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

// Modelo de datos simple para representar un contacto de emergencia
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

    // Escuchar la colección "contactos_emergencia" en tiempo real
    LaunchedEffect(Unit) {
        db.collection("contactos_emergencia")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    contactosEmergenciaList.clear()
                    for (doc in snapshot.documents) {
                        // Mapeamos el documento a nuestra clase Contactos_emergencia
                        // Asumimos que en Firebase tienes campos "nombre" y "numero"
                        val contacto = Contactos_emergencia(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "Sin nombre",
                            numero = doc.getString("numero") ?: "Sin numero"
                        )
                        contactosEmergenciaList.add(contacto)
                    }
                }
            }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Contactos de emergencia",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (contactosEmergenciaList.isEmpty()) {
            Text(text = "Cargando contactos de emergencia o la colección está vacía...")
        } else {
            LazyColumn {
                items(contactosEmergenciaList) { auto ->
                    ContactoItem(auto)
                }
            }
        }
    }
}

@Composable
fun ContactoItem(contacto: Contactos_emergencia) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Nombre: ${contacto.nombre}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Numero: ${contacto.numero}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}