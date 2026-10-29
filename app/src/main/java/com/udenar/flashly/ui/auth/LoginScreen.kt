package com.udenar.flashly.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.R
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.errorCorreo
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    var correo by mutableStateOf(""); private set
    var clave by mutableStateOf(""); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var intento by mutableStateOf(false); private set

    fun cambiarCorreo(t: String) { correo = t.trim(); error = null }
    fun cambiarClave(t: String) { clave = t; error = null }

    val errorCorreoVisible get() = if (intento) errorCorreo(correo) else null
    val errorClaveVisible get() = if (intento && clave.isEmpty()) "Escribe tu contraseña" else null

    fun entrar() {
        intento = true
        if (errorCorreo(correo) != null || clave.isEmpty()) return
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                // Si funciona, el estado de sesión cambia y la app abre la pantalla principal sola
                CuentaRepository.iniciarSesion(correo, clave)
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            cargando = false
        }
    }
}

/** Pantalla 3: inicio de sesión con correo y contraseña. */
@Composable
fun LoginScreen(onVolver: () -> Unit, onCrearCuenta: () -> Unit, vm: LoginViewModel = viewModel()) {
    Box(Modifier.fillMaxSize()) {
        FondoDecorado(intensidad = 0.7f)
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EncabezadoAuth("¡Hola de nuevo!", "Inicia sesión para seguir aprendiendo.", onVolver)
            Image(
                painterResource(R.drawable.logo_flashly), contentDescription = "Flashly",
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp)
            )
            CampoTexto(
                vm.correo, vm::cambiarCorreo, "Correo electrónico",
                icono = Icons.Rounded.Mail, marcador = "tu@correo.com",
                error = vm.errorCorreoVisible, tipoTeclado = KeyboardType.Email
            )
            CampoTexto(
                vm.clave, vm::cambiarClave, "Contraseña",
                icono = Icons.Rounded.Lock, esContrasena = true,
                error = vm.errorClaveVisible,
                accionTeclado = ImeAction.Done, alAccion = vm::entrar
            )
            MensajeAviso(vm.error, TipoMensaje.Error)
            BotonFlashly(
                if (vm.cargando) "Entrando…" else "Iniciar sesión",
                vm::entrar, Modifier.fillMaxWidth().padding(top = 4.dp), cargando = vm.cargando
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
                BotonFlashly("Crear cuenta", onCrearCuenta, estilo = EstiloBoton.Texto)
            }
        }
    }
}
