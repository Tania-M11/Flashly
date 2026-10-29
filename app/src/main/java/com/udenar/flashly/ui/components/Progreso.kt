package com.udenar.flashly.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.Paleta

/** Barra de progreso gruesa y redondeada que se anima al cambiar de valor. */
@Composable
fun BarraProgreso(
    progreso: Float,
    modifier: Modifier = Modifier,
    color: Color = Paleta.VerdeAzulado,
    fondo: Color = Paleta.Menta.copy(alpha = 0.55f),
    alto: Dp = 12.dp,
) {
    val valor by animateFloatAsState(progreso.coerceIn(0f, 1f), spring(dampingRatio = 0.8f), label = "progreso")
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(CircleShape)
            .background(fondo)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progreso.coerceIn(0f, 1f), 0f..1f) }
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(valor).clip(CircleShape).background(color))
    }
}

/** Progreso con texto, p. ej. «4 de 20» a la izquierda y el porcentaje a la derecha. */
@Composable
fun ProgresoConTexto(actual: Int, total: Int, modifier: Modifier = Modifier, color: Color = Paleta.VerdeAzulado) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$actual de $total", style = MaterialTheme.typography.labelLarge)
            Text(
                if (total > 0) "${actual * 100 / total} %" else "0 %",
                style = MaterialTheme.typography.labelLarge, color = Paleta.TextoSecundario
            )
        }
        BarraProgreso(if (total > 0) actual.toFloat() / total else 0f, color = color)
    }
}
