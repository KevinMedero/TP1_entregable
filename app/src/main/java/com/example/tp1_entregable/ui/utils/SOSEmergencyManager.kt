package com.example.tp1_entregable.ui.utils

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

// Clase gestora para emitir la señal internacional de socorro S.O.S. mediante el flash y un tono de audio sincronizados
class SOSEmergencyManager(private val context: Context) {

    // Servicio para el flash de la camara
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    // Reproductor de audio PCM para emitir el silbato de alta frecuencia
    private var audioTrack: AudioTrack? = null

    // Corrutina que controla los tiempos de pausa y emisión del código Morse en segundo plano
    private var job: Job? = null

    // Estado de ejecución de la alerta S.O.S
    var isRunning = false
        private set

    init {
        try {
            // Identifica el ID de la cámara trasera para el flash
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Prepara la sintetización del tono de audio en memoria
        setupAudioTrack()
    }

    // Genera una onda senoidal pura (3000 Hz) en buffer estático configurado en bucle infinito
    private fun setupAudioTrack() {
        val sampleRate = 44100
        val frequency = 3000.0 // // Frecuencia en Hz para simular un silbato agudo de socorro
        val numSamples = sampleRate // 1 segundo de audio en buffer
        val buffer = ShortArray(numSamples)

        // Sintetiza matemáticamente los valores de la onda senoidal en PCM 16-bit
        for (i in 0 until numSamples) {
            val sample = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequency))
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        val bufferSize = numSamples * 2 // 16 bits = 2 bytes por muestra

        // Construye el AudioTrack configurando el uso como alarma para máximo volumen
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC) // Carga estática: mantiene el buffer cargado en hardware para bucle infinito
            .build()

        // Escribe la onda generada en el buffer del reproductor
        audioTrack?.write(buffer, 0, buffer.size)

        // Asigna el bucle infinito (-1) cubriendo todas las muestras del buffer
        audioTrack?.setLoopPoints(0, numSamples, -1)

        // Inicializa el volumen en silencio (0.0f) para habilitarlo solo durante los pulsos de la señal
        audioTrack?.setVolume(0.0f)
    }

    // Alterna el estado de activación de la señal S.O.S. (Inicia / Detiene)
    fun toggleSos(onStateChanged: (Boolean) -> Unit) {
        if (isRunning) {
            stopSos()
            onStateChanged(false)
        } else {
            startSos()
            onStateChanged(true)
        }
    }

    // Inicia la secuencia de pulsos Morse en una corrutina asíncrona
    private fun startSos() {
        if (isRunning) return
        isRunning = true

        // Inicia la reproducción continua del bucle en segundo plano
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Ejecuta el patrón de tiempos S.O.S. en un hilo secundario sin congelar la UI
        job = CoroutineScope(Dispatchers.Default).launch {
            // Estándar de tiempos Morse (milisegundos)
            val dot = 150L          // Punto (.)
            val dash = 450L         // Raya (-)
            val elementGap = 150L   // Pausa entre elementos del mismo carácter
            val letterGap = 450L    // Pausa entre letras (S - O - S)
            val wordGap = 1500L     // Pausa de silencio al completar la palabra S.O.S

            audioTrack?.play()

            while (isActive && isRunning) {
                // S (...) - Tres pulsos cortos
                emitSignal(dot, elementGap)
                emitSignal(dot, elementGap)
                emitSignal(dot, letterGap)

                // O (---) - Tres pulsos largos
                emitSignal(dash, elementGap)
                emitSignal(dash, elementGap)
                emitSignal(dash, letterGap)

                // S (...) - Tres pulsos cortos
                emitSignal(dot, elementGap)
                emitSignal(dot, elementGap)
                emitSignal(dot, wordGap)
            }
        }
    }

    // Emite un pulso sincronizado activando hardware (flash + audio) y esperando la duración/pausa asignada
    private suspend fun emitSignal(durationMs: Long, pauseMs: Long) {
        setOutputsState(true)           // Enciende Flash y Sonido
        delay(durationMs)    // Mantiene encendido
        setOutputsState(false)          // Apaga Flash y Sonido
        delay(pauseMs)       // Tiempo de silencio/oscuridad entre pulsos
    }

    // Sincroniza el estado del flash de la cámara y el volumen del tono audible
    private fun setOutputsState(enabled: Boolean) {
        // Enciende/Apaga el Flash
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, enabled)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sube/Baja volumen del audio silbato
        try {
            if (enabled) {
                audioTrack?.setVolume(1.0f)
            } else {
                audioTrack?.setVolume(0.0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cancela el bucle, apaga las salidas físicas y frena la corrutina activa
    fun stopSos() {
        isRunning = false
        job?.cancel()
        setOutputsState(false)
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Libera los recursos de hardware del sistema al salir de la pantalla para evitar fugas de memoria
    fun release() {
        stopSos()
        audioTrack?.release()
        audioTrack = null
    }
}