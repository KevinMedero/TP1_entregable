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

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Cargar la configuración de osmdroid ANTES de renderizar la vista
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, sharedPrefs)
        Configuration.getInstance().userAgentValue = "MiAppMapaUnica/1.0 (tutt@gmail.com)"
    }

    // Estados para la ubicación, dirección y errores
    var currentGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var currentAddress by remember { mutableStateOf("Buscando ubicación...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var referenceText by remember { mutableStateOf("") }

    // Variable para mantener la referencia al MapView real que se infla en el XML
    var activeMapView by remember { mutableStateOf<MapView?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // Función para obtener la dirección en texto (Reverse Geocoding)
    fun updateAddress(lat: Double, lon: Double) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        currentAddress = addresses[0].getAddressLine(0) ?: "Dirección no disponible"
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    currentAddress = addresses[0].getAddressLine(0) ?: "Dirección no disponible"
                }
            }
        } catch (e: Exception) {
            currentAddress = "Lat: $lat, Lon: $lon"
        }
    }

    // Función para verificar GPS y obtener ubicación
    fun fetchLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        // Función Error: Verifica si el GPS está desactivado
        if (!isGpsEnabled) {
            errorMessage = "ERROR: El GPS está desactivado. Por favor, actívalo para continuar."
            return
        }

        errorMessage = null

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    currentGeoPoint = point
                    updateAddress(location.latitude, location.longitude)

                    // Actualizar el mapa que está dibujado en pantalla
                    activeMapView?.let { map ->
                        map.controller.setZoom(17.0)
                        map.controller.setCenter(point)
                        map.overlays.clear()

                        val marker = Marker(map).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Mi Ubicación Actual"
                        }
                        map.overlays.add(marker)

                        // Forzar el redibujado inmediato de las imágenes/tiles
                        map.invalidate()
                    }
                } else {
                    errorMessage = "No se pudo obtener la ubicación actual. Intenta nuevamente."
                }
            }
        }
    }

    // Solicitud de Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            fetchLocation()
        } else {
            errorMessage = "Permiso de ubicación denegado."
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Manejo del ciclo de vida de la vista de mapa (equivale a onResume/onPause de la Activity)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activeMapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> activeMapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activeMapView?.onDetach()
        }
    }

    // Registra locación en Firebase
    fun saveLocationToFirebase() {
        val point = currentGeoPoint
        if (point == null) {
            Toast.makeText(context, "No hay ubicación disponible para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        // Instancia de Firestore
        val db = FirebaseFirestore.getInstance()

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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                // Infla el layout XML existente
                val view = LayoutInflater.from(context).inflate(R.layout.layout_profile, null)

                // Obtiene la referencia del MapView desde el XML o configurarlo directamente
                val map = view.findViewById<MapView>(R.id.mapView)

                map.apply {
                    setTileSource(TileSourceFactory.MAPNIK)

                    // Enable zoom controls
                    // setBuiltInZoomControls(true)
                    setMultiTouchControls(true)

                    val mapController = controller
                    mapController.setZoom(15.0)
                }

                // Guarda la referencia al MapView activo
                activeMapView = map

                // Vinculación de Vistas del XML
                val etReference = view.findViewById<EditText>(R.id.et_referencia)
                val btnSave = view.findViewById<Button>(R.id.btn_guardar_ubicacion)

                btnSave?.setOnClickListener {
                    referenceText = etReference?.text.toString()
                    saveLocationToFirebase()
                }

                view
            },
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