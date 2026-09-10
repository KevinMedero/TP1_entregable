package com.example.tp1_entregable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tp1_entregable.R
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore

// Modelo de datos para representar un mensaje de chat
data class MensajeChat(
    val remitente: String = "", // "usuario" o "asistente"
    val mensaje: String = "",
    val fechaHora: Long = System.currentTimeMillis()  // Para ordenar cronológicamente los mensajes
)

// Pantalla principal del Chat de Asistencia usando Jetpack Compose
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val messagesList = remember { mutableStateListOf<MensajeChat>() }

    // Instancia y referencia a a la colleccion de Firestore
    val db = FirebaseFirestore.getInstance()
    val messagesCollection = db.collection("mensajes_chat")

    // Ciclo de vida: Configura un listener en tiempo real al abrir la pantalla y lo destruye al salir
    DisposableEffect(Unit) {
        val registro = messagesCollection
            .orderBy("fechaHora", Query.Direction.ASCENDING)    // Ordena los mensajes del más antiguo al más reciente
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    messagesList.clear()
                    // Itera sobre los documentos recuperados y los mapea al objeto MensajeChat
                    for (doc in snapshot.documents) {
                        try {
                            val mensajeObtenido = doc.toObject(MensajeChat::class.java)
                            if (mensajeObtenido != null) {
                                messagesList.add(mensajeObtenido)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChatScreen", "Error al leer documento ${doc.id}: ${e.message}")
                        }
                    }
                }
            }

        // Se ejecuta cuando el usuario sale del chat
        onDispose {
            registro.remove()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()    // Evita que la interfaz se solape con la barra de estado del sistema operativo
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Encabezado Superior
        Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Volver"
                    )
                }
                Text(
                    text = "Asistente de Emergencias",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Lista de Mensajes
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messagesList) { itemMensaje ->
                MessageBubble(mensajeObj = itemMensaje)
            }
        }

        // Campo de Texto y Botón Enviar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Escribe un mensaje...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    // Solo envía a la base de datos si el texto no está vacío
                    if (messageText.isNotBlank()) {
                        val docRef = messagesCollection.document()
                        val nuevoMensaje = MensajeChat(
                            remitente = "usuario",
                            mensaje = messageText.trim(),
                            fechaHora = System.currentTimeMillis()
                        )
                        docRef.set(nuevoMensaje)  // Sube el objeto serializado a Firestore
                        messageText = ""          // Limpia la caja de texto
                    }
                },
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Enviar")
            }
        }
    }
}

// Componente individual que representa gráficamente cada mensaje recibido de Firebase
@Composable
fun MessageBubble(mensajeObj: MensajeChat) {
    val isUsuario = mensajeObj.remitente == "usuario"
    val alignment = if (isUsuario) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUsuario) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Etiqueta del remitente
                Text(
                    text = if (isUsuario) "Tú" else "Representante",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                // Cuerpo del mensaje
                Text(
                    text = mensajeObj.mensaje,
                    fontSize = 14.sp
                )
            }
        }
    }
}