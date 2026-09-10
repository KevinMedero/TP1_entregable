package com.example.tp1_entregable.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tp1_entregable.R

// Modelo de datos para representar la información de cada guía de catástrofe
data class GuiaDeCatastrofe(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val pasos: List<String>,
    val imagenResId: Int, // ID de imagen en res/drawable
    val videoRawId: Int? = null // ID de video en res/raw (opcional)
)

// Pantalla principal de la Guía de Acción ante Catástrofes mediante Jetpack Compose
@Composable
fun GuiaScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Lista de catastrofes
    val guides = listOf(
        GuiaDeCatastrofe(
            id = "1",
            titulo = "1. Inundación",
            descripcion = "Ante una alerta de inundación repentina o desborde de cauces:",
            pasos = listOf(
                "Desconecta la energía eléctrica y el gas antes de evacuar.",
                "Mantén preparados documentos importantes en bolsas herméticas.",
                "Dirígete inmediatamente a zonas elevadas fijadas por las autoridades.",
                "Evita cruzar calles inundadas a pie o en vehículo."
            ),
            imagenResId = R.drawable.img_inundacion
        ),
        GuiaDeCatastrofe(
            id = "2",
            titulo = "2. Sismo / Terremoto",
            descripcion = "En caso de movimientos telúricos o temblores fuertes:",
            pasos = listOf(
                "Mantén la calma y ubícate bajo una mesa firme o estructura resistente (Agáchate, Cúbrete y Agárrate).",
                "Aléjate de ventanas, espejos y muebles pesados que puedan caer.",
                "No utilices ascensores durante ni inmediatamente después del sismo.",
                "Una vez terminado el movimiento, evacúa de forma ordenada hacia zonas seguras abiertas."
            ),
            imagenResId = R.drawable.img_sismo
        ),
        GuiaDeCatastrofe(
            id = "3",
            titulo = "3. Incendio Forestal",
            descripcion = "Si te encuentras cerca o en zona de amenaza de incendios:",
            pasos = listOf(
                "Cierra ventanas y vías de ventilación para evitar el ingreso de humo denso.",
                "Cubre nariz y boca con un paño húmedo para proteger tus vías respiratorias.",
                "Evacúa en dirección contraria al viento y hacia zonas despejadas de vegetación.",
                "Llama de inmediato a los números de emergencia de bomberos (100) o policía (101)."
            ),
            imagenResId = R.drawable.img_incendio,
            videoRawId = R.raw.video_incendio
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra Superior Estándar con espacio para la barra de estado del sistema
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // Esta linea empuja el contenido debajo de la barra superior del telefono
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de acción para regresar a la pantalla de Inicio (HomeScreen)
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Volver"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guía de Acción ante Catástrofes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Contenido en Lista
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Renderiza eficientemente cada elemento de la lista mediante el componente GuiaCard
            items(guides) { guia ->
                GuiaCard(guia = guia)
            }
        }
    }
}

// Componente individual que dibuja la información detallada, imagen, lista de pasos y video
@Composable
fun GuiaCard(guia: GuiaDeCatastrofe) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Título
            Text(
                text = guia.titulo,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Imagen explicativa
            Image(
                painter = painterResource(id = guia.imagenResId),
                contentDescription = guia.titulo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = guia.descripcion,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Pasos a seguir
            guia.pasos.forEach { paso ->
                Text(
                    text = "• $paso",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // Reproductor de Video (si existe un video asociado en res/raw)
            guia.videoRawId?.let { videoRes ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Video explicativo:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            val videoUri = Uri.parse("android.resource://${context.packageName}/$videoRes")
                            setVideoURI(videoUri)
                            val mediaController = MediaController(context)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}