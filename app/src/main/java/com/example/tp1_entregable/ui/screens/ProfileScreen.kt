package com.example.tp1_entregable.ui.screens

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tp1_entregable.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Instanciamos el MapView usando el contexto actual
    val mapView = remember { MapView(context) }

    // Manejo del ciclo de vida (equivale a onResume/onPause de la Activity)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { ctx ->
            // 1. Configurar osmdroid y User-Agent para evitar Error 403
            val sharedPrefs = ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            Configuration.getInstance().load(ctx, sharedPrefs)
            Configuration.getInstance().userAgentValue = "MiAppMapaUnica/1.0 (tutt@gmail.com)"

            // 2. Inflar el layout XML existente
            val view = LayoutInflater.from(ctx).inflate(R.layout.layout_profile, null)

            // 3. Obtener la referencia del MapView desde el XML o configurarlo directamente
            val map = view.findViewById<MapView>(R.id.mapView) ?: mapView

            map.apply {
                setTileSource(TileSourceFactory.MAPNIK)

                // Enable zoom controls
                // setBuiltInZoomControls(true)
                setMultiTouchControls(true)

                val mapController = controller
                mapController.setZoom(15.0)

                val lat = 48.8566
                val lon = 2.3522
                val startPoint = GeoPoint(lat, lon)
                // Example coordinates: Paris (Latitude, Longitude)
                //  val startPoint = GeoPoint(48.8566, 2.3522)

                mapController.setCenter(startPoint)

                // Agregar el Marcador
                val startMarker = Marker(this)
                startMarker.position = startPoint
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                startMarker.title = "Paris, France"
                overlays.add(startMarker)
            }

            view
        },
        modifier = modifier.fillMaxSize()
    )
}