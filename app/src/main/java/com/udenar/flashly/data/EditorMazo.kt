package com.udenar.flashly.data

import java.util.UUID

/** Datos generales del mazo en el editor. */
data class DatosMazo(
    val nombre: String = "",
    val descripcion: String = "",
    val categoriaId: String = "otros",
    val color: String = "ROSA",
    val esPublico: Boolean = false,
)

/**
 * Tarjeta mientras se edita. [clave] identifica la fila en pantalla; [id] es el de la base de datos
 * cuando la tarjeta ya existía (así guardar_mazo la actualiza en lugar de crear otra).
 */
data class TarjetaEditable(
    val clave: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val tipo: String,
    val enunciado: String = "",
    val opciones: List<String> = listOf("", ""),
    val correcta: Int = -1,
    val explicacion: String = "",
    val termino: String = "",
    val definicion: String = "",
) {
    val esPregunta get() = tipo == Tarjeta.TIPO_PREGUNTA

    /** Quita una opción y corrige el índice de la respuesta correcta. */
    fun sinOpcion(i: Int): TarjetaEditable = copy(
        opciones = opciones.filterIndexed { k, _ -> k != i },
        correcta = when {
            correcta == i -> -1
            correcta > i -> correcta - 1
            else -> correcta
        }
    )

    companion object {
        fun nuevaPregunta() = TarjetaEditable(tipo = Tarjeta.TIPO_PREGUNTA)
        fun nuevaDefinicion() = TarjetaEditable(tipo = Tarjeta.TIPO_DEFINICION)
    }
}

/** Devuelve el problema de la tarjeta, o null si está lista para guardarse. */
fun validarTarjeta(t: TarjetaEditable): String? {
    if (t.esPregunta) {
        val opciones = t.opciones.map { it.trim() }
        return when {
            t.enunciado.isBlank() -> "Escribe el enunciado de la pregunta"
            t.enunciado.trim().length > 300 -> "El enunciado admite hasta 300 caracteres"
            opciones.any { it.isEmpty() } -> "Completa o elimina las opciones vacías"
            opciones.size < 2 -> "Agrega al menos 2 opciones"
            opciones.map { it.lowercase() }.toSet().size != opciones.size -> "Las opciones deben ser distintas"
            t.correcta !in opciones.indices -> "Marca cuál es la respuesta correcta"
            t.explicacion.trim().length > 600 -> "La explicación admite hasta 600 caracteres"
            else -> null
        }
    }
    return when {
        t.termino.isBlank() -> "Escribe el término del anverso"
        t.termino.trim().length > 120 -> "El término admite hasta 120 caracteres"
        t.definicion.isBlank() -> "Escribe la definición del reverso"
        t.definicion.trim().length > 600 -> "La definición admite hasta 600 caracteres"
        else -> null
    }
}

/** Valida el mazo completo antes de enviarlo a guardar_mazo(). */
fun validarMazo(datos: DatosMazo, tarjetas: List<TarjetaEditable>): String? {
    if (datos.nombre.isBlank()) return "Ponle un nombre al mazo"
    if (datos.nombre.trim().length > 60) return "El nombre admite hasta 60 caracteres"
    if (datos.descripcion.trim().length > 200) return "La descripción admite hasta 200 caracteres"
    if (tarjetas.isEmpty()) return "Agrega al menos una tarjeta"
    tarjetas.forEachIndexed { i, t ->
        validarTarjeta(t)?.let { return "Revisa la tarjeta ${i + 1}: ${it.replaceFirstChar { c -> c.lowercase() }}" }
    }
    return null
}
