package com.udenar.flashly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

/** Superficie base: esquinas de 24 dp, sombra muy suave y color de fondo configurable. */
@Composable
fun TarjetaFlashly(
    modifier: Modifier = Modifier,
    color: Color = Paleta.Blanco,
    esquinas: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(esquinas)
    Column(
        modifier
            .shadow(elevation = 6.dp, shape = forma, ambientColor = Color(0x22B08A70), spotColor = Color(0x22B08A70))
            .clip(forma)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(20.dp),
        content = contenido
    )
}

/**
 * Tarjeta de mazo: color de portada de la paleta, título en Fraunces, descripción corta,
 * creador, número de tarjetas, visibilidad y flecha para abrirlo.
 */
@Composable
fun TarjetaMazo(
    titulo: String,
    numTarjetas: Int,
    color: ColorFlashly,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    creador: String? = null,
    esPublico: Boolean? = null,
    avatarCreadorUrl: String? = null,
    colorAvatarCreador: Color? = null,
    fecha: String? = null,
    interactiva: Boolean = true,   // false: vista previa del editor (sin toque ni flecha)
    accesorio: (@Composable () -> Unit)? = null,
) {
    val forma = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(6.dp, forma, ambientColor = Color(0x22B08A70), spotColor = Color(0x22B08A70))
            .clip(forma)
            .background(color.fondo)
            .then(if (interactiva) Modifier.clickable(role = Role.Button, onClickLabel = "Abrir mazo", onClick = onClick) else Modifier)
    ) {
        // Adorno sutil: dos círculos claros en la esquina, como tarjetas apiladas
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.18f), radius = size.minDimension * 0.55f, center = Offset(size.width * 1.02f, -size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.14f), radius = size.minDimension * 0.32f, center = Offset(size.width * 0.86f, size.height * 1.05f))
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color.contenido,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                accesorio?.invoke()
            }
            if (!descripcion.isNullOrBlank()) {
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.contenido.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (creador != null || fecha != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (creador != null) {
                        // Aro blanco: el avatar se distingue aunque su color coincida con el del mazo
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                            AvatarCirculo(
                                url = avatarCreadorUrl, colorFondo = colorAvatarCreador ?: Color.White.copy(alpha = 0.6f),
                                tamano = 28.dp, iniciales = creador
                            )
                        }
                        Text("@$creador", style = MaterialTheme.typography.labelLarge, color = color.contenido)
                    }
                    if (fecha != null) {
                        Text(
                            if (creador != null) "· $fecha" else "Actualizado $fecha",
                            style = MaterialTheme.typography.labelMedium, color = color.contenido.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            Spacer(Modifier.size(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaInfo(texto = "$numTarjetas ${if (numTarjetas == 1) "tarjeta" else "tarjetas"}", icono = Icons.Rounded.Style)
                if (esPublico != null) EtiquetaVisibilidad(esPublico)
                Spacer(Modifier.weight(1f))
                if (interactiva) Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Paleta.Carbon),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
