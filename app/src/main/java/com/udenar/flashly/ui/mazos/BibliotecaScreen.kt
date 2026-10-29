package com.udenar.flashly.ui.mazos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.MazoResumen
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.BarraBusqueda
import com.udenar.flashly.ui.components.BotonCircular
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstadoVacio
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.FiltroChip
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.launch

enum class FiltroBiblioteca(val etiqueta: String) { Todos("Todos"), Propios("Propios"), Guardados("Guardados") }

class BibliotecaViewModel : ViewModel() {
    var propios by mutableStateOf<List<MazoResumen>>(emptyList()); private set
    var guardados by mutableStateOf<List<MazoResumen>>(emptyList()); private set
    var cargando by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var filtro by mutableStateOf(FiltroBiblioteca.Todos)
    var busqueda by mutableStateOf("")

    /** Mazos del filtro elegido, buscando por nombre sin importar mayúsculas ni tildes. */
    val visibles: List<MazoResumen>
        get() {
            val base = when (filtro) {
                FiltroBiblioteca.Todos -> (propios + guardados).sortedByDescending { it.updatedAt }
                FiltroBiblioteca.Propios -> propios
                FiltroBiblioteca.Guardados -> guardados
            }
            val q = normalizar(busqueda.trim())
            return if (q.isEmpty()) base else base.filter { q in normalizar(it.nombre) }
        }

    fun esPropio(m: MazoResumen) = m.creadorId == MazosRepository.usuarioId()

    fun cargar() {
        viewModelScope.launch {
            error = null
            try {
                propios = MazosRepository.misMazos()
                guardados = MazosRepository.guardados()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            cargando = false
        }
    }

    private fun accion(bloque: suspend () -> Unit) {
        viewModelScope.launch {
            try { bloque(); cargar() } catch (e: Exception) { error = mensajeError(e) }
        }
    }

    fun alternarVisibilidad(m: MazoResumen) = accion { MazosRepository.cambiarVisibilidad(m.id, !m.esPublico) }
    fun eliminar(m: MazoResumen) = accion { MazosRepository.eliminar(m.id) }
    fun quitarGuardado(m: MazoResumen) = accion { MazosRepository.quitarGuardado(m.id) }
}

/** Pantalla 5: biblioteca de mazos (propios y guardados) con buscador, filtros y acceso a Explorar. */
@Composable
fun BibliotecaScreen(
    onAbrir: (String) -> Unit,
    onExplorar: () -> Unit,
    onCrear: () -> Unit,
    onEditar: (String) -> Unit,
    vm: BibliotecaViewModel = viewModel(),
) {
    LifecycleResumeEffect(Unit) {
        vm.cargar()
        onPauseOrDispose { }
    }
    val context = LocalContext.current
    var porBorrar by remember { mutableStateOf<MazoResumen?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mis mazos", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    BotonFlashly("Explorar", onExplorar, estilo = EstiloBoton.Suave, icono = Icons.Rounded.TravelExplore)
                }
                BarraBusqueda(vm.busqueda, { vm.busqueda = it }, "Buscar en mis mazos")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroBiblioteca.entries.forEach { f -> FiltroChip(f.etiqueta, vm.filtro == f, { vm.filtro = f }) }
                }
            }

            val lista = vm.visibles
            Box(Modifier.fillMaxSize()) {
                when {
                    vm.cargando -> EstadoCarga(Modifier.align(Alignment.Center), "Cargando tus mazos…")
                    vm.error != null && lista.isEmpty() -> EstadoError(vm.error.orEmpty(), vm::cargar, Modifier.align(Alignment.Center))
                    lista.isEmpty() && vm.busqueda.isNotBlank() -> EstadoVacio(
                        "Sin coincidencias", "No hay mazos que se llamen «${vm.busqueda.trim()}». Prueba con otra palabra.",
                        Modifier.align(Alignment.Center), color = ColorFlashly.MANTEQUILLA
                    )
                    lista.isEmpty() && vm.filtro == FiltroBiblioteca.Guardados -> EstadoVacio(
                        "Aún no guardas mazos", "En Explorar encontrarás mazos públicos de otros estudiantes para guardar aquí.",
                        Modifier.align(Alignment.Center), color = ColorFlashly.LAVANDA,
                        textoAccion = "Ir a Explorar", onAccion = onExplorar
                    )
                    lista.isEmpty() -> EstadoVacio(
                        "Aún no tienes mazos", "Un mazo reúne tarjetas de preguntas y definiciones sobre un tema. Crea el primero y empieza a estudiar.",
                        Modifier.align(Alignment.Center), color = ColorFlashly.MENTA,
                        textoAccion = "Crear mi primer mazo", onAccion = onCrear
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(lista, key = { it.id }) { m ->
                            val propio = vm.esPropio(m)
                            TarjetaDeMazo(
                                m, onClick = { onAbrir(m.id) },
                                mostrarCreador = !propio, mostrarVisibilidad = propio,
                                accesorio = {
                                    if (propio) MenuMazoPropio(
                                        m,
                                        onEditar = { onEditar(m.id) },
                                        onCompartir = { compartirMazo(context, m) },
                                        onVisibilidad = { vm.alternarVisibilidad(m) },
                                        onEliminar = { porBorrar = m },
                                    ) else IconButton(onClick = { vm.quitarGuardado(m) }) {
                                        Icon(Icons.Rounded.Bookmark, contentDescription = "Quitar de guardados", tint = ColorFlashly.porNombre(m.color).contenido)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        // Botón visible para crear, flotando sobre la lista
        BotonFlashly(
            "Crear mazo", onCrear,
            Modifier.align(Alignment.BottomEnd).padding(20.dp),
            icono = Icons.Rounded.Add
        )
    }

    porBorrar?.let { m ->
        DialogoConfirmacion(
            titulo = "¿Eliminar «${m.nombre}»?",
            mensaje = "Se borrarán sus ${m.numTarjetas} tarjetas y dejará de estar en la biblioteca de quienes lo guardaron. No se puede deshacer.",
            textoConfirmar = "Eliminar", peligroso = true,
            onConfirmar = { vm.eliminar(m); porBorrar = null },
            onCancelar = { porBorrar = null }
        )
    }
}

/** Menú de opciones de un mazo propio: editar, compartir (si es público), cambiar visibilidad y eliminar. */
@Composable
fun MenuMazoPropio(
    m: MazoResumen,
    onEditar: () -> Unit,
    onCompartir: () -> Unit,
    onVisibilidad: () -> Unit,
    onEliminar: () -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones del mazo", tint = ColorFlashly.porNombre(m.color).contenido)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, containerColor = Paleta.Fondo) {
            DropdownMenuItem(text = { Text("Editar") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { abierto = false; onEditar() })
            if (m.esPublico) {
                DropdownMenuItem(text = { Text("Compartir") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { abierto = false; onCompartir() })
            }
            DropdownMenuItem(
                text = { Text(if (m.esPublico) "Hacer privado" else "Hacer público") },
                leadingIcon = { Icon(if (m.esPublico) Icons.Rounded.Lock else Icons.Rounded.Public, null) },
                onClick = { abierto = false; onVisibilidad() }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = Paleta.Error) },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = Paleta.Error) },
                onClick = { abierto = false; onEliminar() }
            )
        }
    }
}
