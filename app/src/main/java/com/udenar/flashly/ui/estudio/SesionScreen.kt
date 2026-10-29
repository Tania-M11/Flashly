package com.udenar.flashly.ui.estudio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.ResultadoRespuesta
import com.udenar.flashly.data.TarjetaSesion
import com.udenar.flashly.ui.components.BotonCircular
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.ProgresoConTexto
import com.udenar.flashly.ui.components.TarjetaVolteable
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.mazos.letraOpcion
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta
import kotlinx.coroutines.delay

/** Pantallas 11 (sesión) y 12 (resumen al terminar). */
@Composable
fun SesionScreen(
    onSalir: () -> Unit,
    onRepetir: () -> Unit,
    onInicio: () -> Unit,
    onHistorial: () -> Unit,
    vm: SesionViewModel = viewModel(),
) {
    val estilo = estiloModo(vm.modo)
    var confirmarSalida by remember { mutableStateOf(false) }
    val sinRespuestas = vm.indice == 0 && vm.respuesta == null

    BackHandler {
        when {
            vm.resumen != null || vm.sesion == null -> onSalir()
            sinRespuestas -> vm.abandonar(onSalir)
            else -> confirmarSalida = true
        }
    }

    val resumen = vm.resumen
    when {
        resumen != null -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ResumenContenido(resumen, Modifier.weight(1f)) {
                BotonFlashly("Volver a estudiar", onRepetir, Modifier.fillMaxWidth(), icono = Icons.Rounded.Replay)
                BotonFlashly("Ir al inicio", onInicio, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
                BotonFlashly("Ver mi historial", onHistorial, Modifier.fillMaxWidth(), estilo = EstiloBoton.Texto)
            }
        }
        vm.cargando -> EstadoCarga(Modifier.fillMaxSize().statusBarsPadding(), "Preparando tus tarjetas…")
        vm.errorInicio != null -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(16.dp)) { BotonCircular(Icons.Rounded.Close, "Salir", onSalir) }
            EstadoError(vm.errorInicio!!, vm::iniciar, titulo = "No se pudo empezar")
        }
        else -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BarraSesion(vm, estilo) { if (sinRespuestas) vm.abandonar(onSalir) else confirmarSalida = true }
            val t = vm.tarjeta
            if (t != null) {
                AnimatedContent(
                    targetState = vm.indice,
                    transitionSpec = {
                        (slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(260)))
                            .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(200)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "tarjeta"
                ) { _ ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (vm.modo == ModoJuego.REPASO) NivelCaja(t.caja, estilo.color)
                        if (t.esPregunta) Pregunta(t, vm.seleccion, vm.respuesta, vm.enviando, vm::elegirOpcion)
                        else Definicion(t, vm.volteada, estilo.color, vm::voltear)
                    }
                }
                MensajeAviso(vm.error, TipoMensaje.Error, Modifier.padding(horizontal = 20.dp))
                PieSesion(vm, t, estilo.color)
            }
        }
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Salir de la sesión?",
            mensaje = "Perderás el XP de esta sesión. Lo que ya respondiste sí cuenta para tu repaso inteligente.",
            textoConfirmar = "Salir",
            textoCancelar = "Seguir estudiando",
            onConfirmar = { confirmarSalida = false; vm.abandonar(onSalir) },
            onCancelar = { confirmarSalida = false },
            peligroso = true,
            icono = Icons.AutoMirrored.Rounded.ExitToApp,
        )
    }
}

