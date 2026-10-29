package com.udenar.flashly.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.Perfil
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.theme.Paleta
import com.udenar.flashly.ui.theme.colorDeHex
import kotlinx.coroutines.launch

class CuentaCreadaViewModel : ViewModel() {
    var perfil by mutableStateOf<Perfil?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    init { cargar() }

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                perfil = CuentaRepository.miPerfil()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }
}

/** Registro completado: muestra el perfil recién creado por el trigger, con su avatar asignado al azar. */
@Composable
fun CuentaCreadaScreen(onEmpezar: () -> Unit, vm: CuentaCreadaViewModel = viewModel()) {
    Box(Modifier.fillMaxSize()) {
        FondoDecorado()
        val perfil = vm.perfil
        when {
            vm.error != null -> EstadoError(vm.error.orEmpty(), vm::cargar, Modifier.align(Alignment.Center))
            perfil == null -> EstadoCarga(Modifier.align(Alignment.Center), "Preparando tu perfil…")
            else -> Contenido(perfil, onEmpezar)
        }
    }
}

@Composable
private fun Contenido(perfil: Perfil, onEmpezar: () -> Unit) {
    val escala = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { escala.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 180f)) }
    val brillo = rememberInfiniteTransition(label = "brillo")
    val giro by brillo.animateFloat(-12f, 12f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "giro")

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            val avatar = perfil.avatar
            AvatarCirculo(
                url = avatar?.let { SupabaseProvider.urlAvatar(it) },
                colorFondo = colorDeHex(avatar?.color),
                tamano = 168.dp,
                iniciales = perfil.nombreUsuario,
                descripcion = "Tu avatar",
                modifier = Modifier.graphicsLayer { scaleX = escala.value; scaleY = escala.value }
            )
            Icon(
                Icons.Rounded.AutoAwesome, contentDescription = null, tint = Paleta.Mantequilla,
                modifier = Modifier.size(40.dp).offset(x = 84.dp, y = (-70).dp).graphicsLayer { rotationZ = giro }
            )
            Icon(
                Icons.Rounded.AutoAwesome, contentDescription = null, tint = Paleta.RosaPastel,
                modifier = Modifier.size(28.dp).offset(x = (-88).dp, y = 60.dp).graphicsLayer { rotationZ = -giro }
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("¡Cuenta creada!", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Hola, @${perfil.nombreUsuario}", style = MaterialTheme.typography.titleLarge, color = Paleta.VerdeAzuladoOscuro)
        Spacer(Modifier.height(12.dp))
        Text(
            "Te asignamos este avatar al azar. Puedes cambiarlo cuando quieras desde tu perfil.",
            style = MaterialTheme.typography.bodyLarge, color = Paleta.TextoSecundario, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        BotonFlashly("Empezar a estudiar", onEmpezar, Modifier.fillMaxWidth())
    }
}
