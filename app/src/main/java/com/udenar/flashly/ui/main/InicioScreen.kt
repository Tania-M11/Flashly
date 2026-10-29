package com.udenar.flashly.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.EstudioRepository
import com.udenar.flashly.data.MazoResumen
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.ResumenInicio
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TarjetaDato
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.components.TituloSeccion
import com.udenar.flashly.ui.haceCuanto
import com.udenar.flashly.ui.hoyLegible
import com.udenar.flashly.ui.mazos.TarjetaDeMazo
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import com.udenar.flashly.ui.theme.colorDeHex
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class InicioViewModel : ViewModel() {
    var resumen by mutableStateOf<ResumenInicio?>(null); private set
    var recientes by mutableStateOf<List<MazoResumen>?>(null); private set
    var descubre by mutableStateOf<List<MazoResumen>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                val r = async { EstudioRepository.resumenInicio() }
                val mios = async { MazosRepository.misMazos() }
                val publicos = async { runCatching { MazosRepository.explorar("", null) }.getOrDefault(emptyList()) }
                if (CuentaRepository.perfil.value == null) runCatching { CuentaRepository.miPerfil() }
                resumen = r.await()
                recientes = mios.await().sortedByDescending { it.updatedAt }.take(6)
                descubre = publicos.await().take(6)
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }
}

/**
 * Inicio (sección 5 del documento): saludo, racha y XP, «Continuar estudiando», reto del día,
 * repasos pendientes, mazos recientes, mazos para descubrir y resumen de actividad.
 */
