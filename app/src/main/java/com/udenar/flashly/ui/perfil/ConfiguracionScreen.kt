package com.udenar.flashly.ui.perfil

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.udenar.flashly.BuildConfig
import com.udenar.flashly.R
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.FilaOpcion
import com.udenar.flashly.ui.components.Separador
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.launch

/** Pantalla 16: configuración básica (sin privacidad: la visibilidad se elige en cada mazo). */
@Composable
fun ConfiguracionScreen(onVolver: () -> Unit, onCambiarContrasena: () -> Unit) {
    var acerca by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("Configuración", onVolver)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Cuenta", style = MaterialTheme.typography.titleLarge)
            TarjetaFlashly(Modifier.fillMaxWidth()) {
                FilaOpcion("Cambiar contraseña", Icons.Rounded.Password, onCambiarContrasena, detalle = "Actualiza tu clave de acceso", color = ColorFlashly.LAVANDA)
            }
            Text("Aplicación", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            TarjetaFlashly(Modifier.fillMaxWidth()) {
                FilaOpcion("Acerca de Flashly", Icons.Rounded.Info, { acerca = true }, detalle = "Versión ${BuildConfig.VERSION_NAME}", color = ColorFlashly.MELOCOTON)
                Separador()
                FilaOpcion("Cerrar sesión", Icons.AutoMirrored.Rounded.Logout, { confirmarSalida = true }, peligro = true, conFlecha = false)
            }
        }
    }

    if (acerca) {
        Dialog(onDismissRequest = { acerca = false }) {
            Surface(shape = RoundedCornerShape(32.dp), color = Paleta.Fondo) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(painterResource(R.drawable.logo_flashly), contentDescription = "Flashly", modifier = Modifier.fillMaxWidth().height(120.dp))
                    Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, color = Paleta.VerdeAzuladoOscuro)
                    Text(
                        "Flashly es una app de tarjetas de aprendizaje: crea mazos con preguntas y definiciones, " +
                            "estúdialos con cuatro modos de juego y sigue tu progreso. Proyecto académico de la " +
                            "Universidad de Nariño que usa Supabase como Backend as a Service.",
                        style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario, textAlign = TextAlign.Center
                    )
                    BotonFlashly("Entendido", { acerca = false }, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
    }
    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            mensaje = "Podrás volver a entrar con tu correo y contraseña cuando quieras.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmarSalida = false; alcance.launch { runCatching { CuentaRepository.cerrarSesion() } } },
            onCancelar = { confirmarSalida = false },
            icono = Icons.AutoMirrored.Rounded.Logout
        )
    }
}
