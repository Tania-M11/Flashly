package com.udenar.flashly.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.udenar.flashly.ui.theme.Fraunces
import com.udenar.flashly.ui.theme.Paleta

/**
 * Avatar circular: la imagen en blanco y negro del bucket «avatares» sobre su color pastel.
 * Mientras carga, o si no hay imagen, muestra las iniciales. [seleccionado] añade borde y check
 * (selector de avatares de «Editar perfil»).
 */
@Composable
fun AvatarCirculo(
    url: String?,
    colorFondo: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 56.dp,
    iniciales: String = "",
    seleccionado: Boolean = false,
    descripcion: String? = null,
    rolToque: Role = Role.Button,
    onClick: (() -> Unit)? = null,
) {
    val borde by animateDpAsState(if (seleccionado) 4.dp else 0.dp, label = "borde")
    Box(modifier.size(tamano)) {
        Box(
            Modifier
                .size(tamano)
                .clip(CircleShape)
                // El toque va en el círculo (no en el contenedor) para no recortar la marca de selección
                .then(if (onClick != null) Modifier.clickable(role = rolToque, onClickLabel = descripcion, onClick = onClick) else Modifier)
                .background(colorFondo)
                // Un borde de 0 dp dibuja igual una línea de 1 px: solo se aplica cuando hay selección
                .then(if (borde > 0.dp) Modifier.border(borde, Paleta.VerdeAzuladoOscuro, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            val respaldo: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        iniciales.take(2).uppercase(),
                        fontFamily = Fraunces, fontWeight = FontWeight.Bold,
                        fontSize = (tamano.value * 0.32f).sp, color = Paleta.Texto
                    )
                }
            }
            if (url == null) respaldo() else SubcomposeAsyncImage(
                model = url,
                contentDescription = descripcion,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(tamano).padding(tamano * 0.08f),
                loading = { respaldo() },
                error = { respaldo() },
            )
        }
        if (seleccionado) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(tamano * 0.32f)
                    .clip(CircleShape)
                    .background(Paleta.VerdeAzuladoOscuro)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Seleccionado", tint = Color.White, modifier = Modifier.size(tamano * 0.2f))
            }
        }
    }
}

/** Título de sección con acción opcional a la derecha («Ver todo»). */
@Composable
fun TituloSeccion(titulo: String, modifier: Modifier = Modifier, textoAccion: String? = null, onAccion: () -> Unit = {}) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (textoAccion != null) BotonFlashly(textoAccion, onAccion, estilo = EstiloBoton.Texto)
    }
}