@Composable
fun InicioScreen(
    onPerfil: () -> Unit,
    onCrearMazo: () -> Unit,
    onVerMazos: () -> Unit,
    onExplorar: () -> Unit,
    onHistorial: () -> Unit,
    onAbrirMazo: (String) -> Unit,
    onEstudiar: (String) -> Unit,
    vm: InicioViewModel = viewModel(),
) {
    val perfil by CuentaRepository.perfil.collectAsState()
    LifecycleResumeEffect(Unit) {
        vm.cargar()
        onPauseOrDispose { }
    }
    val r = vm.resumen

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val margen = Modifier.padding(horizontal = 20.dp)
        Row(margen, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val nombre = perfil?.nombreCompleto?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
                Text(nombre?.let { "¡Hola, $it!" } ?: "¡Hola!", style = MaterialTheme.typography.headlineMedium)
                Text(hoyLegible(), style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
            }
            val a = perfil?.avatar
            AvatarCirculo(
                url = a?.let { SupabaseProvider.urlAvatar(it) }, colorFondo = colorDeHex(a?.color), tamano = 56.dp,
                iniciales = perfil?.nombreUsuario.orEmpty(), descripcion = "Ir a mi perfil",
                onClick = onPerfil
            )
        }

        Text(mensajeMotivador(r), style = MaterialTheme.typography.bodyLarge, modifier = margen)
        MensajeAviso(vm.error, TipoMensaje.Error, margen)

        // Racha, XP total y XP de la semana
        Row(margen, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaDato("${r?.racha ?: "–"}", if (r?.racha == 1) "día de racha" else "días de racha", ColorFlashly.MELOCOTON, Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
            TarjetaDato("${r?.xp ?: perfil?.xp ?: "–"}", "XP total", ColorFlashly.MANTEQUILLA, Icons.Rounded.Star, Modifier.weight(1f))
            TarjetaDato("${r?.xpSemana ?: "–"}", "XP esta semana", ColorFlashly.LAVANDA, Icons.Rounded.EmojiEvents, Modifier.weight(1f))
        }

        r?.continuar?.let { c ->
            val color = ColorFlashly.porNombre(c.mazoColor)
            TarjetaFlashly(margen.fillMaxWidth(), color = color.fondo, esquinas = 28.dp, onClick = { onEstudiar(c.mazoId) }) {
                Text("Continuar estudiando", style = MaterialTheme.typography.labelLarge, color = color.contenido)
                Text(c.mazoNombre, style = MaterialTheme.typography.headlineSmall, color = color.contenido, modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    listOfNotNull("Último modo: ${ModoJuego.de(c.modo).titulo}", haceCuanto(c.terminadaEn)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = color.contenido.copy(alpha = 0.85f)
                )
                BotonFlashly("Estudiar", { onEstudiar(c.mazoId) }, Modifier.fillMaxWidth().padding(top = 14.dp), icono = Icons.Rounded.PlayArrow)
            }
        }

        if (r != null && (!r.retoHoy || r.pendientesRepaso > 0)) {
            Row(margen, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!r.retoHoy) {
                    Acceso(
                        "Reto diario", "XP doble en tu primer reto de hoy", ColorFlashly.ROSA, Icons.Rounded.EmojiEvents,
                        Modifier.weight(1f)
                    ) { r.continuar?.let { onEstudiar(it.mazoId) } ?: onVerMazos() }
                }
                if (r.pendientesRepaso > 0) {
                    Acceso(
                        "Para repasar", "${r.pendientesRepaso} tarjetas pendientes hoy", ColorFlashly.LAVANDA, Icons.Rounded.Psychology,
                        Modifier.weight(1f)
                    ) { r.continuar?.let { onEstudiar(it.mazoId) } ?: onVerMazos() }
                }
            }
        }

        BotonFlashly("Crear mazo", onCrearMazo, margen.fillMaxWidth(), icono = Icons.Rounded.Add)

        TituloSeccion("Tus mazos recientes", margen, textoAccion = "Ver todos", onAccion = onVerMazos)
        val recientes = vm.recientes
        if (recientes != null && recientes.isEmpty()) {
            TarjetaFlashly(margen.fillMaxWidth(), color = ColorFlashly.MENTA.fondo.copy(alpha = 0.6f)) {
                Text("Aún no tienes mazos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Un mazo agrupa tus tarjetas de un tema: preguntas de selección y definiciones que giran. " +
                        "Crea el primero o guarda uno de la comunidad.",
                    style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario, modifier = Modifier.padding(vertical = 8.dp)
                )
                BotonFlashly("Explorar mazos públicos", onExplorar, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
            }
        } else {
            FilaMazos(recientes.orEmpty(), mostrarCreador = false, onAbrirMazo)
        }

        if (vm.descubre.isNotEmpty()) {
            TituloSeccion("Descubre nuevos mazos", margen, textoAccion = "Explorar", onAccion = onExplorar)
            FilaMazos(vm.descubre, mostrarCreador = true, onAbrirMazo)
        }

        TituloSeccion("Tu actividad", margen, textoAccion = "Ver progreso", onAccion = onHistorial)
        Row(margen, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaDato("${r?.sesiones ?: "–"}", "Sesiones completadas", ColorFlashly.MENTA, Icons.Rounded.SportsEsports, Modifier.weight(1f))
            TarjetaDato("${r?.tarjetasEstudiadas ?: "–"}", "Tarjetas estudiadas", ColorFlashly.PERIWINKLE, Icons.Rounded.Style, Modifier.weight(1f))
        }
        BotonFlashly("Ver mi progreso", onHistorial, margen.fillMaxWidth(), estilo = EstiloBoton.Secundario, icono = Icons.Rounded.InsertChart)
    }
}

private fun mensajeMotivador(r: ResumenInicio?): String = when {
    r == null -> "Un poco cada día hace la diferencia."
    r.sesiones == 0 -> "Empieza tu primera sesión y gana tus primeros XP."
    r.estudioHoy && r.racha > 1 -> "¡${r.racha} días seguidos! Hoy ya sumaste a tu racha."
    r.estudioHoy -> "¡Bien hecho! Ya estudiaste hoy."
    r.racha > 0 -> "Llevas ${r.racha} ${if (r.racha == 1) "día" else "días"} de racha. Estudia hoy para no perderla."
    else -> "Un repaso corto hoy empieza una nueva racha."
}

@Composable
private fun Acceso(
    titulo: String, texto: String, color: ColorFlashly, icono: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, onClick: () -> Unit,
) {
    TarjetaFlashly(modifier, color = color.fondo, onClick = onClick) {
        Icon(icono, contentDescription = null, tint = color.fuerte)
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = color.contenido, modifier = Modifier.padding(top = 6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = color.contenido.copy(alpha = 0.85f))
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = color.contenido,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        )
    }
}

@Composable
private fun FilaMazos(mazos: List<MazoResumen>, mostrarCreador: Boolean, onAbrir: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(mazos, key = { it.id }) { m ->
            TarjetaDeMazo(m, { onAbrir(m.id) }, Modifier.width(270.dp), mostrarCreador = mostrarCreador, mostrarVisibilidad = !mostrarCreador)
        }
    }
}
