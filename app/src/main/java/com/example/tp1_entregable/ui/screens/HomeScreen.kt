package com.example.tp1_entregable.ui.screens

import android.util.Log
import android.view.LayoutInflater
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.tp1_entregable.R
import com.example.tp1_entregable.ui.utils.EmergencyAlertManager
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val emergencyManager = remember { EmergencyAlertManager(context) }

    // Estado para activar el parpadeo de luces rojas en pantalla
    var isEmergencyActive by remember { mutableStateOf(false) }

    // Color intermitente (rojo/negro) para la pantalla
    val infiniteTransition = rememberInfiniteTransition(label = "EmergencyFlash")
    val flashColor by infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Black,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorAnimation"
    )

    // Obtiene Fecha (YYYY-MM-DD)
    val currentDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Ubicación de la app
    val userLocation = "General Pico, La Pampa"

    // Escucha en tiempo real la colección "catastrofes" de Firebase
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        // Se crea un Handler para revisar la hora cada 5 segundos
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkRunnable: Runnable? = null

        val listenerRegistration = db.collection("catastrofes")
            .whereEqualTo("fecha", currentDate)
            .whereEqualTo("ubicacion", userLocation)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Catastrofe", "Error al escuchar eventos: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Remueve chequeos anteriores si los hubiera
                    checkRunnable?.let { handler.removeCallbacks(it) }

                    // Define la tarea que verifica si ya llegó la hora
                    checkRunnable = object : Runnable {
                        override fun run() {
                            val nowHour = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                            for (doc in snapshot.documents) {
                                val eventHour = doc.getString("hora")

                                // Compara si la hora actual ya alcanzó o superó la hora del evento
                                if (eventHour != null && nowHour == eventHour) {
                                    isEmergencyActive = true
                                    emergencyManager.triggerAlert()
                                    return // Se detiene la verificación si ya se activó
                                }
                            }

                            // Si aún no es la hora, vuelve a verificar en 5 segundos
                            if (!isEmergencyActive) {
                                handler.postDelayed(this, 5000)
                            }
                        }
                    }

                    // Ejecuta la primera verificación
                    handler.post(checkRunnable!!)
                }
            }

        onDispose {
            checkRunnable?.let { handler.removeCallbacks(it) }
            listenerRegistration.remove()
            emergencyManager.stopAlert()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Tu interfaz original (AndroidView, etc.)
        AndroidView(
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.layout_home, null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Interfaz de pantalla entera con efecto de luces si hay emergencia
        if (isEmergencyActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(flashColor.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¡ALERTA DE CATASTROFE!",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isEmergencyActive = false
                            emergencyManager.stopAlert()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "DESACTIVAR ALERTA", color = Color.Red)
                    }
                }
            }
        }
    }
}