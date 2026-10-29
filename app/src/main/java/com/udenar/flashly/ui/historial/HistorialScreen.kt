package com.udenar.flashly.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.CuentaRepository
import com.udenar.flashly.data.EstudioRepository
import com.udenar.flashly.data.FilaRanking
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.ResumenSesion
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstadoVacio
import com.udenar.flashly.ui.components.FiltroChip
import com.udenar.flashly.ui.components.TarjetaDato
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TituloSeccion
import com.udenar.flashly.ui.estudio.Pildora
import com.udenar.flashly.ui.estudio.estiloModo
import com.udenar.flashly.ui.haceCuanto
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import com.udenar.flashly.ui.theme.colorDeHex
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

enum class Periodo(val texto: String) { SEMANA("Semana"), MES("Mes"), TODO("Todo") }

class HistorialViewModel : ViewModel() {
    var sesiones by mutableStateOf<List<ResumenSesion>?>(null); private set
    var ranking by mutableStateOf<List<FilaRanking>?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var errorRanking by mutableStateOf<String?>(null); private set

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                sesiones = EstudioRepository.historial()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }

    fun cargarRanking() {
        errorRanking = null
        viewModelScope.launch {
            try {
                ranking = EstudioRepository.ranking()
            } catch (e: Exception) {
                errorRanking = mensajeError(e)
            }
        }
    }
}

private fun fechaLocal(iso: String): LocalDate? =
    runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

private val espanol: Locale = Locale.forLanguageTag("es-CO")

/** Pantalla 13: «Mi progreso» con estadísticas reales, gráficas, sesiones recientes y ranking semanal. */
@Composable
fun HistorialScreen(onAbrirSesion: (String) -> Unit, onIrAMazos: () -> Unit, vm: HistorialViewModel = viewModel()) {
    var pestana by rememberSaveable { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        vm.cargar()
        vm.cargarRanking()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mi progreso", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChip("Actividad", pestana == 0, { pestana = 0 })
                FiltroChip("Ranking semanal", pestana == 1, { pestana = 1 })
            }
        }
        if (pestana == 0) Actividad(vm, onAbrirSesion, onIrAMazos) else Ranking(vm)
    }
}

