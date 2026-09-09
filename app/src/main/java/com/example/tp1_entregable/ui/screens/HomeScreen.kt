package com.example.tp1_entregable.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.tp1_entregable.R
import com.example.tp1_entregable.ui.utils.CalculateBatteryUseCase
import com.example.tp1_entregable.ui.utils.AudioRecorderManager
import com.example.tp1_entregable.ui.utils.VideoCaptureManager
import com.example.tp1_entregable.ui.utils.VoiceCommandManager
import com.example.tp1_entregable.ui.utils.EmergencyAlertManager
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // -----------------------------------------------------------------------------------------------------
    // LINTERNA
    // -----------------------------------------------------------------------------------------------------

    // Estado de la linterna
    var isFlashOn by remember { mutableStateOf(false) }

    // Servicio del sistema para la linterna
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // Inicializa CameraManager - Obtiene el ID de la cámara con flash de forma segura
    val cameraId = remember(cameraManager) {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    // function que controla ambos estados (encendido/apagado)
    fun setFlashState(turnOn: Boolean) {
        cameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, turnOn)
                isFlashOn = turnOn
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Ciclo de vida y apagado automático de la linterna al salir de la app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (isFlashOn) setFlashState(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            if (isFlashOn) setFlashState(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // COMANDO POR VOZ: LINTERNA
    // -----------------------------------------------------------------------------------------------------

    // Lee el valor real del flash en el momento en que habla y no la versión que había cuando se inició la pantalla.
    val currentIsFlashOn by rememberUpdatedState(isFlashOn)

    // Gestor de voz
    val voiceCommandManager = remember {
        VoiceCommandManager(context) { comando ->
            when {
                comando.contains("flash") || comando.contains("linterna") -> {
                    // Se invoca la lógica para encender/apagar la linterna
                    Log.d("VoiceAction", "Ejecutando acción: Linterna")
                    setFlashState(!currentIsFlashOn)
                }
            }
        }
    }

    // Solicitud de permiso en tiempo de ejecución
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceCommandManager.startListening()
        }
    }

    // Limpieza de recursos del microfono / comando de voz al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            voiceCommandManager.destroy()
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // GRABAR VIDEO (FRONTAL Y SELFI) Y AUDIO POR MIC
    // -----------------------------------------------------------------------------------------------------

    // Estados de grabbing
    var isRecordingTrasera by remember { mutableStateOf(false) }
    var isRecordingSelfie by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }

    // Gestores
    val videoManager = remember { VideoCaptureManager(context) }
    val audioManager = remember { AudioRecorderManager(context) }
    val previewView = remember { PreviewView(context) }

    // La linterna (CameraManager) funcionará desde el primer momento porque la cámara estará libre. Ya
    // que genera conflictos con inicializar CameraX tambien (codigo que tengo comentado).
    /*
    // Inicializar la cámara por defecto (trasera)
    LaunchedEffect(Unit) {
        videoManager.setupCamera(
            lifecycleOwner = lifecycleOwner,
            useFrontCamera = false,
            surfaceProvider = previewView.surfaceProvider
        )
    }
     */

    // -----------------------------------------------------------------------------------------------------
    // ALERTA DE EMERGENCIA
    // -----------------------------------------------------------------------------------------------------

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
                            val nowHour =
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

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

    // -----------------------------------------------------------------------------------------------------
    // DIRECCION A OTRAS SCREENS: CHAT CON ASISTENCIA Y GUIA
    // -----------------------------------------------------------------------------------------------------

    var showChatScreen by remember { mutableStateOf(false) }
    var showGuiaScreen by remember { mutableStateOf(false) }

    // Se dibuja el Chat o la Guía ocupando el espacio de HomeScreen,
    // pero la barra de navegacion de MainActivity sigue visible debajo.
    when {
        showChatScreen -> {
            ChatScreen(
                onBackClick = { showChatScreen = false }
            )
        }
        showGuiaScreen -> {
            GuiaScreen(
                onBackClick = { showGuiaScreen = false }
            )
        } else -> {
            Box(modifier = modifier.fillMaxSize()) {
                // Mantiene la vista de previsualización activa pero transparente/diminuta
                // para que CameraX continúe produciendo fotogramas sin fallar con Error 8
                AndroidView(
                    factory = { previewView.apply { alpha = 0.01f } },
                    modifier = Modifier.size(1.dp)
                )
                // Interfaz original XML
                AndroidView(
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_home, null)

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON PARA EL CHAT DE ASISTENCIA
                        // -----------------------------------------------------------------------------------------------------

                        // Botón para el Chat con Asistencia
                        val btnChat = view.findViewById<Button>(R.id.btn_chat)
                        btnChat?.setOnClickListener {
                            showChatScreen = true
                        }

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON DE MICROFONO PARA COMANDO POR VOZ: LINTERNA
                        // -----------------------------------------------------------------------------------------------------

                        // Botón de micrófono para activar la escucha por voz
                        val btnMic: Button? = view.findViewById(R.id.btn_mic)
                        btnMic?.setOnClickListener {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }

                        // -----------------------------------------------------------------------------------------------------
                        // WIDGET DEL CLIMA
                        // -----------------------------------------------------------------------------------------------------

                        // Carga el WebView del Widget del Clima desde assets
                        val webView: android.webkit.WebView? = view.findViewById(R.id.wv_weather_widget)
                        webView?.apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)

                            // Carga el archivo desde la carpeta assets
                            loadUrl("file:///android_asset/weather_widget.html")
                        }

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON LINTERNA
                        // -----------------------------------------------------------------------------------------------------

                        // Boton flash
                        val botonFlash: Button? = view.findViewById(R.id.btn_flash)
                        // Alterna el estado estoppel en cad click
                        botonFlash?.setOnClickListener {
                            val nextState = !isFlashOn
                            setFlashState(nextState)
                            Toast.makeText(
                                context,
                                if (nextState) "Encendido" else "Apagado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON DE GUIA
                        // -----------------------------------------------------------------------------------------------------

                        val btnGuion = view.findViewById<Button>(R.id.btn_guia)
                        btnGuion?.setOnClickListener {
                            showGuiaScreen = true
                        }

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON GRABAR VIDEO (TRASERA Y SELFI) Y AUDIO POR MICROFONO
                        // -----------------------------------------------------------------------------------------------------

                        // Grabar Video Cámara Trasera
                        val btnTrasera: Button? = view.findViewById(R.id.btn_video_trasera)
                        btnTrasera?.setOnClickListener {
                            if (isRecordingSelfie || isRecordingAudio) {
                                Toast.makeText(
                                    context,
                                    "Detenga la otra grabación primero",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            if (isRecordingTrasera) {
                                videoManager.startRecording("TRASERA") { state ->
                                    isRecordingTrasera = state
                                }
                            } else {
                                videoManager.setupCamera(
                                    lifecycleOwner,
                                    useFrontCamera = false,
                                    previewView.surfaceProvider
                                ) {
                                    videoManager.startRecording("TRASERA") { state ->
                                        isRecordingTrasera = state
                                    }
                                }
                            }
                        }

                        // Grabar Video Cámara Selfie
                        val btnSelfie: Button? = view.findViewById(R.id.btn_video_selfie)
                        btnSelfie?.setOnClickListener {
                            if (isRecordingTrasera || isRecordingAudio) {
                                Toast.makeText(
                                    context,
                                    "Detenga la otra grabación primero",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            if (isRecordingSelfie) {
                                videoManager.startRecording("SELFIE") { state ->
                                    isRecordingSelfie = state
                                }
                            } else {
                                videoManager.setupCamera(
                                    lifecycleOwner,
                                    useFrontCamera = true,
                                    previewView.surfaceProvider
                                ) {
                                    videoManager.startRecording("SELFIE") { state ->
                                        isRecordingSelfie = state
                                    }
                                }
                            }
                        }

                        // Grabar Audio con Micrófono
                        val btnAudio: Button? = view.findViewById(R.id.btn_grabar_audio)
                        btnAudio?.setOnClickListener {
                            if (isRecordingTrasera || isRecordingSelfie) {
                                Toast.makeText(
                                    context,
                                    "Detenga la grabación de video primero",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            if (isRecordingAudio) {
                                audioManager.stopRecording { state -> isRecordingAudio = state }
                            } else {
                                audioManager.startRecording { state -> isRecordingAudio = state }
                            }
                        }

                        view
                    },

                    // Actualizaciones de estado en tiempo de ejecución
                    update = { view ->
                        // -----------------------------------------------------------------------------------------------------
                        // BATERIA
                        // -----------------------------------------------------------------------------------------------------

                        // Sincronizacion y calculo de la Batería
                        val calculateBatteryUseCase = CalculateBatteryUseCase()
                        val batteryInfo = calculateBatteryUseCase(context)

                        // Sincroniza el TextView de la batería por su ID y le asigno el texto
                        val tvBatteryBox: TextView? = view.findViewById(R.id.tv_battery_box)
                        tvBatteryBox?.text = "Batería: ${batteryInfo.percentage}%\n" +
                                "Duración estimada: ${batteryInfo.hoursRemainingText}\n" +
                                "Se agotará a las: ${batteryInfo.formattedTimeToEmpty}"

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON LINTERNA
                        // -----------------------------------------------------------------------------------------------------

                        // Sincronizacion el texto del Botón de la Linterna
                        val btnFlash: Button? = view.findViewById(R.id.btn_flash)
                        btnFlash?.text = if (isFlashOn) "LINTERNA: ENCENDIDA" else "LINTERNA: APAGADA"

                        // -----------------------------------------------------------------------------------------------------
                        // BOTON GRABAR VIDEO (FRONTAL/TRASERA Y SELFI) Y AUDIO POR MICROFONO
                        // -----------------------------------------------------------------------------------------------------

                        // Actualización de textos en tiempo real según el estado
                        val btnFrontal: Button? = view.findViewById(R.id.btn_video_trasera)
                        btnFrontal?.text =
                            if (isRecordingTrasera) "DETENER VIDEO TRASERA" else "VIDEO TRASERA"

                        val btnSelfie: Button? = view.findViewById(R.id.btn_video_selfie)
                        btnSelfie?.text =
                            if (isRecordingSelfie) "DETENER VIDEO SELFIE" else "VIDEO SELFIE"

                        val btnAudio: Button? = view.findViewById(R.id.btn_grabar_audio)
                        btnAudio?.text = if (isRecordingAudio) "DETENER AUDIO" else "AUDIO"
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
    }
}