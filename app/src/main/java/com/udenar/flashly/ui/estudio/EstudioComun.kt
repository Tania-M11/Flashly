package com.udenar.flashly.ui.estudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

/** Identidad visual de cada modo (documento: menta, naranja, lavanda y rosa). */
data class EstiloModo(
    val color: ColorFlashly,
    val icono: ImageVector,
    val descripcion: String,
    val reglas: List<String>,
)

fun estiloModo(modo: ModoJuego): EstiloModo = when (modo) {
    ModoJuego.PRACTICA -> EstiloModo(
        ColorFlashly.MENTA, Icons.Rounded.Spa,
        "Estudio libre, sin presión ni reloj.",
        listOf("Retroalimentación inmediata", "Sin tiempo ni penalizaciones", "10 XP por acierto · 2 XP por definición"),
    )
    ModoJuego.CONTRARRELOJ -> EstiloModo(
        ColorFlashly.MELOCOTON, Icons.Rounded.Timer,
        "Responde todo lo que puedas antes de que se acabe el tiempo.",
        listOf("Unos 12 segundos por tarjeta", "Los aciertos seguidos forman combos", "El combo suma hasta +10 XP por respuesta"),
    )
    ModoJuego.REPASO -> EstiloModo(
        ColorFlashly.LAVANDA, Icons.Rounded.Psychology,
        "Repetición espaciada: primero lo que fallaste o marcaste para practicar.",
        listOf("Solo las tarjetas pendientes de hoy", "Si aciertas, vuelve en más días", "Si fallas, vuelve hoy mismo"),
    )
    ModoJuego.RETO -> EstiloModo(
        ColorFlashly.ROSA, Icons.Rounded.EmojiEvents,
        "Hasta 10 tarjetas al azar con tres vidas.",
        listOf("Cada pregunta fallada quita una vida", "Voltear una definición nunca resta vidas", "XP doble en tu primer reto completado del día"),
    )
}

/** Días hasta el próximo repaso según la caja de Leitner (igual que en el servidor). */
private val intervalos = listOf(0, 1, 3, 7, 14, 30)

fun textoProximoRepaso(caja: Int): String = when (val dias = intervalos[caja.coerceIn(0, 5)]) {
    0 -> "Volverá hoy para reforzarla"
    1 -> "Volverá mañana"
    else -> "Volverá en $dias días"
}

fun textoCaja(caja: Int?): String = when (caja) {
    null -> "Tarjeta nueva"
    0 -> "Por reforzar"
    5 -> "Dominada"
    else -> "Nivel $caja de 5"
}

/** Indicador de nivel de repetición espaciada: 5 puntos, rellenos hasta la caja actual. */
@Composable
fun NivelCaja(caja: Int?, color: ColorFlashly, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Paleta.Blanco.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { i ->
            Box(
                Modifier.size(7.dp).clip(CircleShape)
                    .background(if (caja != null && i < caja) color.fuerte else color.fuerte.copy(alpha = 0.2f))
            )
        }
        Text(
            textoCaja(caja), style = MaterialTheme.typography.labelSmall, color = color.contenido,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/** Vidas del Reto diario como corazones (con descripción para lectores de pantalla). */
@Composable
fun Vidas(vidas: Int, modifier: Modifier = Modifier, maximo: Int = 3) {
    Row(
        modifier.semantics { contentDescription = "$vidas de $maximo vidas" },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(maximo) { i ->
            Icon(
                if (i < vidas) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (i < vidas) Paleta.Error else Paleta.TextoSecundario.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Píldora pequeña de color con ícono, para XP, combos y avisos de modo. */
@Composable
fun Pildora(texto: String, color: ColorFlashly, modifier: Modifier = Modifier, icono: ImageVector? = null, fondo: Color = color.fondo) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icono != null) Icon(icono, contentDescription = null, tint = color.contenido, modifier = Modifier.size(16.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color.contenido)
    }
}

/** "1:05" a partir de segundos. */
fun formatoTiempo(segundos: Int): String = "%d:%02d".format(segundos / 60, segundos % 60)
