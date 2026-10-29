package com.udenar.flashly.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.theme.Paleta

/** Círculos pastel grandes y difuminados en las esquinas: dan color sin saturar la pantalla. */
@Composable
fun FondoDecorado(modifier: Modifier = Modifier, intensidad: Float = 1f) {
    Canvas(modifier.fillMaxSize()) {
        drawCircle(Paleta.Menta.copy(alpha = 0.55f * intensidad), radius = size.width * 0.55f, center = Offset(-size.width * 0.12f, size.height * 0.02f))
        drawCircle(Paleta.Lavanda.copy(alpha = 0.45f * intensidad), radius = size.width * 0.42f, center = Offset(size.width * 1.08f, size.height * 0.30f))
        drawCircle(Paleta.RosaSuave.copy(alpha = 0.9f * intensidad), radius = size.width * 0.6f, center = Offset(size.width * 0.15f, size.height * 1.05f))
    }
}

/** Botón circular de volver, título en Fraunces y subtítulo. */
@Composable
fun EncabezadoAuth(titulo: String, subtitulo: String, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IconButton(
            onClick = onVolver,
            modifier = Modifier
                .shadow(4.dp, CircleShape, ambientColor = Color(0x22B08A70), spotColor = Color(0x22B08A70))
                .clip(CircleShape)
                .background(Paleta.Blanco)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Paleta.Texto)
        }
        Text(titulo, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp))
        Text(subtitulo, style = MaterialTheme.typography.bodyLarge, color = Paleta.TextoSecundario)
    }
}

// Avatares del catálogo usados como ilustración de la bienvenida (mismas imágenes del bucket)
private val decorativos = listOf(
    "avatar_2.png" to Paleta.Menta, "avatar_1.png" to Paleta.RosaPastel, "avatar_6.png" to Paleta.Periwinkle,
    "avatar_4.png" to Paleta.Melocoton, "avatar_3.png" to Paleta.Lavanda,
)

/** Fila de avatares en arco que flotan suavemente, cada uno con su propio ritmo. */
@Composable
fun AvataresFlotantes(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "flotar")
    val arco = listOf(26, 6, 0, 6, 26)
    val tamanos = listOf(58, 66, 76, 66, 58)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
        decorativos.forEachIndexed { i, (archivo, color) ->
            val vaiven by transicion.animateFloat(
                initialValue = -5f, targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(1600 + i * 270, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "vaiven$i"
            )
            AvatarCirculo(
                url = SupabaseProvider.urlAvatar(archivo),
                colorFondo = color,
                tamano = tamanos[i].dp,
                modifier = Modifier.offset(y = (arco[i] + vaiven).dp)
            )
        }
    }
}

/** Una condición de la contraseña: check verde si se cumple, punto gris si no (no depende solo del color). */
@Composable
fun Requisito(texto: String, cumplido: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(if (cumplido) Paleta.Exito else Paleta.BordeSuave),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (cumplido) Icons.Rounded.Check else Icons.Rounded.Circle,
                contentDescription = if (cumplido) "Cumplido" else "Pendiente",
                tint = if (cumplido) Color.White else Paleta.TextoSecundario,
                modifier = Modifier.size(if (cumplido) 14.dp else 6.dp)
            )
        }
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (cumplido) Paleta.Exito else Paleta.TextoSecundario
        )
    }
}
