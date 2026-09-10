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

// Modelo de datos para representar un contacto de emergencia guardado en Firebase
data class ContactoEmergencia(
    val id: String = "",
    val nombre: String = "",
    val numero: String = ""
)

// Pantalla de emergencia que combina lista nativa en Compose y vista inflada desde XML para el S.O.S.
@Composable
fun EmergenciaScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Instancia de Firestore para consultar la colección de contactos
    val db = FirebaseFirestore.getInstance()

    // Lista reactiva que se actualizará cuando lleguen datos de Firebase
    val contactosEmergenciaList = remember { mutableStateListOf<ContactoEmergencia>() }

    // Ciclo de vida: suscripción en tiempo real a la colección "contactos_emergencia" en Firestore
    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("contactos_emergencia")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    contactosEmergenciaList.clear()
                    // Mapea cada documento recibido de la base de datos a una instancia de ContactoEmergencia
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

        // Se ejecuta al salir de la pantalla: destruye el listener de Firestore y libera los recursos de hardware
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
                    // Lista de desplazamiento eficiente para renderizar los elementos
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
                val view = LayoutInflater.from(context).inflate(R.layout.layout_emergencia, null)

                // Obtiene la referencia al botón S.O.S. y le asigna el listener de activación
                val btnSos = view.findViewById<Button>(R.id.btn_sos)
                btnSos?.setOnClickListener {
                    sosManager.toggleSos { newState ->
                        isSosActive = newState  // Actualiza el estado reactivo
                    }
                }

                view
            },
            // Bloque de actualización: Se vuelve a ejecutar cuando la variable isSosActive cambia
            update = { view ->
                // Actualiza el texto en tiempo real cuando cambia el estado del S.O.S.
                val btnSos = view.findViewById<Button>(R.id.btn_sos)
                btnSos?.text = if (isSosActive) "DETENER S.O.S." else "ACTIVAR S.O.S."
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Componente individual que renderiza la tarjeta de información y el botón de llamada directa para cada contacto
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
            // Columna con los datos de texto del contacto
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

            // Botón de acción para abrir la aplicación del marcador telefónico del sistema
            IconButton(
                onClick = {
                    // Crea un Intent implícito para abrir la aplicación de llamadas con el número precargado
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