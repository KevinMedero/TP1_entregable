package com.example.tp1_entregable.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.tp1_entregable.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tp1_entregable.ui.utils.SOSEmergencyManager
import com.google.firebase.firestore.FirebaseFirestore

// Modelo de datos para representar un contacto de emergencia
data class ContactoEmergencia(
    val id: String = "",
    val nombre: String = "",
    val numero: String = ""
)

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Instancia de Firebase
    val db = FirebaseFirestore.getInstance()
    // Lista reactiva que se actualizará cuando lleguen datos de Firebase
    val contactosEmergenciaList = remember { mutableStateListOf<ContactoEmergencia>() }

    // DisposableEffect para remover el listener y liberar recursos cuando la pantalla se destruya
    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("contactos_emergencia")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    contactosEmergenciaList.clear()
                    for (doc in snapshot.documents) {
                        val contacto = ContactoEmergencia(
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

    // -----------------------------------------------------------------------------------------
    // PUNTO INNOVADOR: S.O.S
    // -----------------------------------------------------------------------------------------

    // Gestor de la lógica del silbato y flash S.O.S.
    val sosManager = remember { SOSEmergencyManager(context) }
    var isSosActive by remember { mutableStateOf(false) }


    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // -----------------------------------------------------------------------------------------
        // PARTE SUPERIOR: LISTA DE CONTACTOS DE EMERGENCIA (COMPOSE NATIVO)
        // -----------------------------------------------------------------------------------------
        Text(
            text = "Números de Emergencia",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // El peso .weight(1f) asegura que la lista tome el espacio disponible superior
        Box(modifier = Modifier.weight(1f)) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // -----------------------------------------------------------------------------------------
        // PARTE INFERIOR: BOTÓN S.O.S. DESDE XML
        // -----------------------------------------------------------------------------------------
        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.layout_favorites, null)

                // Listener del botón S.O.S. en el XML
                val btnSos = view.findViewById<Button>(R.id.btn_sos)
                btnSos?.setOnClickListener {
                    sosManager.toggleSos { newState ->
                        isSosActive = newState
                    }
                }

                view
            },
            update = { view ->
                // Actualiza el texto en tiempo real cuando cambia el estado del S.O.S.
                val btnSos = view.findViewById<Button>(R.id.btn_sos)
                btnSos?.text = if (isSosActive) "DETENER S.O.S." else "ACTIVAR S.O.S."
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ContactoItem(contacto: ContactoEmergencia) {
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