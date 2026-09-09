package com.example.tp1_entregable.ui.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale

class VideoCaptureManager(private val context: Context) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var currentLensFacing: Boolean? = null

    companion object {
        private const val TAG = "VideoCaptureManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean,
        surfaceProvider: Preview.SurfaceProvider,
        onReady: (() -> Unit)? = null
    ) {
        // Si la cámara solicitada ya es la activa y el caso de uso existe, ejecutamos directamente
        if (currentLensFacing == useFrontCamera && videoCapture != null) {
            onReady?.invoke()
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Usamos SD/HD estándar para máxima compatibilidad con el codificador
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(surfaceProvider)

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                currentLensFacing = useFrontCamera

                // Damos 300ms para que el codificador reciba los primeros fotogramas tras el unbindAll
                Handler(Looper.getMainLooper()).postDelayed({
                    onReady?.invoke()
                }, 300)

            } catch (exc: Exception) {
                Log.e(TAG, "Error al configurar CameraX", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission")
    fun startRecording(prefix: String, onRecordingStateChanged: (Boolean) -> Unit) {
        val capture = videoCapture ?: run {
            Toast.makeText(context, "Iniciando cámara, reintente...", Toast.LENGTH_SHORT).show()
            return
        }

        // Si ya hay una grabación en curso, la detenemos
        if (activeRecording != null) {
            activeRecording?.stop()
            activeRecording = null
            return
        }

        val name = "${prefix}_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TP1_Videos")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val pendingRecording = capture.output.prepareRecording(context, mediaStoreOutputOptions)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pendingRecording.withAudioEnabled()
        }

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    onRecordingStateChanged(true)
                    Toast.makeText(context, "Grabando video ($prefix)...", Toast.LENGTH_SHORT).show()
                }
                is VideoRecordEvent.Finalize -> {
                    onRecordingStateChanged(false)
                    activeRecording = null

                    if (!recordEvent.hasError()) {
                        val uri = recordEvent.outputResults.outputUri
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(uri.path),
                            arrayOf("video/mp4"),
                            null
                        )
                        Toast.makeText(context, "Video guardado en Movies/TP1_Videos", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e(TAG, "Error en grabación: ${recordEvent.error}")
                        Toast.makeText(context, "Error al grabar: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}