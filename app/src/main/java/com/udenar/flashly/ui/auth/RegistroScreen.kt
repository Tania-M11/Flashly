package com.udenar.flashly.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.errorConfirmacion
import com.udenar.flashly.data.errorCorreo
import com.udenar.flashly.data.errorNombre
import com.udenar.flashly.data.errorTelefono
import com.udenar.flashly.data.errorUsuario
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.requisitosContrasena
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Estado de la comprobación en vivo del nombre de usuario. */
enum class EstadoUsuario { Vacio, Invalido, Comprobando, Disponible, Ocupado, SinVerificar }

/** Marca que la sesión viene de un registro recién hecho, para mostrar la pantalla «¡Cuenta creada!». */
object RegistroReciente {
    var activo by mutableStateOf(false)
}

class RegistroViewModel : ViewModel() {
    var nombre by mutableStateOf(""); private set
    var usuario by mutableStateOf(""); private set
    var telefono by mutableStateOf(""); private set
    var correo by mutableStateOf(""); private set
    var clave by mutableStateOf(""); private set
    var repetida by mutableStateOf(""); private set
    var estadoUsuario by mutableStateOf(EstadoUsuario.Vacio); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var aviso by mutableStateOf<String?>(null); private set
    /** Tras el primer intento de enviar se muestran todos los errores, también los de campos vacíos. */
    var intento by mutableStateOf(false); private set

    private var comprobacion: Job? = null

    val requisitos get() = requisitosContrasena(clave)

    fun cambiarNombre(t: String) { nombre = t.take(80) }
    fun cambiarTelefono(t: String) { telefono = t.filter { it.isDigit() || it == ' ' || it == '+' }.take(20) }
    fun cambiarCorreo(t: String) { correo = t.trim().take(120) }
    fun cambiarClave(t: String) { clave = t.take(72) }
    fun cambiarRepetida(t: String) { repetida = t.take(72) }

    /** Valida el formato al instante y consulta la disponibilidad 0,5 s después de dejar de escribir. */
    fun cambiarUsuario(t: String) {
        usuario = t.replace(" ", "").take(20)
        comprobacion?.cancel()
        estadoUsuario = when {
            usuario.isEmpty() -> EstadoUsuario.Vacio
            errorUsuario(usuario) != null -> EstadoUsuario.Invalido
            else -> EstadoUsuario.Comprobando
        }
        if (estadoUsuario != EstadoUsuario.Comprobando) return
        comprobacion = viewModelScope.launch {
            delay(500)
            estadoUsuario = try {
                if (CuentaRepository.nombreUsuarioDisponible(usuario)) EstadoUsuario.Disponible else EstadoUsuario.Ocupado
            } catch (e: Exception) {
                EstadoUsuario.SinVerificar
            }
        }
    }

    /** Muestra el error de un campo si ya tiene texto o si se intentó enviar el formulario. */
    private fun visible(valor: String, mensaje: String?) = if (intento || valor.isNotEmpty()) mensaje else null

    val errorNombreVisible get() = visible(nombre, errorNombre(nombre))
    val errorTelefonoVisible get() = visible(telefono, errorTelefono(telefono))
    val errorCorreoVisible get() = visible(correo, errorCorreo(correo))
    val errorRepetidaVisible get() = visible(repetida, errorConfirmacion(clave, repetida))
    val errorClaveVisible get() = if (intento && !requisitos.cumpleTodos) "La contraseña no cumple todos los requisitos" else null
    val errorUsuarioVisible get() = when (estadoUsuario) {
        EstadoUsuario.Ocupado -> "Ese nombre ya está en uso, prueba otro"
        EstadoUsuario.Invalido -> errorUsuario(usuario)
        EstadoUsuario.Vacio -> if (intento) errorUsuario(usuario) else null
        else -> null
    }

