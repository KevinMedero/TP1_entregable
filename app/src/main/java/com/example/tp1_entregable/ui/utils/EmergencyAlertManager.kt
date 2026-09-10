package com.example.tp1_entregable.ui.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*

// Clase auxiliar para gestionar la emisión de alertas sonoras y de vibración en situaciones de catástrofe/emergencia
class EmergencyAlertManager(private val context: Context) {

    // Generador de tonos y de sistema para alertas sonoras rápidas
    private var toneGenerator: ToneGenerator? = null

    // Para la vibracion
    private var vibrator: Vibrator? = null
    private var vibratorManager: VibratorManager? = null

    // Para el sonido irritante / alarma
    private var soundJob: Job? = null

    // Dispara la secuencia de alarma acústica e intermitencia de vibración en el dispositivo
    fun triggerAlert() {
        // Cancela alertas previas si ya estaban activas
        stopAlert()

        // Sonido Irritante / Alarma
        soundJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                while (isActive) {
                    // Reproduce un bloque de 5 segundos
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 5000)
                    delay(5000) // Espera a que finalice para volver a dispararlo
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Patron de vibracion en bucle infinito
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)

        // Vibración adaptado a la versión del sistema Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Para Android 12+ (API 31+): Uso del nuevo servicio VibratorManager
            vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), 0)
            vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
        }
        else {
            // Para Android 11 o inferior: Uso del servicio clásico Vibrator
            @Suppress("DEPRECATION")
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26+: Uso de VibrationEffect con patrón de onda
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    // API < 26: Invocación legacy directa pasando patrón e índice de repetición
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        }
    }

    // Interrumpe tanto el sonido como la vibración activa
    fun stopAlert() {
        // Cancela el bucle de sonido y liberar recursos de audio
        soundJob?.cancel()
        soundJob = null
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        toneGenerator = null

        // Cancelar la vibración activa en el hardware
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vibratorManager?.cancel()
            } else {
                @Suppress("DEPRECATION")
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}