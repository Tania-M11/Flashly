package com.udenar.flashly.ui.historial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udenar.flashly.ui.theme.Paleta

/**
 * Gráfica de barras con el valor encima de cada barra y la etiqueta debajo,
 * para no depender solo del color ni de la altura.
 */
@Composable
fun GraficaBarras(valores: List<Int>, etiquetas: List<String>, color: Color, descripcion: String, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val estiloValor = TextStyle(fontSize = 11.sp, color = Paleta.Texto)
    val estiloEtiqueta = TextStyle(fontSize = 11.sp, color = Paleta.TextoSecundario)
    val maximo = (valores.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = descripcion }) {
        val altoTexto = 18.dp.toPx()
        val altoUtil = size.height - altoTexto * 2
        val ancho = size.width / valores.size.coerceAtLeast(1)
        val anchoBarra = (ancho * 0.56f).coerceAtMost(36.dp.toPx())
        valores.forEachIndexed { i, v ->
            val cx = ancho * i + ancho / 2
            val alto = if (v > 0) (altoUtil * v / maximo).coerceAtLeast(4.dp.toPx()) else 0f
            val base = altoTexto + altoUtil
            // Fondo suave de la columna
            drawRoundRect(
                color.copy(alpha = 0.15f), Offset(cx - anchoBarra / 2, altoTexto), Size(anchoBarra, altoUtil),
                CornerRadius(8.dp.toPx())
            )
            if (alto > 0) {
                drawRoundRect(color, Offset(cx - anchoBarra / 2, base - alto), Size(anchoBarra, alto), CornerRadius(8.dp.toPx()))
                val t = medidor.measure(v.toString(), estiloValor)
                drawText(t, topLeft = Offset(cx - t.size.width / 2, (base - alto - t.size.height - 2).coerceAtLeast(0f)))
            }
            val e = medidor.measure(etiquetas.getOrElse(i) { "" }, estiloEtiqueta)
            drawText(e, topLeft = Offset(cx - e.size.width / 2, base + 4.dp.toPx()))
        }
    }
}

/** Gráfica de línea (0-100) con cada punto rotulado con su valor. */
@Composable
fun GraficaLinea(valores: List<Int>, etiquetas: List<String>, color: Color, descripcion: String, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val estiloValor = TextStyle(fontSize = 11.sp, color = Paleta.Texto)
    val estiloEtiqueta = TextStyle(fontSize = 10.sp, color = Paleta.TextoSecundario)
    Canvas(modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = descripcion }) {
        val margenX = 16.dp.toPx()
        val arriba = 20.dp.toPx()
        val abajo = size.height - 20.dp.toPx()
        // Líneas guía en 0, 50 y 100 %
        listOf(0, 50, 100).forEach { nivel ->
            val y = abajo - (abajo - arriba) * nivel / 100f
            drawLine(Paleta.Borde, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (valores.isEmpty()) return@Canvas
        val paso = if (valores.size > 1) (size.width - margenX * 2) / (valores.size - 1) else 0f
        val puntos = valores.mapIndexed { i, v ->
            Offset(if (valores.size > 1) margenX + paso * i else size.width / 2, abajo - (abajo - arriba) * v / 100f)
        }
        val camino = Path().apply {
            puntos.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(camino, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        puntos.forEachIndexed { i, p ->
            drawCircle(Color.White, 6.dp.toPx(), p)
            drawCircle(color, 4.dp.toPx(), p)
            // Con muchos puntos se rotulan uno sí y otro no para que no se encimen
            if (valores.size <= 8 || i % 2 == 0 || i == puntos.lastIndex) {
                val t = medidor.measure("${valores[i]}%", estiloValor)
                drawText(t, topLeft = Offset(p.x - t.size.width / 2, (p.y - t.size.height - 6.dp.toPx()).coerceAtLeast(0f)))
                val e = medidor.measure(etiquetas.getOrElse(i) { "" }, estiloEtiqueta)
                drawText(e, topLeft = Offset((p.x - e.size.width / 2).coerceIn(0f, size.width - e.size.width), abajo + 4.dp.toPx()))
            }
        }
    }
}
