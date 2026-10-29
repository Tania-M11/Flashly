package com.udenar.flashly.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.EstudioRepository
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.BotonCircular
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EtiquetaInfo
import com.udenar.flashly.ui.components.FilaDato
import com.udenar.flashly.ui.components.FilaOpcion
import com.udenar.flashly.ui.components.Separador
import com.udenar.flashly.ui.components.TarjetaDato
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.fechaLarga
import com.udenar.flashly.ui.mesYAnio
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import com.udenar.flashly.ui.theme.colorDeHex
import kotlinx.coroutines.launch

class PerfilViewModel : ViewModel() {
    val perfil = CuentaRepository.perfil
    var mazosCreados by mutableStateOf<Int?>(null); private set
    var partidas by mutableStateOf<Int?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                CuentaRepository.miPerfil()
                mazosCreados = MazosRepository.misMazos().size
                partidas = EstudioRepository.resumenInicio().sesiones
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                CuentaRepository.cerrarSesion()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }
}

/** Pantalla 14: perfil del usuario con su avatar, datos, resumen de actividad y accesos. */
@Composable
fun PerfilScreen(onEditar: () -> Unit, onConfiguracion: () -> Unit, vm: PerfilViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
    val perfil by vm.perfil.collectAsState()
    var confirmarSalida by remember { mutableStateOf(false) }

    val p = perfil
    when {
        p == null && vm.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoError(vm.error.orEmpty(), vm::cargar) }
        p == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoCarga(texto = "Cargando tu perfil…") }
        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mi perfil", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                BotonCircular(Icons.Rounded.Settings, "Configuración", onConfiguracion)
            }

            // Tarjeta principal con el color del avatar
            val colorAvatar = colorDeHex(p.avatar?.color)
            TarjetaFlashly(Modifier.fillMaxWidth(), color = colorAvatar.copy(alpha = 0.45f), esquinas = 32.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarCirculo(
                        url = p.avatar?.let { SupabaseProvider.urlAvatar(it) },
                        colorFondo = colorAvatar, tamano = 112.dp,
                        iniciales = p.nombreUsuario, descripcion = "Tu avatar"
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("@${p.nombreUsuario}", style = MaterialTheme.typography.headlineSmall)
                    Text(p.nombreCompleto, style = MaterialTheme.typography.bodyLarge, color = Paleta.TextoSecundario, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EtiquetaInfo("${p.xp} XP", icono = Icons.Rounded.AutoAwesome)
                        mesYAnio(p.createdAt)?.let { EtiquetaInfo("Desde $it", icono = Icons.Rounded.CalendarMonth) }
                    }
                }
            }

            Text("Resumen de actividad", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaDato("${p.xp}", "XP acumulados", ColorFlashly.MANTEQUILLA, Icons.Rounded.AutoAwesome, Modifier.weight(1f))
                TarjetaDato(vm.mazosCreados?.toString() ?: "–", "Mazos creados", ColorFlashly.LAVANDA, Icons.Rounded.Style, Modifier.weight(1f))
                TarjetaDato(vm.partidas?.toString() ?: "–", "Sesiones", ColorFlashly.MENTA, Icons.Rounded.SportsEsports, Modifier.weight(1f))
            }

            Text("Información personal", style = MaterialTheme.typography.titleLarge)
            TarjetaFlashly(Modifier.fillMaxWidth()) {
                FilaDato("Correo electrónico", CuentaRepository.correoActual() ?: "—", Icons.Rounded.Mail)
                Separador()
                FilaDato("Teléfono", p.telefono ?: "Sin registrar", Icons.Rounded.Phone)
                Separador()
                FilaDato("Fecha de registro", fechaLarga(p.createdAt) ?: "—", Icons.Rounded.CalendarMonth)
            }

            TarjetaFlashly(Modifier.fillMaxWidth()) {
                FilaOpcion("Editar perfil", Icons.Rounded.Edit, onEditar, detalle = "Avatar, nombre, usuario y teléfono", color = ColorFlashly.LAVANDA)
                Separador()
                FilaOpcion("Configuración", Icons.Rounded.Settings, onConfiguracion, detalle = "Contraseña e información de la app", color = ColorFlashly.MELOCOTON)
                Separador()
                FilaOpcion("Cerrar sesión", Icons.AutoMirrored.Rounded.Logout, { confirmarSalida = true }, peligro = true, conFlecha = false)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            mensaje = "Podrás volver a entrar con tu correo y contraseña cuando quieras.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmarSalida = false; vm.cerrarSesion() },
            onCancelar = { confirmarSalida = false },
            icono = Icons.AutoMirrored.Rounded.Logout
        )
    }
}
