package com.udenar.flashly.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udenar.flashly.data.Categoria
import com.udenar.flashly.data.DatosMazo
import com.udenar.flashly.data.MazosRepository
import com.udenar.flashly.data.Tarjeta
import com.udenar.flashly.data.TarjetaEditable
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.validarMazo
import com.udenar.flashly.data.validarTarjeta
import kotlinx.coroutines.launch

/**
 * Estado del editor de mazos (pantallas 8 y 9). Si la ruta trae [mazoId] se edita ese mazo;
 * si no, se crea uno nuevo. Todo se guarda de una vez con guardar_mazo() al pulsar «Guardar».
 */
class EditorMazoViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val mazoId: String? = savedStateHandle["mazoId"]
    val esNuevo get() = mazoId == null

    var cargando by mutableStateOf(mazoId != null); private set
    var errorCarga by mutableStateOf<String?>(null); private set
    var categorias by mutableStateOf<List<Categoria>>(emptyList()); private set

    var datos by mutableStateOf(DatosMazo()); private set
    var tarjetas by mutableStateOf<List<TarjetaEditable>>(emptyList()); private set
    private var original = DatosMazo() to emptyList<TarjetaEditable>()

    /** Tarjeta abierta en el editor de tarjetas (pantalla 9); null = se ve el editor del mazo. */
    var enEdicion by mutableStateOf<TarjetaEditable?>(null); private set
    var esTarjetaNueva by mutableStateOf(false); private set
    var errorTarjeta by mutableStateOf<String?>(null); private set

    var guardando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    val hayCambios get() = datos != original.first || tarjetas != original.second

    init { cargar() }

    fun cargar() {
        errorCarga = null
        viewModelScope.launch {
            try {
                categorias = MazosRepository.categorias()
                val id = mazoId ?: return@launch
                val m = MazosRepository.mazo(id)
                val claves = MazosRepository.clavesDeMiMazo(id).associateBy { it.tarjetaId }
                datos = DatosMazo(m.nombre, m.descripcion.orEmpty(), m.categoriaId, m.color, m.esPublico)
                tarjetas = MazosRepository.tarjetas(id).map { t ->
                    val clave = claves[t.id]
                    TarjetaEditable(
                        id = t.id, tipo = t.tipo,
                        enunciado = t.enunciado.orEmpty(), opciones = t.opciones ?: listOf("", ""),
                        correcta = clave?.indiceCorrecto ?: -1, explicacion = clave?.explicacion.orEmpty(),
                        termino = t.termino.orEmpty(), definicion = t.definicion.orEmpty(),
                    )
                }
                original = datos to tarjetas
            } catch (e: Exception) {
                errorCarga = mensajeError(e)
            }
            cargando = false
        }
    }

    fun cambiarDatos(cambio: (DatosMazo) -> DatosMazo) {
        datos = cambio(datos)
        error = null
    }

    // ---------- Tarjetas ----------

    fun nuevaTarjeta(tipo: String) {
        enEdicion = if (tipo == Tarjeta.TIPO_PREGUNTA) TarjetaEditable.nuevaPregunta() else TarjetaEditable.nuevaDefinicion()
        esTarjetaNueva = true
        errorTarjeta = null
    }

    fun abrirTarjeta(t: TarjetaEditable) {
        enEdicion = t
        esTarjetaNueva = false
        errorTarjeta = null
    }

    fun cambiarTarjeta(cambio: (TarjetaEditable) -> TarjetaEditable) {
        enEdicion = enEdicion?.let(cambio)
        errorTarjeta = null
    }

    /** Valida la tarjeta; si está bien la agrega (o reemplaza) en la lista y vuelve al editor del mazo. */
    fun guardarTarjeta(): Boolean {
        val t = enEdicion ?: return false
        validarTarjeta(t)?.let { errorTarjeta = it; return false }
        tarjetas = if (esTarjetaNueva) tarjetas + t else tarjetas.map { if (it.clave == t.clave) t else it }
        enEdicion = null
        error = null
        return true
    }

    fun cerrarTarjeta() {
        enEdicion = null
        errorTarjeta = null
    }

    fun eliminarTarjeta(t: TarjetaEditable) {
        tarjetas = tarjetas.filterNot { it.clave == t.clave }
    }

    // ---------- Guardar el mazo ----------

    fun guardar(onListo: (String) -> Unit) {
        validarMazo(datos, tarjetas)?.let { error = it; return }
        viewModelScope.launch {
            guardando = true
            error = null
            try {
                val id = MazosRepository.guardarMazo(mazoId, datos, tarjetas)
                original = datos to tarjetas
                onListo(id)
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            guardando = false
        }
    }
}
