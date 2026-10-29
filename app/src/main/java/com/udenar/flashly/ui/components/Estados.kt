package com.udenar.flashly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.udenar.flashly.R
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

/** Carga de datos: indicador en verde azulado con un texto opcional. */
@Composable
fun EstadoCarga(modifier: Modifier = Modifier, texto: String? = "Cargando…") {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(color = Paleta.VerdeAzulado, trackColor = Paleta.Menta, strokeWidth = 4.dp)
        if (texto != null) Text(texto, style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
    }
}

/**
 * Estado vacío: ilustración (por defecto el logo de Flashly sobre un círculo pastel), título,
 * explicación breve y una acción opcional, p. ej. «Crear mi primer mazo».
 */
@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    color: ColorFlashly = ColorFlashly.LAVANDA,
    textoAccion: String? = null,
    onAccion: () -> Unit = {},
) {
    Column(
        modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(148.dp).clip(CircleShape).background(color.fondo), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.logo_flashly), contentDescription = null, modifier = Modifier.size(116.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario, textAlign = TextAlign.Center)
        if (textoAccion != null) {
            Spacer(Modifier.height(4.dp))
            BotonFlashly(textoAccion, onAccion)
        }
    }
}

/** Error de conexión u otro fallo, con botón «Reintentar». */
@Composable
fun EstadoError(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    titulo: String = "Algo salió mal",
) {
    val sinConexion = "conexión" in mensaje.lowercase() || "internet" in mensaje.lowercase()
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Paleta.RosaSuave), contentAlignment = Alignment.Center) {
            Icon(
                if (sinConexion) Icons.Rounded.CloudOff else Icons.Rounded.ErrorOutline,
                contentDescription = null, tint = Paleta.Error, modifier = Modifier.size(40.dp)
            )
        }
        Text(if (sinConexion) "Sin conexión" else titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario, textAlign = TextAlign.Center)
        BotonFlashly("Reintentar", onReintentar, estilo = EstiloBoton.Secundario, icono = Icons.Rounded.Refresh)
    }
}

enum class TipoMensaje(val fondo: Color, val contenido: Color, val icono: ImageVector) {
    Exito(Paleta.ExitoSuave, Paleta.Exito, Icons.Rounded.CheckCircle),
    Error(Paleta.ErrorSuave, Paleta.Error, Icons.Rounded.ErrorOutline),
    Info(Paleta.Crema, Paleta.VerdeAzuladoOscuro, Icons.Rounded.Info),
}

/** Aviso en línea para éxito («Cambios guardados»), error o información. Se anuncia a lectores de pantalla. */
@Composable
fun MensajeAviso(texto: String?, tipo: TipoMensaje, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn() + slideInVertically { -it / 3 },
        exit = fadeOut() + slideOutVertically { -it / 3 },
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tipo.fondo)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(tipo.icono, contentDescription = null, tint = tipo.contenido, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = tipo.contenido)
        }
    }
}
