package com.example.tp1_entregable.ui.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Clase auxiliar para gestionar la grabación de audio en el almacenamiento del dispositivo
class AudioRecorderManager(private val context: Context) {

    // Instancia del MediaRecorder nativo para capturar audio desde el micrófono
    private var mediaRecorder: MediaRecorder? = null

    // Ruta física del archivo guardado
    private var outputFile: String = ""

    // URI del contenido insertado en la base de datos de MediaStore
    private var currentUri: Uri? = null

    // Inicia el proceso de grabación configurando la fuente, formato y destino según la versión de Android
    fun startRecording(onStateChanged: (Boolean) -> Unit) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // Instancia el MediaRecorder usando el constructor moderno en Android 12+ o el tradicional en versiones anteriores
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 o mayor -> MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "AUDIO_$timeStamp.3gp")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                }

                currentUri = context.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
                )

                val fileDescriptor = currentUri?.let {
                    context.contentResolver.openFileDescriptor(it, "w")?.fileDescriptor
                }

                // Configura e inicia la captura en el hardware de audio
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setOutputFile(fileDescriptor)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    prepare()
                    start()
                }
                Toast.makeText(context, "Grabando audio en carpeta Music (MediaStore)...", Toast.LENGTH_SHORT).show()
            } else {
                // Android 10 o menor -> File directo
                @Suppress("DEPRECATION")
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                if (!musicDir.exists()) musicDir.mkdirs()

                val audioFile = File(musicDir, "AUDIO_$timeStamp.3gp")
                outputFile = audioFile.absolutePath

                // Configura e inicia la captura de audio indicando la ruta directa del archivo
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setOutputFile(outputFile)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    prepare()
                    start()
                }
                Toast.makeText(context, "Grabando audio en carpeta Music...", Toast.LENGTH_SHORT).show()
            }
            // Notifica a la interfaz (Compose) que la grabación se inició con éxito
            onStateChanged(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al iniciar grabación de audio", Toast.LENGTH_SHORT).show()
            onStateChanged(false)
        }
    }

    // Detiene la grabación actual, empaqueta el archivo y libera el hardware del micrófono
    fun stopRecording(onStateChanged: (Boolean) -> Unit) {
        try {
            // Finaliza la captura y libera los recursos del sistema
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            onStateChanged(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Toast.makeText(context, "Audio guardado en Music (MediaStore)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Audio guardado en: $outputFile", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder = null
            // Garantiza que el estado vuelva a falso si ocurre una interrupción inesperada al detener
            onStateChanged(false)
        }
    }
}