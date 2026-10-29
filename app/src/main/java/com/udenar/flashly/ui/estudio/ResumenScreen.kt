package com.udenar.flashly.ui.estudio

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.EstudioRepository
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.ResumenSesion
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.TarjetaDato
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TituloSeccion
import com.udenar.flashly.ui.fechaLarga
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.launch

/**
 * Contenido del resumen de una sesión (pantalla 12 y detalle desde el historial).
 * La puntuación del cuestionario y la autoevaluación de definiciones se muestran por separado.
 */
@Composable
fun ResumenContenido(r: ResumenSesion, modifier: Modifier = Modifier, acciones: @Composable ColumnScope.() -> Unit) {
    val estilo = estiloModo(r.modoJuego)
    val (icono, titulo) = when {
        r.estado == "fallida" -> Icons.Rounded.HeartBroken to "Te quedaste sin vidas"
        r.estado == "tiempo_agotado" -> Icons.Rounded.Timer to "¡Se acabó el tiempo!"
        r.estado == "abandonada" -> Icons.Rounded.Cancel to "Sesión sin terminar"
        r.modoJuego == ModoJuego.RETO -> Icons.Rounded.EmojiEvents to "¡Reto superado!"
        r.puntuacion == 100 -> Icons.Rounded.EmojiEvents to "¡Sesión perfecta!"
        else -> Icons.Rounded.TaskAlt to "¡Sesión completada!"
    }

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(estilo.color.fondo), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = estilo.color.fuerte, modifier = Modifier.size(52.dp))
            }
            Text(titulo, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
            Text(
                "${r.mazoNombre} · ${r.modoJuego.titulo}", style = MaterialTheme.typography.bodyMedium,
                color = Paleta.TextoSecundario, textAlign = TextAlign.Center
            )
        }

        TarjetaXp(r)

        // Cuestionario: solo si hubo preguntas evaluables
        val tiles = buildList {
            add(Triple(if (r.totalTarjetas > 0) "${r.revisadas}/${r.totalTarjetas}" else "${r.revisadas}", "Tarjetas revisadas", Icons.Rounded.Style))
            r.puntuacion?.let { add(Triple("$it%", "Puntuación", Icons.Rounded.Percent)) }
            if (r.preguntas > 0) {
                add(Triple("${r.aciertos}", "Correctas", Icons.Rounded.CheckCircle))
                add(Triple("${r.errores}", "Incorrectas", Icons.Rounded.Cancel))
            }
            if (r.modoJuego == ModoJuego.CONTRARRELOJ) add(Triple("×${r.mejorCombo}", "Mejor combo", Icons.Rounded.Bolt))
            if (r.modoJuego == ModoJuego.RETO) add(Triple("${r.vidas ?: 0} de 3", "Vidas restantes", Icons.Rounded.Favorite))
        }
        val colores = listOf(ColorFlashly.MENTA, ColorFlashly.LAVANDA, ColorFlashly.MENTA, ColorFlashly.ROSA, ColorFlashly.MELOCOTON, ColorFlashly.ROSA)
        tiles.chunked(2).forEachIndexed { fila, par ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                par.forEachIndexed { i, (valor, etiqueta, ic) ->
                    TarjetaDato(valor, etiqueta, colores[(fila * 2 + i) % colores.size], ic, Modifier.weight(1f))
                }
                if (par.size == 1) Box(Modifier.weight(1f))
            }
        }

        if (r.definiciones > 0) {
            TituloSeccion("Definiciones")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaDato("${r.loSe}", "Lo sé", ColorFlashly.MENTA, Icons.Rounded.CheckCircle, Modifier.weight(1f))
                TarjetaDato("${r.practicar}", "Necesito practicar", ColorFlashly.LAVANDA, Icons.Rounded.Replay, Modifier.weight(1f))
            }
            Text(
                "Es tu propia valoración: no cuenta en la puntuación del cuestionario.",
                style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario
            )
        }

        if (r.refuerzo.isNotEmpty()) {
            TituloSeccion("Para reforzar")
            TarjetaFlashly(Modifier.fillMaxWidth()) {
                r.refuerzo.forEach { concepto ->
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = ColorFlashly.LAVANDA.fuerte, modifier = Modifier.size(18.dp))
                        Text(concepto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Volverán a aparecer hoy en tu Repaso inteligente.",
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = acciones)
    }
}

/** XP ganado con conteo animado y desglose de bonificaciones. */
@Composable
private fun TarjetaXp(r: ResumenSesion) {
    var objetivo by remember { mutableIntStateOf(0) }
    LaunchedEffect(r.xpTotal) { objetivo = r.xpTotal }
    val mostrado by animateIntAsState(objetivo, tween(900), label = "xp")
    val c = ColorFlashly.MANTEQUILLA
    TarjetaFlashly(Modifier.fillMaxWidth(), color = c.fondo, esquinas = 28.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("XP obtenido", style = MaterialTheme.typography.labelLarge, color = c.contenido)
                Text("+$mostrado XP", style = MaterialTheme.typography.displaySmall, color = c.contenido)
            }
            if (r.xpDoble) Pildora("XP doble ×2", ColorFlashly.ROSA, icono = Icons.Rounded.Bolt, fondo = Paleta.Blanco)
        }
        if (r.xpBono > 0) {
            Text(
                "${r.xp} XP por respuestas + ${r.xpBono} XP de bonificación" + if (r.xpDoble) " (incluye el XP doble del reto)" else "",
                style = MaterialTheme.typography.bodySmall, color = c.contenido, modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

class DetalleSesionViewModel : ViewModel() {
    var resumen by mutableStateOf<ResumenSesion?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar(id: String) {
        error = null
        viewModelScope.launch {
            try {
                resumen = EstudioRepository.resumen(id)
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }
}

/** Detalle de una sesión abierta desde el historial. */
@Composable
fun DetalleSesionScreen(
    sesionId: String,
    onVolver: () -> Unit,
    onEstudiar: (String) -> Unit,
    vm: DetalleSesionViewModel = viewModel(),
) {
    LaunchedEffect(sesionId) { if (vm.resumen == null) vm.cargar(sesionId) }
    val r = vm.resumen
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(fechaLarga(r?.iniciadaEn) ?: "Detalle de la sesión", onVolver)
        when {
            vm.error != null -> EstadoError(vm.error!!, { vm.cargar(sesionId) })
            r == null -> EstadoCarga()
            else -> ResumenContenido(r, Modifier.weight(1f)) {
                r.mazoId?.let { id ->
                    BotonFlashly("Estudiar este mazo", { onEstudiar(id) }, Modifier.fillMaxWidth(), icono = Icons.Rounded.Replay)
                }
            }
        }
    }
}
