package com.example.tp1_entregable.ui.utils

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

class SOSEmergencyManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    var isRunning = false
        private set

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setupAudioTrack()
    }

    // Configura un generador de tono senoidal (AudioTrack estático) agudo de alta frecuencia (3000 Hz) en bucle infinito
    private fun setupAudioTrack() {
        val sampleRate = 44100
        val frequency = 3000.0 // Tono agudo tipo silbato de socorro
        val numSamples = sampleRate // 1 segundo de audio en buffer
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val sample = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequency))
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        val bufferSize = numSamples * 2 // 16 bits = 2 bytes por muestra

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
            .setTransferMode(AudioTrack.MODE_STATIC) // Carga estática para bucle continuo
            .build()

        // Escribe el tono en el buffer
        audioTrack?.write(buffer, 0, buffer.size)
        // Configura el bucle infinito sobre todo el buffer
        audioTrack?.setLoopPoints(0, numSamples, -1)
        // Inicia en silencio
        audioTrack?.setVolume(0.0f)
    }

    // Inicia o Detiene el ciclo S.O.S.
    fun toggleSos(onStateChanged: (Boolean) -> Unit) {
        if (isRunning) {
            stopSos()
            onStateChanged(false)
        } else {
            startSos()
            onStateChanged(true)
        }
    }

    private fun startSos() {
        if (isRunning) return
        isRunning = true

        // Comienza la reproducción continua del bucle
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        job = CoroutineScope(Dispatchers.Default).launch {
            // Tiempos Morse en milisegundos
            val dot = 150L
            val dash = 450L
            val elementGap = 150L
            val letterGap = 450L
            val wordGap = 1500L

            audioTrack?.play()

            while (isActive && isRunning) {
                // S (...)
                emitSignal(dot, elementGap)
                emitSignal(dot, elementGap)
                emitSignal(dot, letterGap)

                // O (---)
                emitSignal(dash, elementGap)
                emitSignal(dash, elementGap)
                emitSignal(dash, letterGap)

                // S (...)
                emitSignal(dot, elementGap)
                emitSignal(dot, elementGap)
                emitSignal(dot, wordGap)
            }
        }
    }

    private suspend fun emitSignal(durationMs: Long, pauseMs: Long) {
        setOutputsState(true)
        delay(durationMs)
        setOutputsState(false)
        delay(pauseMs)
    }

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

    fun release() {
        stopSos()
        audioTrack?.release()
        audioTrack = null
    }
}