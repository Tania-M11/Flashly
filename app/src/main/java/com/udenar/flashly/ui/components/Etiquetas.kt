package com.udenar.flashly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.Paleta

/** Etiqueta pequeña translúcida, pensada para ir sobre tarjetas de color. */
@Composable
fun EtiquetaInfo(texto: String, modifier: Modifier = Modifier, icono: ImageVector? = null) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = Paleta.Texto, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = Paleta.Texto)
    }
}

/** Público o privado, con ícono y texto (no depende solo del color). */
@Composable
fun EtiquetaVisibilidad(esPublico: Boolean, modifier: Modifier = Modifier) {
    EtiquetaInfo(
        texto = if (esPublico) "Público" else "Privado",
        icono = if (esPublico) Icons.Rounded.Public else Icons.Rounded.Lock,
        modifier = modifier
    )
}

/** Filtro tipo píldora («Todos», «Propios», «Semana»…). El seleccionado va en verde oscuro con check. */
@Composable
fun FiltroChip(texto: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(if (seleccionado) Paleta.VerdeAzuladoOscuro else Paleta.Blanco)
            .border(1.dp, if (seleccionado) Paleta.VerdeAzuladoOscuro else Paleta.Borde, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = seleccionado }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (seleccionado) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (seleccionado) Color.White else Paleta.Texto
        )
    }
}
