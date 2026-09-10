package com.example.tp1_entregable.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tp1_entregable.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

// Pantalla de geolocalización que integra OpenStreetMap (osmdroid) y persistencia de reportes en Firestore
@Composable
fun UbicacionScreen(modifier: Modifier = Modifier) {
    // Contexto de la app e instancia del ciclo de vida para sincronizar el mapa
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Carga inicial de la configuración del motor de mapas OpenStreetMap (User-Agent y caché) antes de inflar la interfaz
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, sharedPrefs)
        Configuration.getInstance().userAgentValue = "MiAppMapaUnica/1.0 (tutt@gmail.com)"
    }

    // Estados reactivos para rastrear coordenadas, dirección procesada, mensajes de error e inputs
    var currentGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var currentAddress by remember { mutableStateOf("Buscando ubicación...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var referenceText by remember { mutableStateOf("") }

    // Mantiene la referencia al objeto MapView inflado en el XML para modificar sus marcadores y niveles de zoom
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    // Cliente de servicios de ubicación fusionada de Google Play Services y conversor de coordenadas a texto (Geocoder)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // Realiza Geocodificación Inversa (convierte coordenadas lat/lon a una dirección legible de calle)
    fun updateAddress(lat: Double, lon: Double) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ (Android 13+): Utiliza la llamada asíncrona mediante callback para no congelar la UI
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        currentAddress = addresses[0].getAddressLine(0) ?: "Dirección no disponible"
                    }
                }
            } else {
                // API < 33: Utiliza el metodo sincrónico tradicional
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    currentAddress = addresses[0].getAddressLine(0) ?: "Dirección no disponible"
                }
            }
        } catch (e: Exception) {
            // Muestra las coordenadas brutas como respaldo si falla el servicio de Geocoder de Google
            currentAddress = "Lat: $lat, Lon: $lon"
        }
    }

    // Verifica el hardware GPS y obtiene la última ubicación conocida del dispositivo
    fun fetchLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        // Función Error: comprueba si los sensores de ubicación están encendidos en los ajustes del sistema
        if (!isGpsEnabled) {
            errorMessage = "ERROR: El GPS está desactivado. Por favor, actívalo para continuar."
            return
        }

        errorMessage = null // Limpia errores previos si el GPS está activo

        // Verifica que los permisos de localización precisa hayan sido otorgados antes de consultar al proveedor
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    currentGeoPoint = point
                    updateAddress(location.latitude, location.longitude)

                    // Centra el mapa en las coordenadas obtenidas y dibuja el pin de ubicación
                    activeMapView?.let { map ->
                        map.controller.setZoom(17.0)
                        map.controller.setCenter(point)
                        map.overlays.clear()

                        // Crea y dibuja el nuevo marcador personalizado
                        val marker = Marker(map).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Mi Ubicación Actual"
                        }
                        map.overlays.add(marker)

                        // Forzar el redibujado inmediato de las imágenes
                        map.invalidate()
                    }
                } else {
                    errorMessage = "No se pudo obtener la ubicación actual. Intenta nuevamente."
                }
            }
        }
    }

    // Solicitud de permisos de geolocalizacion
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            fetchLocation()
        } else {
            errorMessage = "Permiso de ubicación denegado."
        }
    }

    // Solicita automáticamente los permisos de ubicación al entrar en la pantalla
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Sincroniza el ciclo de vida del mapa (MapView) con la pantalla
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activeMapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> activeMapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Se ejecuta al salir de la pantalla: destruye la vista del mapa y remueve el observador
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activeMapView?.onDetach()
        }
    }

    // Envía la ubicación procesada, la dirección y las notas de referencia a la base de datos de Firestoree
    fun saveLocationToFirebase() {
        val point = currentGeoPoint
        if (point == null) {
            Toast.makeText(context, "No hay ubicación disponible para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        // Estructura de mapa clave-valor para insertar en la colección "siniestros"
        val data = hashMapOf(
            "latitud" to point.latitude,
            "longitud" to point.longitude,
            "direccion" to currentAddress,
            "referencia" to referenceText,
            "fechaHora" to Timestamp.now()
        )

        db.collection("siniestros")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Ubicación registrada en Firestore", Toast.LENGTH_LONG).show()
                referenceText = ""
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Layout principal que contiene la interfaz nativa inflada mediante AndroidView
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.layout_ubicacion, null)

                // Referencia al control de mapa en la vista tradicional XML
                val map = view.findViewById<MapView>(R.id.mapView)

                map.apply {
                    setTileSource(TileSourceFactory.MAPNIK)  // Define la fuente de mapas estándar de OpenStreetMap
                    setMultiTouchControls(true)
                    val mapController = controller
                    mapController.setZoom(15.0)
                }

                // Guarda la instancia activa en la variable de estado
                activeMapView = map

                // Vinculación de Vistas del XML
                val etReference = view.findViewById<EditText>(R.id.et_referencia)
                val btnSave = view.findViewById<Button>(R.id.btn_guardar_ubicacion)

                // Listener para capturar el texto e invocar el guardado en Firebase
                btnSave?.setOnClickListener {
                    referenceText = etReference?.text.toString()
                    saveLocationToFirebase()
                }

                view
            },
            // Bloque de actualización: Actualiza dinámicamente los TextViews ante cambios de dirección o errores
            update = { view ->
                // Muestra la dirección actual EN GRANDE
                val tvAddress = view.findViewById<TextView>(R.id.tv_direccion)
                tvAddress?.text = currentAddress

                // Muestra error en pantalla si el GPS está apagado
                val tvError = view.findViewById<TextView>(R.id.tv_error)
                if (errorMessage != null) {
                    tvError?.visibility = View.VISIBLE
                    tvError?.text = errorMessage
                } else {
                    tvError?.visibility = View.GONE
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}