    fun crearCuenta() {
        intento = true
        error = null
        val valido = errorNombre(nombre) == null && errorUsuario(usuario) == null &&
            estadoUsuario != EstadoUsuario.Ocupado && errorTelefono(telefono) == null &&
            errorCorreo(correo) == null && requisitos.cumpleTodos && errorConfirmacion(clave, repetida) == null
        if (!valido) {
            error = "Revisa los campos marcados en rojo"
            return
        }
        viewModelScope.launch {
            cargando = true
            // Se activa antes del registro: al crearse la sesión la app pasa enseguida a la pantalla principal
            RegistroReciente.activo = true
            try {
                val sesionIniciada = CuentaRepository.registrar(nombre, usuario, telefono, correo, clave)
                if (!sesionIniciada) {
                    RegistroReciente.activo = false
                    aviso = "Cuenta creada. Revisa tu correo para confirmarla y luego inicia sesión."
                }
            } catch (e: Exception) {
                RegistroReciente.activo = false
                error = mensajeError(e)
                if ("usuario ya está en uso" in error.orEmpty()) estadoUsuario = EstadoUsuario.Ocupado
            }
            cargando = false
        }
    }
}

/** Pantalla 2: registro con nombre, usuario único, teléfono, correo y contraseña segura. */
@Composable
fun RegistroScreen(onVolver: () -> Unit, onIrALogin: () -> Unit, vm: RegistroViewModel = viewModel()) {
    Box(Modifier.fillMaxSize()) {
        FondoDecorado(intensidad = 0.6f)
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EncabezadoAuth("Crea tu cuenta", "Solo te tomará un minuto. Así te reconocerán en Flashly.", onVolver)

            Text("Tus datos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            CampoTexto(
                vm.nombre, vm::cambiarNombre, "Nombre completo",
                icono = Icons.Rounded.Badge, marcador = "Laura Gómez",
                error = vm.errorNombreVisible, valido = vm.nombre.isNotEmpty() && vm.errorNombreVisible == null,
                mayusculas = KeyboardCapitalization.Words
            )
            CampoTexto(
                vm.usuario, vm::cambiarUsuario, "Nombre de usuario",
                icono = Icons.Rounded.AlternateEmail, marcador = "laura_g",
                error = vm.errorUsuarioVisible,
                valido = vm.estadoUsuario == EstadoUsuario.Disponible,
                ayuda = when (vm.estadoUsuario) {
                    EstadoUsuario.Comprobando -> "Comprobando disponibilidad…"
                    EstadoUsuario.Disponible -> "¡Disponible! Así te verán dentro de Flashly"
                    EstadoUsuario.SinVerificar -> "No pudimos comprobarlo ahora; lo verificaremos al crear la cuenta"
                    else -> "Entre 3 y 20 caracteres: letras, números, punto o guion bajo"
                }
            )
            CampoTexto(
                vm.telefono, vm::cambiarTelefono, "Teléfono",
                icono = Icons.Rounded.Phone, marcador = "300 123 4567",
                error = vm.errorTelefonoVisible, valido = vm.telefono.isNotEmpty() && vm.errorTelefonoVisible == null,
                tipoTeclado = KeyboardType.Phone
            )

            Text("Acceso", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            CampoTexto(
                vm.correo, vm::cambiarCorreo, "Correo electrónico",
                icono = Icons.Rounded.Mail, marcador = "tu@correo.com",
                error = vm.errorCorreoVisible, valido = vm.correo.isNotEmpty() && vm.errorCorreoVisible == null,
                tipoTeclado = KeyboardType.Email
            )
            CampoTexto(
                vm.clave, vm::cambiarClave, "Contraseña",
                icono = Icons.Rounded.Lock, esContrasena = true,
                error = vm.errorClaveVisible
            )
            AnimatedVisibility(vm.clave.isNotEmpty() || vm.intento) {
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
                vm.repetida, vm::cambiarRepetida, "Repetir contraseña",
                icono = Icons.Rounded.LockReset, esContrasena = true,
                error = vm.errorRepetidaVisible,
                valido = vm.repetida.isNotEmpty() && vm.errorRepetidaVisible == null,
                accionTeclado = ImeAction.Done, alAccion = vm::crearCuenta
            )

            MensajeAviso(vm.error, TipoMensaje.Error)
            MensajeAviso(vm.aviso, TipoMensaje.Info)
            BotonFlashly(
                if (vm.cargando) "Creando tu cuenta…" else "Crear cuenta",
                vm::crearCuenta, Modifier.fillMaxWidth(), cargando = vm.cargando
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
                BotonFlashly("Inicia sesión", onIrALogin, estilo = EstiloBoton.Texto)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