/** Encabezado: salir, mazo y modo, indicadores del modo (vidas, tiempo, XP) y progreso «4 de 20». */
@Composable
private fun BarraSesion(vm: SesionViewModel, estilo: EstiloModo, onSalir: () -> Unit) {
    val c = estilo.color
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonCircular(Icons.Rounded.Close, "Salir de la sesión", onSalir)
            Column(Modifier.weight(1f)) {
                Text(
                    vm.sesion?.mazoNombre.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(estilo.icono, contentDescription = null, tint = c.fuerte, modifier = Modifier.size(14.dp))
                    Text(vm.modo.titulo, style = MaterialTheme.typography.labelMedium, color = c.fuerte)
                }
            }
            vm.vidas?.let { Vidas(it) }
            vm.segundosRestantes?.let { s ->
                val urgente = s <= 10
                Pildora(
                    formatoTiempo(s), if (urgente) ColorFlashly.ROSA else ColorFlashly.MELOCOTON,
                    Modifier.semantics { contentDescription = "Quedan $s segundos" },
                    icono = Icons.Rounded.Timer
                )
            }
            Box {
                Pildora("${vm.xp} XP", ColorFlashly.MANTEQUILLA, icono = Icons.Rounded.Star)
                XpFlotante(vm.ultimoXp, vm.ultimoXpId, Modifier.align(Alignment.BottomCenter))
            }
        }
        val respondidas = vm.indice + if (vm.respuesta != null) 1 else 0
        ProgresoConTexto(respondidas, vm.total, color = c.fuerte)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedVisibility(vm.combo >= 2, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
                Pildora("Combo ×${vm.combo}", ColorFlashly.MELOCOTON, icono = Icons.Rounded.Bolt)
            }
            if (vm.modo == ModoJuego.RETO && vm.sesion?.xpDobleDisponible == true) {
                Pildora("XP doble en juego", ColorFlashly.ROSA, icono = Icons.Rounded.Bolt)
            }
        }
    }
}

/** «+10 XP» que aparece un momento cada vez que el servidor suma XP. */
@Composable
private fun XpFlotante(xp: Int, id: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id == 0 || xp <= 0) return@LaunchedEffect
        visible = true
        delay(1100)
        visible = false
    }
    AnimatedVisibility(
        visible, modifier.padding(top = 40.dp),
        enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { it }
    ) {
        Text("+$xp XP", style = MaterialTheme.typography.labelLarge, color = ColorFlashly.MANTEQUILLA.fuerte)
    }
}

@Composable
private fun Pregunta(t: TarjetaSesion, seleccion: Int?, respuesta: ResultadoRespuesta?, enviando: Boolean, onElegir: (Int) -> Unit) {
    Text("Pregunta", style = MaterialTheme.typography.labelLarge, color = Paleta.TextoSecundario)
    Text(t.enunciado.orEmpty(), style = MaterialTheme.typography.headlineSmall)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        t.opciones.orEmpty().forEachIndexed { i, texto ->
            val estado = when {
                respuesta == null -> if (seleccion == i) EstadoOpcion.ELEGIDA else EstadoOpcion.NORMAL
                i == respuesta.indiceCorrecto -> EstadoOpcion.CORRECTA
                i == seleccion -> EstadoOpcion.INCORRECTA
                else -> EstadoOpcion.ATENUADA
            }
            OpcionRespuesta(letraOpcion(i), texto, estado, enviando && seleccion == i, habilitada = respuesta == null && !enviando) { onElegir(i) }
        }
    }
}

private enum class EstadoOpcion { NORMAL, ELEGIDA, CORRECTA, INCORRECTA, ATENUADA }

@Composable
private fun OpcionRespuesta(letra: String, texto: String, estado: EstadoOpcion, cargando: Boolean, habilitada: Boolean, onClick: () -> Unit) {
    val (fondo, borde) = when (estado) {
        EstadoOpcion.CORRECTA -> Paleta.ExitoSuave to Paleta.Exito
        EstadoOpcion.INCORRECTA -> Paleta.ErrorSuave to Paleta.Error
        EstadoOpcion.ELEGIDA -> Paleta.Crema to Paleta.VerdeAzulado
        else -> Paleta.Blanco to Paleta.Borde
    }
    val forma = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(forma).background(fondo)
            .border(if (estado == EstadoOpcion.NORMAL || estado == EstadoOpcion.ATENUADA) 1.dp else 2.dp, borde, forma)
            .clickable(enabled = habilitada, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(if (estado == EstadoOpcion.CORRECTA || estado == EstadoOpcion.INCORRECTA) borde else Paleta.Menta.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                EstadoOpcion.CORRECTA -> Icon(Icons.Rounded.Check, "Correcta", tint = Color.White, modifier = Modifier.size(20.dp))
                EstadoOpcion.INCORRECTA -> Icon(Icons.Rounded.Close, "Incorrecta", tint = Color.White, modifier = Modifier.size(20.dp))
                else -> Text(letra, style = MaterialTheme.typography.labelLarge, color = Paleta.VerdeAzuladoOscuro)
            }
        }
        Text(
            texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            color = if (estado == EstadoOpcion.ATENUADA) Paleta.TextoSecundario else Paleta.Texto
        )
        if (cargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Paleta.VerdeAzulado)
    }
}

