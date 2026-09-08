package com.example.tp1_entregable.ui.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class EmergencyAlertManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    fun triggerAlert() {
        // Sonido Irritante / Alarma
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 3000) // 3 segundos
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibración
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val pattern = VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            vibratorManager.vibrate(CombinedVibration.createParallel(pattern))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        }
    }

    fun stopAlert() {
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }
}