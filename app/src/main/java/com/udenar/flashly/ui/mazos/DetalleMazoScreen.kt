package com.udenar.flashly.ui.mazos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.Categoria
import com.udenar.flashly.data.MazoResumen
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.data.Tarjeta
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.BotonCircular
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.EtiquetaInfo
import com.udenar.flashly.ui.components.EtiquetaVisibilidad
import com.udenar.flashly.ui.components.TarjetaDato
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TarjetaVolteable
import com.udenar.flashly.ui.components.iconoCategoria
import com.udenar.flashly.ui.haceCuanto
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import com.udenar.flashly.ui.theme.colorDeHex
import kotlinx.coroutines.launch

class DetalleMazoViewModel : ViewModel() {
    var mazo by mutableStateOf<MazoResumen?>(null); private set
    var tarjetas by mutableStateOf<List<Tarjeta>>(emptyList()); private set
    var categoria by mutableStateOf<Categoria?>(null); private set
    var guardado by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    val esMio get() = mazo?.creadorId == MazosRepository.usuarioId()

    fun cargar(id: String) {
        error = null
        viewModelScope.launch {
            try {
                val m = MazosRepository.mazo(id)
                tarjetas = MazosRepository.tarjetas(id)
                categoria = MazosRepository.categorias().firstOrNull { it.id == m.categoriaId }
                guardado = id in MazosRepository.idsGuardados()
                mazo = m
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }

    fun alternarGuardado() {
        val m = mazo ?: return
        val estaba = guardado
        guardado = !estaba
        viewModelScope.launch {
            try {
                if (estaba) MazosRepository.quitarGuardado(m.id) else MazosRepository.guardar(m.id)
            } catch (e: Exception) {
                guardado = estaba
                error = mensajeError(e)
            }
        }
    }
}

/** Pantalla 7: detalle de un mazo con su autor, composición, vista previa y acciones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalleMazoScreen(
    mazoId: String,
    onVolver: () -> Unit,
    onEstudiar: (String) -> Unit,
    onEditar: (String) -> Unit,
    vm: DetalleMazoViewModel = viewModel(),
) {
    // Se recarga al volver (por ejemplo, después de editar el mazo)
    LifecycleResumeEffect(mazoId) {
        vm.cargar(mazoId)
        onPauseOrDispose { }
    }
    val context = LocalContext.current
    val m = vm.mazo

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Barra superior: volver y acciones según si el mazo es propio o ajeno
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BotonCircular(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onVolver)
            Spacer(Modifier.weight(1f))
            if (m != null && m.esPublico) BotonCircular(Icons.Rounded.Share, "Compartir mazo", { compartirMazo(context, m) })
            if (m != null && vm.esMio) BotonCircular(Icons.Rounded.Edit, "Editar mazo", { onEditar(m.id) })
            if (m != null && !vm.esMio) BotonCircular(
                if (vm.guardado) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                if (vm.guardado) "Quitar de mi biblioteca" else "Guardar en mi biblioteca",
                vm::alternarGuardado
            )
        }

        when {
            m == null && vm.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoError(vm.error.orEmpty(), { vm.cargar(mazoId) }) }
            m == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoCarga(texto = "Abriendo el mazo…") }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val color = ColorFlashly.porNombre(m.color)
                // Portada
                TarjetaFlashly(Modifier.fillMaxWidth(), color = color.fondo, esquinas = 32.dp) {
                    vm.categoria?.let { c -> EtiquetaInfo(c.nombre, icono = iconoCategoria(c.icono)) }
                    Text(m.nombre, style = MaterialTheme.typography.headlineLarge, color = color.contenido, modifier = Modifier.padding(top = 12.dp))
                    if (!m.descripcion.isNullOrBlank()) {
                        Text(m.descripcion, style = MaterialTheme.typography.bodyLarge, color = color.contenido.copy(alpha = 0.85f), modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val avatar = m.creador?.avatar
                        AvatarCirculo(
                            url = avatar?.let { SupabaseProvider.urlAvatar(it.archivo) }, colorFondo = colorDeHex(avatar?.color),
                            tamano = 40.dp, iniciales = m.creador?.nombreUsuario.orEmpty()
                        )
                        Column {
                            Text(if (vm.esMio) "Creado por ti" else "Creado por", style = MaterialTheme.typography.labelMedium, color = color.contenido.copy(alpha = 0.75f))
                            Text("@${m.creador?.nombreUsuario.orEmpty()}", style = MaterialTheme.typography.titleMedium, color = color.contenido)
                        }
                    }
                    FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EtiquetaInfo("${m.numTarjetas} ${if (m.numTarjetas == 1) "tarjeta" else "tarjetas"}", icono = Icons.Rounded.Style)
                        EtiquetaVisibilidad(m.esPublico)
                        haceCuanto(m.updatedAt)?.let { EtiquetaInfo("Actualizado $it") }
                    }
                }

                // Composición: el mazo puede mezclar preguntas y definiciones
                val preguntas = vm.tarjetas.count { it.esPregunta }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaDato("$preguntas", if (preguntas == 1) "pregunta de selección" else "preguntas de selección", ColorFlashly.LAVANDA, Icons.Rounded.Quiz, Modifier.weight(1f))
                    TarjetaDato("${vm.tarjetas.size - preguntas}", if (vm.tarjetas.size - preguntas == 1) "tarjeta de definición" else "tarjetas de definición", ColorFlashly.MELOCOTON, Icons.AutoMirrored.Rounded.MenuBook, Modifier.weight(1f))
                }

                BotonFlashly("Estudiar", { onEstudiar(m.id) }, Modifier.fillMaxWidth(), icono = Icons.Rounded.PlayArrow, habilitado = vm.tarjetas.isNotEmpty())
                if (vm.esMio) BotonFlashly("Editar mazo", { onEditar(m.id) }, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario, icono = Icons.Rounded.Edit)

                Text("Vista previa", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                vm.tarjetas.forEachIndexed { i, t ->
                    if (t.esPregunta) VistaPregunta(i + 1, t) else VistaDefinicion(i + 1, t, color)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Etiqueta del tipo de tarjeta: ícono + texto, para no depender del color. */
@Composable
private fun TipoTarjeta(numero: Int, esPregunta: Boolean) {
    val color = if (esPregunta) ColorFlashly.LAVANDA else ColorFlashly.MELOCOTON
    Row(
        Modifier.clip(CircleShape).background(color.fondo.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(if (esPregunta) Icons.Rounded.Quiz else Icons.AutoMirrored.Rounded.MenuBook, null, tint = color.contenido, modifier = Modifier.size(14.dp))
        Text("$numero · ${if (esPregunta) "Pregunta" else "Definición"}", style = MaterialTheme.typography.labelMedium, color = color.contenido)
    }
}

/** Pregunta de selección múltiple: enunciado y todas sus opciones con letras (sin marcar la correcta). */
@Composable
private fun VistaPregunta(numero: Int, t: Tarjeta) {
    TarjetaFlashly(Modifier.fillMaxWidth()) {
        TipoTarjeta(numero, esPregunta = true)
        Text(t.enunciado.orEmpty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
        t.opciones.orEmpty().forEachIndexed { i, o ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Paleta.Crema), contentAlignment = Alignment.Center) {
                    Text(letraOpcion(i), style = MaterialTheme.typography.labelMedium)
                }
                Text(o, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Definición: tarjeta que gira al tocarla para mostrar el reverso. */
@Composable
private fun VistaDefinicion(numero: Int, t: Tarjeta, color: ColorFlashly) {
    var volteada by remember { mutableStateOf(false) }
    TarjetaVolteable(
        volteada = volteada, onVoltear = { volteada = !volteada },
        modifier = Modifier.fillMaxWidth().height(170.dp),
        colorAnverso = Paleta.Blanco, colorReverso = color.fondo,
        anverso = {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                TipoTarjeta(numero, esPregunta = false)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(t.termino.orEmpty(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                }
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TouchApp, null, tint = Paleta.TextoSecundario, modifier = Modifier.size(16.dp))
                    Text(" Toca para ver la definición", style = MaterialTheme.typography.labelMedium, color = Paleta.TextoSecundario)
                }
            }
        },
        reverso = {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text(t.termino.orEmpty(), style = MaterialTheme.typography.labelLarge, color = color.contenido.copy(alpha = 0.75f))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(t.definicion.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = color.contenido, textAlign = TextAlign.Center)
                }
            }
        }
    )
}

/** A, B, C… Z, AA, AB… para identificar opciones sin límite fijo. */
fun letraOpcion(indice: Int): String {
    var n = indice
    var letras = ""
    do {
        letras = ('A' + n % 26) + letras
        n = n / 26 - 1
    } while (n >= 0)
    return letras
}
