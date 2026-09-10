package com.example.tp1_entregable.ui.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

// Clase auxiliar para gestionar la inicialización, la escucha y el procesamiento de los comandos de voz
class VoiceCommandManager(
    private val context: Context,

    // Callback que devuelve el texto reconocido en minúsculas hacia la pantalla Home
    private val onCommandRecognized: (String) -> Unit
) {

    // Instancia del motor nativo de reconocimiento de voz de Android
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        // Verifica que el dispositivo tenga un servicio de reconocimiento de voz disponible
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            // Crea el reconocedor de voz y le asigna el listener de eventos
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.e("VoiceCommand", "Error en reconocimiento de voz: $error")
                    }

                    // Se dispara cuando se termina de procesar el audio y devuelve las posibles interpretaciones
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            // Toma la primera opción (la de mayor certeza) y la convierte a minúsculas
                            val commandText = matches[0].lowercase(Locale.ROOT)
                            Log.d("VoiceCommand", "Texto reconocido: $commandText")
                            onCommandRecognized(commandText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    // Inicia la captura de audio en segundo plano mediante el envío de un Intent de reconocimiento
    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }
        // Comienza a escuchar la voz del usuario con la configuración establecida
        speechRecognizer?.startListening(intent)
    }

    // Libera los recursos del micrófono y destruye la instancia para evitar fugas de memoria
    fun destroy() {
        speechRecognizer?.destroy()
    }
}