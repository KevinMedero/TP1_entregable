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

// Clase gestora para la vista previa e inicio/detención de grabación de video usando la API CameraX
class VideoCaptureManager(private val context: Context) {

    // Instancia del caso de uso de captura de video en CameraX
    private var videoCapture: VideoCapture<Recorder>? = null

    // Objeto que representa la sesión de grabación activa
    private var activeRecording: Recording? = null

    // Almacena qué cámara se encuentra configurada actualmente (true = frontal/selfie, false = trasera)
    private var currentLensFacing: Boolean? = null

    companion object {
        private const val TAG = "VideoCaptureManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    // Vincula los casos de uso de CameraX (Preview y VideoCapture) al ciclo de vida de la pantalla
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

        // Obtiene la instancia asíncrona del proveedor de cámara
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Configura el grabador interno (Recorder) estableciendo calidad SD para máxima compatibilidad
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(surfaceProvider)

            // Selecciona la lente (Frontal o Trasera) según el parámetro recibido
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                // Desvincula cualquier caso de uso previo para no saturar el hardware de la cámara
                cameraProvider.unbindAll()
                // Vincula el ciclo de vida, la lente seleccionada, la vista previa y la grabación de video
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                currentLensFacing = useFrontCamera

                // Damos 300ms de pausa para que el codificador reciba los primeros fotogramas tras el unbindAll
                Handler(Looper.getMainLooper()).postDelayed({
                    onReady?.invoke()
                }, 300)

            } catch (exc: Exception) {
                Log.e(TAG, "Error al configurar CameraX", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Inicia o detiene la grabación de video actuando como un conmutador (toggle)
    @SuppressLint("MissingPermission")
    fun startRecording(prefix: String, onRecordingStateChanged: (Boolean) -> Unit) {
        val capture = videoCapture ?: run {
            Toast.makeText(context, "Iniciando cámara, reintente...", Toast.LENGTH_SHORT).show()
            return
        }

        // Si se llama al metodo y ya existe una grabación activa, la detiene
        if (activeRecording != null) {
            activeRecording?.stop()
            activeRecording = null
            return
        }

        // Formatea el nombre del archivo de video con la marca de tiempo actual
        val name = "${prefix}_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        // Define la informacion para guardar el video mediante MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TP1_Videos")
            }
        }

        // Crea las opciones de salida asociadas al almacenamiento externo del dispositivo
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // Prepara la sesión de grabación
        val pendingRecording = capture.output.prepareRecording(context, mediaStoreOutputOptions)

        // Habilita la captura de audio si el usuario concedió el permiso de micrófono
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pendingRecording.withAudioEnabled()
        }

        // Inicia la captura y asigna el listener para escuchar los eventos de la grabación
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
                        // Notifica al escáner de medios para que el video aparezca inmediatamente en la galería
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