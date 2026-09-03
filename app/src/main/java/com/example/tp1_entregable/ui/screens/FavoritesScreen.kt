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

// Modelo de datos simple para representar un Auto
data class Auto(
    val id: String = "",
    val marca: String = ""
)

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    // Lista reactiva que se actualizará cuando lleguen datos de Firebase
    val autosList = remember { mutableStateListOf<Auto>() }

    // Escuchar la colección "autos" en tiempo real
    LaunchedEffect(Unit) {
        db.collection("autos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    autosList.clear()
                    for (doc in snapshot.documents) {
                        // Mapeamos el documento a nuestra clase Auto
                        // Asumimos que en Firebase tienes campos "nombre" y "marca"
                        val auto = Auto(
                            id = doc.id,
                            marca = doc.getString("marca") ?: "Sin marca"
                        )
                        autosList.add(auto)
                    }
                }
            }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Mis Autos Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (autosList.isEmpty()) {
            Text(text = "Cargando autos o la colección está vacía...")
        } else {
            LazyColumn {
                items(autosList) { auto ->
                    AutoItem(auto)
                }
            }
        }
    }
}

@Composable
fun AutoItem(auto: Auto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Marca: ${auto.marca}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}