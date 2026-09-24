package com.dbrstudio.iudigitalradio

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.dbrstudio.iudigitalradio.ui.theme.IUDigitalRadioTheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

data class Emisora(val nombre: String, val genero: String, val url: String)

val emisoras = listOf(
    Emisora("Emisora 1", "Radio Tiempo Cali", "https://26583.live.streamtheworld.com/RT_CALIAAC_SC"),
    Emisora("Emisora 2", "La Mega Medellín", "https://us-b4-p-e-qg12-audio.cdn.mdstrm.com/live-audio-aw/632cb48f613bac0856b931ab"),
    Emisora("Emisora 3", "Tropicana Medellín", "https://playerservices.streamtheworld.com/api/livestream-redirect/TR_MEDELLINAAC.aac"),
    Emisora("Emisora 4", "Bésame Medellín", "https://playerservices.streamtheworld.com/api/livestream-redirect/BESAME_MEDELLINAAC.aac"),
    Emisora("Emisora 5", "Olímpica Stereo Medellín", "https://27343.live.streamtheworld.com/OLP_MEDELLINAAC.aac"),
    Emisora("Emisora 6", "CaracolRadio", "https://playerservices.streamtheworld.com/api/livestream-redirect/CARACOL_RADIOAAC.aac")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IUDigitalRadioTheme {
                RadioScreen()
            }
        }
    }
}

@Composable
fun RadioScreen() {
    val context = LocalContext.current

    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var selectedStation by rememberSaveable { mutableStateOf(0) }
    var photo by rememberSaveable { mutableStateOf<Bitmap?>(null) }

    // Se crea una sola vez y se reutiliza mientras la pantalla vive
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Cuando cambia la emisora seleccionada, cambia lo que suena
    LaunchedEffect(selectedStation) {
        val emisora = emisoras[selectedStation]
        exoPlayer.setMediaItem(MediaItem.fromUri(emisora.url))
        exoPlayer.prepare()
        if (isPlaying) exoPlayer.play()
    }

    // Libera el reproductor cuando la pantalla se destruye
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) photo = bitmap
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Se necesita el permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        ProfileHeader(
            photo = photo,
            onTakePhoto = {
                val tienePermiso = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (tienePermiso) {
                    cameraLauncher.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlayerCard(
            nombre = emisoras[selectedStation].nombre,
            genero = emisoras[selectedStation].genero,
            isPlaying = isPlaying,
            isMuted = isMuted,
            onPlay = {
                isPlaying = true
                exoPlayer.play()
                vibrarCorto(context)
            },
            onPause = {
                isPlaying = false
                exoPlayer.pause()
                vibrarCorto(context)
            },
            onMute = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
                vibrarCorto(context)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        StationList(
            selectedStation = selectedStation,
            onSelect = { selectedStation = it }
        )
    }
}

// Sección superior: foto + botón de cámara
@Composable
fun ProfileHeader(photo: Bitmap?, onTakePhoto: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Bienvenido", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onTakePhoto) {
                Text("Tomar foto")
            }
        }
    }
}

// Sección central: reproductor
@Composable

fun PlayerCard(
    nombre: String,
    genero: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onMute: () -> Unit
) {
    // Aparece al reproducir y se va al pausar
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(600),
        label = "haloAlpha"
    )

    // Latido suave del halo
    val transition = rememberInfiniteTransition(label = "halo")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (haloAlpha > 0f) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.height * 0.9f * pulse
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7).copy(alpha = 0.80f * haloAlpha),
                            Color(0xFF66BB6A).copy(alpha = 0.40f * haloAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )
                    // Se estira en horizontal para que el halo sea ovalado y cubra el ancho
                    scale(
                        scaleX = size.width / size.height,
                        scaleY = 1f,
                        pivot = center
                    ) {
                        drawCircle(brush = brush, radius = radius, center = center)
                    }
                }
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                // Un poco translúcida al reproducir para que se sienta el brillo
                containerColor = MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 1f - 0.25f * haloAlpha)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(nombre, style = MaterialTheme.typography.headlineSmall)
                Text(genero, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (isPlaying) "Reproduciendo..." else "Pausado",
                    style = MaterialTheme.typography.bodySmall
                )
                if (isMuted) {
                    Text("Silenciado", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                    IconButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                    IconButton(onClick = onMute) {
                        Icon(Icons.Default.VolumeOff, contentDescription = "Mute")
                    }
                }
            }
        }
    }
}

fun vibrarCorto(context: android.content.Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) {
        android.util.Log.d("vibrar", "Este dispositivo no tiene motor de vibración")
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(80)
    }
}

// Sección inferior: catálogo
@Composable
fun StationList(selectedStation: Int, onSelect: (Int) -> Unit) {
    LazyColumn {
        itemsIndexed(emisoras) { index, emisora ->
            val seleccionada = index == selectedStation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (seleccionada)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(emisora.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(emisora.genero, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadioScreenPreview() {
    IUDigitalRadioTheme {
        RadioScreen()
    }
}