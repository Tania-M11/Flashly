package com.udenar.flashly.ui.estudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.EstudioRepository
import com.udenar.flashly.data.InfoModos
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstadoVacio
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.launch

class ModosViewModel : ViewModel() {
    var nombre by mutableStateOf(""); private set
    var info by mutableStateOf<InfoModos?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar(mazoId: String) {
        error = null
        viewModelScope.launch {
            try {
                if (nombre.isEmpty()) nombre = MazosRepository.mazo(mazoId).nombre
                info = EstudioRepository.infoModos(mazoId)
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }
}

/** Pantalla 10: elegir uno de los 4 modos de juego, con sus reglas visibles antes de empezar. */
@Composable
fun ModosScreen(mazoId: String, onVolver: () -> Unit, onElegir: (ModoJuego) -> Unit, vm: ModosViewModel = viewModel()) {
    // Se recarga al volver de una sesión: cambian los pendientes de repaso y el XP doble
    LifecycleResumeEffect(mazoId) {
        vm.cargar(mazoId)
        onPauseOrDispose { }
    }
    val info = vm.info

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("¿Cómo quieres estudiar?", onVolver)
        when {
            vm.error != null && info == null -> EstadoError(vm.error!!, { vm.cargar(mazoId) })
            info == null -> EstadoCarga()
            info.total == 0 -> EstadoVacio(
                "Este mazo está vacío",
                "Agrega preguntas o definiciones para poder estudiarlo.",
                textoAccion = "Volver", onAccion = onVolver
            )
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(vm.nombre, style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        append(if (info.total == 1) "1 tarjeta" else "${info.total} tarjetas")
                        if (info.preguntas > 0) append(" · ${info.preguntas} de selección")
                        if (info.definiciones > 0) append(" · ${info.definiciones} de definición")
                    },
                    style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario
                )
                ModoJuego.entries.forEach { modo ->
                    val (extra, disponible) = when (modo) {
                        ModoJuego.REPASO ->
                            if (info.pendientesRepaso > 0) "${info.pendientesRepaso} pendientes hoy · ${info.dominadas} dominadas" to true
                            else "Nada pendiente hoy. ¡Vuelve mañana!" to false
                        ModoJuego.RETO ->
                            (if (info.xpDobleDisponible) "XP doble disponible hoy" else "Ya ganaste el XP doble de hoy") to true
                        else -> null to true
                    }
                    TarjetaModo(modo, extra, disponible) { onElegir(modo) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaModo(modo: ModoJuego, extra: String?, disponible: Boolean, onClick: () -> Unit) {
    val estilo = estiloModo(modo)
    val c = estilo.color
    TarjetaFlashly(
        Modifier.fillMaxWidth().alpha(if (disponible) 1f else 0.6f),
        color = c.fondo, esquinas = 28.dp,
        onClick = if (disponible) onClick else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Paleta.Blanco.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Icon(estilo.icono, contentDescription = null, tint = c.fuerte, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(modo.titulo, style = MaterialTheme.typography.titleLarge, color = c.contenido)
                Text(estilo.descripcion, style = MaterialTheme.typography.bodySmall, color = c.contenido.copy(alpha = 0.85f))
            }
            if (disponible) Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = c.contenido)
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            estilo.reglas.forEach { regla ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.fuerte, modifier = Modifier.size(16.dp))
                    Text(regla, style = MaterialTheme.typography.bodySmall, color = c.contenido)
                }
            }
        }
        if (extra != null) {
            Pildora(
                extra, c, Modifier.padding(top = 12.dp),
                icono = if (modo == ModoJuego.RETO) Icons.Rounded.Bolt else Icons.Rounded.Schedule,
                fondo = Paleta.Blanco.copy(alpha = 0.75f)
            )
        }
    }
}
