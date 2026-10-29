package com.udenar.flashly.ui.mazos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.udenar.flashly.data.Categoria
import com.udenar.flashly.data.MazoResumen
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.BarraBusqueda
import com.udenar.flashly.ui.components.BotonCircular
import com.udenar.flashly.ui.components.ChipCategoria
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstadoVacio
import com.udenar.flashly.ui.components.iconoCategoria
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExplorarViewModel : ViewModel() {
    var categorias by mutableStateOf<List<Categoria>>(emptyList()); private set
    var resultados by mutableStateOf<List<MazoResumen>>(emptyList()); private set
    var guardados by mutableStateOf<Set<String>>(emptySet()); private set
    var cargando by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var texto by mutableStateOf(""); private set
    var categoria by mutableStateOf<String?>(null); private set
    private var busqueda: Job? = null

    init {
        viewModelScope.launch { runCatching { categorias = MazosRepository.categorias() } }
    }

    fun cambiarTexto(t: String) { texto = t; buscar(conPausa = true) }
    fun elegirCategoria(id: String?) { categoria = id; buscar() }

    /** Consulta en Supabase; al escribir espera 0,4 s de pausa para no lanzar una búsqueda por letra. */
    fun buscar(conPausa: Boolean = false) {
        busqueda?.cancel()
        busqueda = viewModelScope.launch {
            if (conPausa) delay(400)
            cargando = true
            error = null
            try {
                resultados = MazosRepository.explorar(texto, categoria)
                guardados = MazosRepository.idsGuardados()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            cargando = false
        }
    }

    fun alternarGuardado(m: MazoResumen) {
        val estaba = m.id in guardados
        guardados = if (estaba) guardados - m.id else guardados + m.id   // respuesta inmediata en pantalla
        viewModelScope.launch {
            try {
                if (estaba) MazosRepository.quitarGuardado(m.id) else MazosRepository.guardar(m.id)
            } catch (e: Exception) {
                guardados = if (estaba) guardados + m.id else guardados - m.id
                error = mensajeError(e)
            }
        }
    }
}

/** Pantalla 6: explorar mazos públicos de otros usuarios por texto y por categoría. */
@Composable
fun ExplorarScreen(onVolver: () -> Unit, onAbrir: (String) -> Unit, vm: ExplorarViewModel = viewModel()) {
    LifecycleResumeEffect(Unit) {
        vm.buscar()
        onPauseOrDispose { }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonCircular(Icons.AutoMirrored.Rounded.ArrowBack, "Volver a mis mazos", onVolver)
                Column(Modifier.weight(1f)) {
                    Text("Explorar", style = MaterialTheme.typography.headlineLarge)
                    Text("Mazos públicos de otros estudiantes", style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
                }
            }
            BarraBusqueda(vm.texto, vm::cambiarTexto, "Buscar por tema o palabra")
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ChipCategoria("Todas", Icons.Rounded.Apps, ColorFlashly.MENTA, vm.categoria == null) { vm.elegirCategoria(null) }
            }
            items(vm.categorias, key = { it.id }) { c ->
                ChipCategoria(c.nombre, iconoCategoria(c.icono), ColorFlashly.porNombre(c.color), vm.categoria == c.id) {
                    vm.elegirCategoria(if (vm.categoria == c.id) null else c.id)
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            val lista = vm.resultados
            when {
                vm.cargando && lista.isEmpty() -> EstadoCarga(Modifier.align(Alignment.Center), "Buscando mazos…")
                vm.error != null && lista.isEmpty() -> EstadoError(vm.error.orEmpty(), { vm.buscar() }, Modifier.align(Alignment.Center))
                lista.isEmpty() && (vm.texto.isNotBlank() || vm.categoria != null) -> EstadoVacio(
                    "Sin coincidencias",
                    if (vm.texto.isNotBlank()) "No encontramos mazos públicos sobre «${vm.texto.trim()}»." else "Todavía no hay mazos públicos en esta categoría.",
                    Modifier.align(Alignment.Center), color = ColorFlashly.MANTEQUILLA
                )
                lista.isEmpty() -> EstadoVacio(
                    "Aún no hay mazos públicos",
                    "Cuando otros estudiantes publiquen sus mazos, aparecerán aquí.",
                    Modifier.align(Alignment.Center), color = ColorFlashly.LAVANDA
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(lista, key = { it.id }) { m ->
                        val guardado = m.id in vm.guardados
                        TarjetaDeMazo(
                            m, onClick = { onAbrir(m.id) },
                            accesorio = {
                                IconButton(onClick = { vm.alternarGuardado(m) }) {
                                    Icon(
                                        if (guardado) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = if (guardado) "Quitar de mi biblioteca" else "Guardar en mi biblioteca",
                                        tint = ColorFlashly.porNombre(m.color).contenido
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
