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

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String = ""
    private var currentUri: Uri? = null

    fun startRecording(onStateChanged: (Boolean) -> Unit) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

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
            onStateChanged(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al iniciar grabación de audio", Toast.LENGTH_SHORT).show()
            onStateChanged(false)
        }
    }

    fun stopRecording(onStateChanged: (Boolean) -> Unit) {
        try {
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
            onStateChanged(false)
        }
    }
}