@Composable
private fun Actividad(vm: HistorialViewModel, onAbrirSesion: (String) -> Unit, onIrAMazos: () -> Unit) {
    var periodo by rememberSaveable { mutableStateOf(Periodo.SEMANA) }
    var modo by rememberSaveable { mutableStateOf<ModoJuego?>(null) }
    val perfil by CuentaRepository.perfil.collectAsState()
    val todas = vm.sesiones

    when {
        vm.error != null && todas == null -> { EstadoError(vm.error!!, vm::cargar); return }
        todas == null -> { EstadoCarga(); return }
        todas.isEmpty() -> {
            EstadoVacio(
                "Aún no tienes sesiones",
                "Estudia cualquier mazo y aquí verás tu XP, tus puntuaciones y tu evolución.",
                textoAccion = "Ir a mis mazos", onAccion = onIrAMazos
            )
            return
        }
    }
    val hoy = LocalDate.now()
    val desde = when (periodo) {
        Periodo.SEMANA -> hoy.minusDays(6)
        Periodo.MES -> hoy.minusDays(29)
        Periodo.TODO -> LocalDate.MIN
    }
    val filtradas = todas!!.filter { s ->
        val d = fechaLocal(s.iniciadaEn) ?: return@filter false
        !d.isBefore(desde) && (modo == null || s.modoJuego == modo)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Periodo.entries.forEach { p -> FiltroChip(p.texto, periodo == p, { periodo = p }) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroChip("Todos los modos", modo == null, { modo = null })
            ModoJuego.entries.forEach { m -> FiltroChip(m.titulo, modo == m, { modo = m }) }
        }

        // Resumen del periodo
        val conPreguntas = filtradas.filter { it.preguntas > 0 }
        val media = if (conPreguntas.isEmpty()) null else conPreguntas.sumOf { it.aciertos } * 100 / conPreguntas.sumOf { it.preguntas }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaDato("${filtradas.size}", "Sesiones", ColorFlashly.MENTA, Icons.Rounded.SportsEsports, Modifier.weight(1f))
            TarjetaDato("${filtradas.sumOf { it.revisadas }}", "Tarjetas estudiadas", ColorFlashly.PERIWINKLE, Icons.Rounded.Style, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaDato(media?.let { "$it%" } ?: "—", "Puntuación media", ColorFlashly.LAVANDA, Icons.Rounded.Percent, Modifier.weight(1f))
            TarjetaDato("+${filtradas.sumOf { it.xpTotal }}", "XP del periodo", ColorFlashly.MANTEQUILLA, Icons.Rounded.Star, Modifier.weight(1f))
        }
        perfil?.let {
            Text("XP acumulado en total: ${it.xp}", style = MaterialTheme.typography.labelLarge, color = Paleta.TextoSecundario)
        }

        if (filtradas.isEmpty()) {
            Text(
                "No hay sesiones en este periodo. Prueba con «Todo» o con otro modo.",
                style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario
            )
            return@Column
        }

        // XP por día (semana), por semana (mes) o por mes (todo)
        val (etiquetas, valores) = agruparXp(filtradas, periodo, hoy)
        TarjetaFlashly(Modifier.fillMaxWidth()) {
            Text("XP obtenido", style = MaterialTheme.typography.titleMedium)
            Text(
                when (periodo) { Periodo.SEMANA -> "Últimos 7 días"; Periodo.MES -> "Últimas 5 semanas"; Periodo.TODO -> "Últimos 6 meses" },
                style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario
            )
            GraficaBarras(
                valores, etiquetas, Paleta.Melocoton,
                "XP obtenido: " + etiquetas.zip(valores).joinToString { (e, v) -> "$e $v" },
                Modifier.padding(top = 12.dp)
            )
        }

        // Evolución de la puntuación de los cuestionarios (solo sesiones con preguntas)
        val evolucion = conPreguntas.reversed().takeLast(12)
        TarjetaFlashly(Modifier.fillMaxWidth()) {
            Text("Puntuación de cuestionarios", style = MaterialTheme.typography.titleMedium)
            Text(
                "Solo preguntas de selección; las definiciones se valoran aparte.",
                style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario
            )
            if (evolucion.isEmpty()) {
                Text("Aún no respondes preguntas en este periodo.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            } else {
                val fechas = evolucion.map { s -> fechaLocal(s.iniciadaEn)?.let { "${it.dayOfMonth}/${it.monthValue}" }.orEmpty() }
                GraficaLinea(
                    evolucion.map { it.puntuacion ?: 0 }, fechas, Paleta.Periwinkle,
                    "Puntuaciones: " + evolucion.joinToString { "${it.puntuacion}%" },
                    Modifier.padding(top = 12.dp)
                )
            }
        }

        // Autoevaluación de definiciones: métrica distinta a la puntuación
        val loSe = filtradas.sumOf { it.loSe }
        val practicar = filtradas.sumOf { it.practicar }
        if (loSe + practicar > 0) {
            TarjetaFlashly(Modifier.fillMaxWidth()) {
                Text("Definiciones", style = MaterialTheme.typography.titleMedium)
                Text("Tu propia valoración al voltear las tarjetas", style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario)
                BarraDividida(loSe, practicar, Modifier.padding(top = 12.dp))
            }
        }

        TituloSeccion("Sesiones recientes")
        filtradas.take(30).forEach { s -> FilaSesion(s) { onAbrirSesion(s.id) } }
    }
}

/** Agrupa el XP: 7 días (L M X…), 5 semanas («d/M» del lunes) o 6 meses («oct»). */
private fun agruparXp(sesiones: List<ResumenSesion>, periodo: Periodo, hoy: LocalDate): Pair<List<String>, List<Int>> {
    val conFecha = sesiones.mapNotNull { s -> fechaLocal(s.iniciadaEn)?.let { it to s.xpTotal } }
    return when (periodo) {
        Periodo.SEMANA -> {
            val dias = (6 downTo 0).map { hoy.minusDays(it.toLong()) }
            dias.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, espanol).uppercase() } to
                dias.map { d -> conFecha.filter { it.first == d }.sumOf { it.second } }
        }
        Periodo.MES -> {
            val lunes = hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
            val semanas = (4 downTo 0).map { lunes.minusWeeks(it.toLong()) }
            semanas.map { "${it.dayOfMonth}/${it.monthValue}" } to
                semanas.map { ini -> conFecha.filter { !it.first.isBefore(ini) && it.first.isBefore(ini.plusWeeks(1)) }.sumOf { it.second } }
        }
        Periodo.TODO -> {
            val meses = (5 downTo 0).map { hoy.withDayOfMonth(1).minusMonths(it.toLong()) }
            meses.map { it.month.getDisplayName(TextStyle.SHORT, espanol).trimEnd('.') } to
                meses.map { m -> conFecha.filter { it.first.year == m.year && it.first.month == m.month }.sumOf { it.second } }
        }
    }
}

/** Barra horizontal «Lo sé» / «Necesito practicar» con los números escritos. */
@Composable
private fun BarraDividida(loSe: Int, practicar: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50))) {
            if (loSe > 0) Box(Modifier.weight(loSe.toFloat()).fillMaxHeight().background(Paleta.Menta))
            if (practicar > 0) Box(Modifier.weight(practicar.toFloat()).fillMaxHeight().background(Paleta.Lavanda))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Lo sé: $loSe", style = MaterialTheme.typography.labelLarge, color = ColorFlashly.MENTA.contenido)
            Text("Necesito practicar: $practicar", style = MaterialTheme.typography.labelLarge, color = ColorFlashly.LAVANDA.contenido)
        }
    }
}

