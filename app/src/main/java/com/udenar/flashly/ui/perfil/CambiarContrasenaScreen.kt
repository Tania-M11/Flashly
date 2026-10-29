package com.udenar.flashly.ui.perfil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.errorConfirmacion
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.requisitosContrasena
import com.udenar.flashly.ui.auth.Requisito
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.launch

class CambiarContrasenaViewModel : ViewModel() {
    var actual by mutableStateOf(""); private set
    var nueva by mutableStateOf(""); private set
    var repetida by mutableStateOf(""); private set
    var intento by mutableStateOf(false); private set
    var guardando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var exito by mutableStateOf<String?>(null); private set

    val requisitos get() = requisitosContrasena(nueva)

    fun cambiarActual(t: String) { actual = t; error = null; exito = null }
    fun cambiarNueva(t: String) { nueva = t.take(72); error = null; exito = null }
    fun cambiarRepetida(t: String) { repetida = t.take(72); error = null; exito = null }

    val errorActualVisible get() = if (intento && actual.isEmpty()) "Escribe tu contraseña actual" else null
    val errorNuevaVisible get() = when {
        intento && !requisitos.cumpleTodos -> "La contraseña no cumple todos los requisitos"
        intento && nueva == actual && nueva.isNotEmpty() -> "La nueva contraseña debe ser distinta a la actual"
        else -> null
    }
    val errorRepetidaVisible get() = if (intento || repetida.isNotEmpty()) errorConfirmacion(nueva, repetida) else null

    fun guardar() {
        intento = true
        if (errorActualVisible != null || errorNuevaVisible != null || errorConfirmacion(nueva, repetida) != null) return
        viewModelScope.launch {
            guardando = true
            error = null
            try {
                CuentaRepository.cambiarContrasena(actual, nueva)
                exito = "¡Listo! Tu contraseña se actualizó."
                actual = ""; nueva = ""; repetida = ""; intento = false
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            guardando = false
        }
    }
}

/** Cambio de contraseña con los mismos requisitos del registro; pide la actual para confirmar. */
@Composable
fun CambiarContrasenaScreen(onVolver: () -> Unit, vm: CambiarContrasenaViewModel = viewModel()) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("Cambiar contraseña", onVolver)
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Por seguridad, primero confirma tu contraseña actual.",
                style = MaterialTheme.typography.bodyLarge, color = Paleta.TextoSecundario
            )
            CampoTexto(vm.actual, vm::cambiarActual, "Contraseña actual", icono = Icons.Rounded.Key, esContrasena = true, error = vm.errorActualVisible)
            CampoTexto(vm.nueva, vm::cambiarNueva, "Nueva contraseña", icono = Icons.Rounded.Lock, esContrasena = true, error = vm.errorNuevaVisible)
            AnimatedVisibility(vm.nueva.isNotEmpty() || vm.intento) {
                val r = vm.requisitos
                TarjetaFlashly(Modifier.fillMaxWidth(), color = Paleta.Crema, esquinas = 20.dp) {
                    Text("Tu contraseña necesita:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Requisito("Mínimo 8 caracteres", r.largo)
                        Requisito("Una letra mayúscula", r.mayuscula)
                        Requisito("Una letra minúscula", r.minuscula)
                        Requisito("Un número", r.numero)
                        Requisito("Un carácter especial (!, #, \$, @…)", r.especial)
                    }
                }
            }
            CampoTexto(
                vm.repetida, vm::cambiarRepetida, "Repetir nueva contraseña", icono = Icons.Rounded.LockReset, esContrasena = true,
                error = vm.errorRepetidaVisible, valido = vm.repetida.isNotEmpty() && vm.errorRepetidaVisible == null,
                accionTeclado = ImeAction.Done, alAccion = vm::guardar
            )
            MensajeAviso(vm.error, TipoMensaje.Error)
            MensajeAviso(vm.exito, TipoMensaje.Exito)
            BotonFlashly(
                if (vm.guardando) "Actualizando…" else "Actualizar contraseña", vm::guardar,
                Modifier.fillMaxWidth(), cargando = vm.guardando
            )
        }
    }
}
