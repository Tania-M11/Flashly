package com.udenar.flashly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

/** Barra de búsqueda en forma de píldora, con lupa y botón para borrar el texto. */
@Composable
fun BarraBusqueda(texto: String, onCambio: (String) -> Unit, marcador: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Paleta.Blanco)
            .border(1.dp, Paleta.Borde, CircleShape)
            .padding(start = 18.dp, end = 6.dp)
            .semantics { contentDescription = marcador },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Paleta.TextoSecundario)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = texto,
            onValueChange = onCambio,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Paleta.Texto),
            cursorBrush = SolidColor(Paleta.VerdeAzuladoOscuro),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            modifier = Modifier.weight(1f).padding(vertical = 16.dp),
            decorationBox = { campo ->
                if (texto.isEmpty()) Text(marcador, style = MaterialTheme.typography.bodyLarge, color = Paleta.TextoSecundario)
                campo()
            }
        )
        if (texto.isNotEmpty()) {
            IconButton(onClick = { onCambio("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Borrar búsqueda", tint = Paleta.TextoSecundario)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

/** Ícono de cada categoría (el nombre viene de la tabla categorias). */
fun iconoCategoria(nombre: String?): ImageVector = when (nombre) {
    "translate" -> Icons.Rounded.Translate
    "science" -> Icons.Rounded.Science
    "calculate" -> Icons.Rounded.Calculate
    "history" -> Icons.Rounded.HistoryEdu
    "public" -> Icons.Rounded.Public
    "computer" -> Icons.Rounded.Computer
    "palette" -> Icons.Rounded.Palette
    else -> Icons.Rounded.Category
}

/** Chip de categoría con ícono; seleccionado se rellena con el color de la categoría. */
@Composable
fun ChipCategoria(texto: String, icono: ImageVector?, color: ColorFlashly, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (seleccionado) color.fondo else Paleta.Blanco)
            .border(1.5.dp, if (seleccionado) color.fuerte else Paleta.Borde, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = seleccionado }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icono != null) Icon(icono, contentDescription = null, tint = if (seleccionado) color.contenido else color.fuerte, modifier = Modifier.size(18.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = if (seleccionado) color.contenido else Paleta.Texto)
    }
}
