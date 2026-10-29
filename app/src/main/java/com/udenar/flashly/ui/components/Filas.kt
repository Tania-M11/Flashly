package com.udenar.flashly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

/**
 * Fila de opción (Perfil, Configuración): ícono en círculo pastel, título, detalle opcional y flecha.
 * Con [peligro] el texto va en rojo (cerrar sesión).
 */
@Composable
fun FilaOpcion(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    color: ColorFlashly = ColorFlashly.MENTA,
    peligro: Boolean = false,
    conFlecha: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (peligro) Paleta.RosaSuave else color.fondo.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = if (peligro) Paleta.Error else color.fuerte, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = if (peligro) Paleta.Error else Paleta.Texto)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (conFlecha) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Paleta.TextoSecundario)
    }
}

/** Dato de solo lectura con etiqueta pequeña encima (correo, teléfono, fecha de registro). */
@Composable
fun FilaDato(etiqueta: String, valor: String, icono: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icono, contentDescription = null, tint = Paleta.TextoSecundario, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Paleta.TextoSecundario)
            Text(valor, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun Separador(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = Paleta.BordeSuave)
}

/** Recuadro de estadística: número grande en Fraunces y etiqueta. */
@Composable
fun TarjetaDato(valor: String, etiqueta: String, color: ColorFlashly, icono: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color.fondo.copy(alpha = 0.55f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = color.fuerte, modifier = Modifier.size(18.dp))
        }
        Text(valor, style = MaterialTheme.typography.headlineMedium, color = Paleta.Texto)
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Paleta.TextoSecundario)
    }
}
