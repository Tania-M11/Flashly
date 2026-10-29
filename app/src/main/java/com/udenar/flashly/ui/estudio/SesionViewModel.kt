package com.udenar.flashly.ui.estudio

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udenar.flashly.data.EstudioRepository
import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.ResultadoRespuesta
import com.udenar.flashly.data.ResumenSesion
import com.udenar.flashly.data.SesionNueva
import com.udenar.flashly.data.TarjetaSesion
import com.udenar.flashly.data.mensajeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sesión de estudio (pantalla 11). La app solo muestra y envía lo que elige el usuario:
 * el servidor decide si la respuesta es correcta y calcula XP, combo, vidas y tiempo.
 */
class SesionViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val mazoId: String = checkNotNull(savedStateHandle["mazoId"])
    val modo: ModoJuego = ModoJuego.de(savedStateHandle["modo"])

    var cargando by mutableStateOf(true); private set
    var errorInicio by mutableStateOf<String?>(null); private set
    var sesion by mutableStateOf<SesionNueva?>(null); private set

    var indice by mutableIntStateOf(0); private set
    var vidas by mutableStateOf<Int?>(null); private set
    var xp by mutableIntStateOf(0); private set
    var combo by mutableIntStateOf(0); private set
    var segundosRestantes by mutableStateOf<Int?>(null); private set

    /** Opción tocada en la pregunta actual y la respuesta del servidor (retroalimentación). */
    var seleccion by mutableStateOf<Int?>(null); private set
    var respuesta by mutableStateOf<ResultadoRespuesta?>(null); private set
    var volteada by mutableStateOf(false); private set
    var enviando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    /** Último XP ganado, para la animación «+10 XP». Cambia [ultimoXpId] para relanzarla. */
    var ultimoXp by mutableIntStateOf(0); private set
    var ultimoXpId by mutableIntStateOf(0); private set

    /** Resumen final: si existe, la pantalla muestra el resumen (pantalla 12). */
    var resumen by mutableStateOf<ResumenSesion?>(null); private set
    private var resumenPendiente: ResumenSesion? = null
    val haTerminado get() = resumenPendiente != null

    private var reloj: Job? = null

    val tarjeta: TarjetaSesion? get() = sesion?.tarjetas?.getOrNull(indice)
    val total get() = sesion?.tarjetas?.size ?: 0
    val terminoTiempo get() = segundosRestantes == 0

    init { iniciar() }

    fun iniciar() {
        cargando = true
        errorInicio = null
        viewModelScope.launch {
            try {
                val s = EstudioRepository.iniciar(mazoId, modo)
                sesion = s
                vidas = s.vidas
                s.limiteSegundos?.let { iniciarReloj(it) }
            } catch (e: Exception) {
                errorInicio = mensajeError(e)
            }
            cargando = false
        }
    }

    /** Contrarreloj: cuenta regresiva con el reloj del sistema; al llegar a 0 el servidor cierra por tiempo. */
    private fun iniciarReloj(limite: Int) {
        val fin = SystemClock.elapsedRealtime() + limite * 1000L
        segundosRestantes = limite
        reloj = viewModelScope.launch {
            while (isActive) {
                val quedan = ((fin - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceAtLeast(0)
                segundosRestantes = quedan
                if (quedan == 0) {
                    cerrarPorTiempo()
                    break
                }
                delay(250)
            }
        }
    }

    private suspend fun cerrarPorTiempo() {
        val s = sesion ?: return
        if (resumen != null) return
        try {
            resumen = EstudioRepository.terminar(s.sesionId)
        } catch (e: Exception) {
            error = mensajeError(e)
        }
    }

    fun voltear() {
        volteada = !volteada
    }

    fun elegirOpcion(i: Int) {
        if (respuesta != null || enviando) return
        seleccion = i
        enviar(opcion = i)
    }

    fun calificarDefinicion(loSe: Boolean) {
        if (enviando) return
        enviar(loSe = loSe)
    }

    private fun enviar(opcion: Int? = null, loSe: Boolean? = null) {
        val s = sesion ?: return
        val t = tarjeta ?: return
        enviando = true
        error = null
        viewModelScope.launch {
            try {
                val r = EstudioRepository.responder(s.sesionId, t.id, opcion, loSe)
                if (r.tiempoAgotado) {
                    reloj?.cancel()
                    resumen = r.resumen
                    return@launch
                }
                xp = r.xpSesion
                combo = r.combo
                vidas = r.vidas
                ultimoXp = r.xp
                ultimoXpId++
                resumenPendiente = r.resumen
                if (r.resumen != null) reloj?.cancel()
                if (t.esPregunta) respuesta = r else avanzar()
            } catch (e: Exception) {
                error = mensajeError(e)
                seleccion = null
            } finally {
                enviando = false
            }
        }
    }

    /** «Siguiente»: pasa a la próxima tarjeta o, si la sesión terminó, al resumen. */
    fun avanzar() {
        resumenPendiente?.let { resumen = it; return }
        seleccion = null
        respuesta = null
        volteada = false
        if (indice < total - 1) indice++
    }

    /** Salir sin terminar: el servidor marca la sesión como abandonada (se pierde el XP). */
    fun abandonar(onListo: () -> Unit) {
        val s = sesion
        if (s == null || resumen != null) { onListo(); return }
        reloj?.cancel()
        viewModelScope.launch {
            runCatching { EstudioRepository.terminar(s.sesionId) }
            onListo()
        }
    }

    override fun onCleared() {
        // Si se cerró la pantalla de otra forma, la sesión no queda «en curso» para siempre
        val s = sesion ?: return
        if (resumen == null && resumenPendiente == null) {
            CoroutineScope(Dispatchers.IO).launch { runCatching { EstudioRepository.terminar(s.sesionId) } }
        }
    }
}