@Composable
private fun Definicion(t: TarjetaSesion, volteada: Boolean, color: ColorFlashly, onVoltear: () -> Unit) {
    TarjetaVolteable(
        volteada = volteada,
        onVoltear = onVoltear,
        modifier = Modifier.fillMaxWidth().height(340.dp),
        colorAnverso = color.fondo,
        colorReverso = Paleta.Blanco,
        anverso = {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Término", style = MaterialTheme.typography.labelLarge, color = color.contenido.copy(alpha = 0.8f))
                Text(
                    t.termino.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = color.contenido,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = color.contenido, modifier = Modifier.size(18.dp))
                    Text("Toca para ver la definición", style = MaterialTheme.typography.bodySmall, color = color.contenido)
                }
            }
        },
        reverso = {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(t.termino.orEmpty(), style = MaterialTheme.typography.labelLarge, color = color.fuerte)
                Text(
                    t.definicion.orEmpty(), style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    )
}

/** Parte inferior: retroalimentación + «Siguiente» en preguntas, o «Lo sé» / «Necesito practicar» en definiciones. */
@Composable
private fun PieSesion(vm: SesionViewModel, t: TarjetaSesion, color: ColorFlashly) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val r = vm.respuesta
        when {
            t.esPregunta && r != null -> {
                Retroalimentacion(r, vm.modo, t.opciones.orEmpty())
                BotonFlashly(if (vm.haTerminado) "Ver resultados" else "Siguiente", vm::avanzar, Modifier.fillMaxWidth())
            }
            t.esPregunta -> Text(
                "Elige una opción", style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
            !vm.volteada -> BotonFlashly("Ver definición", vm::voltear, Modifier.fillMaxWidth(), estilo = EstiloBoton.Suave)
            else -> {
                Text(
                    "¿Lo sabías? Sé honesto: ninguna opción te quita vidas ni XP.",
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonFlashly(
                        "Necesito practicar", { vm.calificarDefinicion(false) }, Modifier.weight(1f),
                        estilo = EstiloBoton.Secundario, habilitado = !vm.enviando
                    )
                    BotonFlashly(
                        "Lo sé", { vm.calificarDefinicion(true) }, Modifier.weight(1f),
                        icono = Icons.Rounded.Check, cargando = vm.enviando
                    )
                }
            }
        }
    }
}

@Composable
private fun Retroalimentacion(r: ResultadoRespuesta, modo: ModoJuego, opciones: List<String>) {
    val bien = r.correcta == true
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (bien) Paleta.ExitoSuave else Paleta.ErrorSuave)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (bien) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null,
                tint = if (bien) Paleta.Exito else Paleta.Error
            )
            Text(
                if (bien) "¡Correcto! +${r.xp} XP" else "Incorrecto",
                style = MaterialTheme.typography.titleMedium, color = if (bien) Paleta.Exito else Paleta.Error
            )
        }
        if (!bien) r.indiceCorrecto?.let { i ->
            Text("La respuesta correcta era ${letraOpcion(i)}: ${opciones.getOrNull(i).orEmpty()}", style = MaterialTheme.typography.bodyMedium)
        }
        if (!bien && modo == ModoJuego.RETO) {
            Text("Perdiste una vida", style = MaterialTheme.typography.bodySmall, color = Paleta.Error)
        }
        if (bien && r.combo >= 2 && modo == ModoJuego.CONTRARRELOJ) {
            Text("Combo ×${r.combo}: más XP por cada acierto seguido", style = MaterialTheme.typography.bodySmall)
        }
        r.explicacion?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Paleta.Texto, modifier = Modifier.padding(top = 4.dp))
        }
        if (modo == ModoJuego.REPASO) r.caja?.let {
            Text(textoProximoRepaso(it), style = MaterialTheme.typography.labelMedium, color = Paleta.TextoSecundario)
        }
    }
}