@Composable
private fun FilaSesion(s: ResumenSesion, onClick: () -> Unit) {
    val estilo = estiloModo(s.modoJuego)
    TarjetaFlashly(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(estilo.color.fondo), contentAlignment = Alignment.Center) {
                Icon(estilo.icono, contentDescription = null, tint = estilo.color.fuerte)
            }
            Column(Modifier.weight(1f)) {
                Text(s.mazoNombre, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    listOfNotNull(s.modoJuego.titulo, haceCuanto(s.iniciadaEn)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario
                )
                Text(
                    listOfNotNull(
                        s.puntuacion?.let { "$it%" },
                        if (s.revisadas == 1) "1 tarjeta" else "${s.revisadas} tarjetas",
                        "+${s.xpTotal} XP",
                        when (s.estado) { "fallida" -> "sin vidas"; "tiempo_agotado" -> "por tiempo"; else -> null },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Ver detalle", tint = Paleta.TextoSecundario)
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun Ranking(vm: HistorialViewModel) {
    // Realtime: mientras el ranking esté en pantalla, cada cambio de XP semanal lo recarga
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            runCatching { EstudioRepository.cambiosRanking().debounce(400).collect { vm.cargarRanking() } }
        }
    }
    val yo = EstudioRepository.usuarioId()
    val lista = vm.ranking

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TarjetaFlashly(Modifier.fillMaxWidth(), color = ColorFlashly.MANTEQUILLA.fondo, esquinas = 28.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = ColorFlashly.MANTEQUILLA.fuerte, modifier = Modifier.size(36.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ranking de la semana", style = MaterialTheme.typography.titleLarge, color = ColorFlashly.MANTEQUILLA.contenido)
                    Text(
                        "XP ganado desde el lunes. Se reinicia cada semana.",
                        style = MaterialTheme.typography.bodySmall, color = ColorFlashly.MANTEQUILLA.contenido
                    )
                }
            }
            Pildora("● En vivo", ColorFlashly.MENTA, Modifier.padding(top = 10.dp), fondo = Paleta.Blanco)
        }
        when {
            vm.errorRanking != null && lista == null -> EstadoError(vm.errorRanking!!, vm::cargarRanking)
            lista == null -> EstadoCarga()
            lista.isEmpty() -> EstadoVacio(
                "Nadie ha ganado XP esta semana",
                "Completa una sesión y aparecerás aquí. ¡Sé el primero!",
                color = ColorFlashly.MANTEQUILLA
            )
            else -> {
                val miPosicion = lista.indexOfFirst { it.usuarioId == yo }
                Text(
                    if (miPosicion >= 0) "Vas en el puesto ${miPosicion + 1} de ${lista.size}" else "Aún no sumas XP esta semana",
                    style = MaterialTheme.typography.labelLarge, color = Paleta.TextoSecundario
                )
                lista.forEachIndexed { i, fila -> FilaRanking(i + 1, fila, fila.usuarioId == yo) }
            }
        }
    }
}

@Composable
private fun FilaRanking(puesto: Int, fila: FilaRanking, esYo: Boolean) {
    val medalla = when (puesto) {
        1 -> ColorFlashly.MANTEQUILLA
        2 -> ColorFlashly.PERIWINKLE
        3 -> ColorFlashly.MELOCOTON
        else -> null
    }
    val forma = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(forma)
            .background(if (esYo) Paleta.Menta.copy(alpha = 0.5f) else Paleta.Blanco)
            .border(if (esYo) 2.dp else 1.dp, if (esYo) Paleta.VerdeAzulado else Paleta.BordeSuave, forma)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(medalla?.fondo ?: Paleta.BordeSuave),
            contentAlignment = Alignment.Center
        ) {
            Text("$puesto", style = MaterialTheme.typography.labelLarge, color = medalla?.contenido ?: Paleta.Texto)
        }
        val a = fila.perfil?.avatar
        AvatarCirculo(
            url = a?.let { SupabaseProvider.urlAvatar(it.archivo) }, colorFondo = colorDeHex(a?.color), tamano = 40.dp,
            iniciales = fila.perfil?.nombreUsuario.orEmpty()
        )
        Text(
            "@${fila.perfil?.nombreUsuario ?: "usuario"}" + if (esYo) " (tú)" else "",
            style = MaterialTheme.typography.bodyLarge, fontWeight = if (esYo) FontWeight.Bold else null,
            modifier = Modifier.weight(1f), maxLines = 1
        )
        Text("${fila.xp} XP", style = MaterialTheme.typography.titleMedium)
    }
}
