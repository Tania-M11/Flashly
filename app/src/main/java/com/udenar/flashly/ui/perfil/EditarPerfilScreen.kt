package com.udenar.flashly.ui.perfil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.Avatar
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.data.errorNombre
import com.udenar.flashly.data.errorTelefono
import com.udenar.flashly.data.errorUsuario
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.normalizarUsuario
import com.udenar.flashly.ui.auth.EstadoUsuario
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.theme.Paleta
import com.udenar.flashly.ui.theme.colorDeHex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditarPerfilViewModel : ViewModel() {
    var cargado by mutableStateOf(false); private set
    var errorCarga by mutableStateOf<String?>(null); private set
    var avatares by mutableStateOf<List<Avatar>>(emptyList()); private set

    // Valores originales, para saber si hay cambios sin guardar
    private var original = Triple("", "", "")
    private var avatarOriginal = ""

    var nombre by mutableStateOf(""); private set
    var usuario by mutableStateOf(""); private set
    var telefono by mutableStateOf(""); private set
    var avatarId by mutableStateOf(""); private set
    var estadoUsuario by mutableStateOf(EstadoUsuario.Disponible); private set
    var guardando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var exito by mutableStateOf<String?>(null); private set
    private var comprobacion: Job? = null

    val avatarActual get() = avatares.firstOrNull { it.id == avatarId }
    val hayCambios get() = Triple(nombre.trim(), normalizarUsuario(usuario), telefono.trim()) != original || avatarId != avatarOriginal

    init { cargar() }

    fun cargar() {
        errorCarga = null
        viewModelScope.launch {
            try {
                val p = CuentaRepository.miPerfil()
                avatares = CuentaRepository.avatares()
                nombre = p.nombreCompleto; usuario = p.nombreUsuario; telefono = p.telefono.orEmpty(); avatarId = p.avatarId
                original = Triple(p.nombreCompleto, p.nombreUsuario, p.telefono.orEmpty())
                avatarOriginal = p.avatarId
                cargado = true
            } catch (e: Exception) {
                errorCarga = mensajeError(e)
            }
        }
    }

    fun cambiarNombre(t: String) { nombre = t.take(80); exito = null }
    fun cambiarTelefono(t: String) { telefono = t.filter { it.isDigit() || it == ' ' || it == '+' }.take(20); exito = null }
    fun elegirAvatar(id: String) { avatarId = id; exito = null }

    /** Igual que en el registro, pero el nombre propio actual cuenta como disponible. */
    fun cambiarUsuario(t: String) {
        usuario = t.replace(" ", "").take(20)
        exito = null
        comprobacion?.cancel()
        estadoUsuario = when {
            usuario.isEmpty() -> EstadoUsuario.Vacio
            errorUsuario(usuario) != null -> EstadoUsuario.Invalido
            normalizarUsuario(usuario) == original.second -> EstadoUsuario.Disponible
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

    val errorNombreVisible get() = errorNombre(nombre)
    val errorTelefonoVisible get() = errorTelefono(telefono)
    val errorUsuarioVisible get() = when (estadoUsuario) {
        EstadoUsuario.Ocupado -> "Ese nombre ya está en uso, prueba otro"
        EstadoUsuario.Invalido, EstadoUsuario.Vacio -> errorUsuario(usuario)
        else -> null
    }

    fun guardar() {
        error = null
        exito = null
        if (errorNombreVisible != null || errorTelefonoVisible != null || errorUsuario(usuario) != null || estadoUsuario == EstadoUsuario.Ocupado) {
            error = "Revisa los campos marcados en rojo"
            return
        }
        viewModelScope.launch {
            guardando = true
            try {
                val p = CuentaRepository.actualizarPerfil(nombre, usuario, telefono, avatarId)
                original = Triple(p.nombreCompleto, p.nombreUsuario, p.telefono.orEmpty())
                avatarOriginal = p.avatarId
                usuario = p.nombreUsuario
                exito = "¡Cambios guardados!"
            } catch (e: Exception) {
                error = mensajeError(e)
                if ("usuario ya está en uso" in error.orEmpty()) estadoUsuario = EstadoUsuario.Ocupado
            }
            guardando = false
        }
    }
}

/** Pantalla 15: editar perfil. El avatar se elige de la colección predefinida en un panel inferior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(onVolver: () -> Unit, onCambiarContrasena: () -> Unit, vm: EditarPerfilViewModel = viewModel()) {
    var selector by remember { mutableStateOf(false) }
    var descartar by remember { mutableStateOf(false) }
    val salir = { if (vm.hayCambios) descartar = true else onVolver() }
    BackHandler(enabled = vm.hayCambios) { descartar = true }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla("Editar perfil", salir)
        when {
            vm.errorCarga != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoError(vm.errorCarga.orEmpty(), vm::cargar) }
            !vm.cargado -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoCarga() }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Avatar
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val a = vm.avatarActual
                    AvatarCirculo(
                        url = a?.let { SupabaseProvider.urlAvatar(it) }, colorFondo = colorDeHex(a?.color),
                        tamano = 128.dp, iniciales = vm.usuario, descripcion = "Avatar seleccionado"
                    )
                    BotonFlashly("Cambiar avatar", { selector = true }, estilo = EstiloBoton.Suave, icono = Icons.Rounded.Face)
                }

                // 2. Información personal
                Text("Información personal", style = MaterialTheme.typography.titleLarge)
                CampoTexto(
                    vm.nombre, vm::cambiarNombre, "Nombre completo", icono = Icons.Rounded.Badge,
                    error = vm.errorNombreVisible, mayusculas = KeyboardCapitalization.Words
                )
                CampoTexto(
                    vm.usuario, vm::cambiarUsuario, "Nombre de usuario", icono = Icons.Rounded.AlternateEmail,
                    error = vm.errorUsuarioVisible,
                    valido = vm.estadoUsuario == EstadoUsuario.Disponible,
                    ayuda = when (vm.estadoUsuario) {
                        EstadoUsuario.Comprobando -> "Comprobando disponibilidad…"
                        EstadoUsuario.SinVerificar -> "No pudimos comprobarlo ahora; lo verificaremos al guardar"
                        else -> "Así te verán dentro de Flashly"
                    }
                )
                CampoTexto(
                    vm.telefono, vm::cambiarTelefono, "Teléfono", icono = Icons.Rounded.Phone,
                    error = vm.errorTelefonoVisible, tipoTeclado = KeyboardType.Phone
                )
                CampoTexto(
                    CuentaRepository.correoActual().orEmpty(), {}, "Correo electrónico", icono = Icons.Rounded.Mail,
                    habilitado = false,
                    ayuda = "Cambiar el correo requiere enviar un correo de confirmación; se habilitará más adelante."
                )
                CampoTexto(
                    "••••••••", {}, "Contraseña", icono = Icons.Rounded.Lock, habilitado = false
                )
                BotonFlashly("Cambiar contraseña", onCambiarContrasena, estilo = EstiloBoton.Texto)

                // 3. Acciones
                MensajeAviso(vm.error, TipoMensaje.Error)
                MensajeAviso(vm.exito, TipoMensaje.Exito)
                BotonFlashly(
                    if (vm.guardando) "Guardando…" else "Guardar cambios", vm::guardar,
                    Modifier.fillMaxWidth(), cargando = vm.guardando, habilitado = vm.hayCambios
                )
                BotonFlashly("Volver", salir, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (selector) {
        SelectorAvatar(
            avatares = vm.avatares, actual = vm.avatarId,
            onConfirmar = { vm.elegirAvatar(it); selector = false },
            onCancelar = { selector = false }
        )
    }
    if (descartar) {
        DialogoConfirmacion(
            titulo = "¿Descartar cambios?",
            mensaje = "Tienes cambios sin guardar en tu perfil. Si sales ahora se perderán.",
            textoConfirmar = "Descartar", textoCancelar = "Seguir editando",
            onConfirmar = { descartar = false; onVolver() },
            onCancelar = { descartar = false }
        )
    }
}

/** Panel inferior con la colección de avatares; el elegido se marca con borde y check antes de confirmar. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SelectorAvatar(avatares: List<Avatar>, actual: String, onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    var elegido by remember { mutableStateOf(actual) }
    ModalBottomSheet(
        onDismissRequest = onCancelar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Paleta.Fondo,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Elige tu avatar", style = MaterialTheme.typography.headlineSmall)
            Text("Toca uno para seleccionarlo y confirma tu elección.", style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 3
            ) {
                avatares.forEachIndexed { i, a ->
                    AvatarCirculo(
                        url = SupabaseProvider.urlAvatar(a), colorFondo = colorDeHex(a.color), tamano = 88.dp,
                        seleccionado = a.id == elegido, descripcion = "Avatar ${i + 1}",
                        rolToque = Role.RadioButton, onClick = { elegido = a.id }
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                BotonFlashly("Confirmar selección", { onConfirmar(elegido) }, Modifier.fillMaxWidth())
                BotonFlashly("Cancelar", onCancelar, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
            }
        }
    }
}
