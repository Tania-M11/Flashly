package com.udenar.flashly

import com.udenar.flashly.data.DatosMazo
import com.udenar.flashly.data.TarjetaEditable
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.validarMazo
import com.udenar.flashly.data.validarTarjeta
import com.udenar.flashly.ui.mazos.letraOpcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EditorMazoTest {
    private val pregunta = TarjetaEditable.nuevaPregunta().copy(
        enunciado = "¿Capital de Colombia?", opciones = listOf("Bogotá", "Lima", "Quito", "Caracas", "Asunción"), correcta = 0
    )
    private val definicion = TarjetaEditable.nuevaDefinicion().copy(termino = "Ribosoma", definicion = "Sintetiza proteínas.")
    private val datos = DatosMazo(nombre = "Geografía")

    @Test
    fun preguntaConCincoOpciones_esValida() {
        assertNull(validarTarjeta(pregunta))
    }

    @Test
    fun preguntaSinCorrecta_oConOpcionVacia_oRepetida_noEsValida() {
        assertEquals("Marca cuál es la respuesta correcta", validarTarjeta(pregunta.copy(correcta = -1)))
        assertNotNull(validarTarjeta(pregunta.copy(opciones = listOf("Bogotá", ""))))
        assertNotNull(validarTarjeta(pregunta.copy(opciones = listOf("Sí", "sí"))))
        assertNotNull(validarTarjeta(pregunta.copy(opciones = listOf("Única"), correcta = 0)))
    }

    @Test
    fun definicionNecesitaAmbasCaras() {
        assertNull(validarTarjeta(definicion))
        assertNotNull(validarTarjeta(definicion.copy(definicion = " ")))
        assertNotNull(validarTarjeta(definicion.copy(termino = "")))
    }

    @Test
    fun quitarOpcion_corrigeLaCorrecta() {
        val p = pregunta.copy(correcta = 3)
        assertEquals(2, p.sinOpcion(0).correcta)      // se quitó una anterior: la correcta baja un puesto
        assertEquals(3, p.sinOpcion(4).correcta)      // se quitó una posterior: no cambia
        assertEquals(-1, p.sinOpcion(3).correcta)     // se quitó la correcta: queda sin marcar
    }

    @Test
    fun mazo_validaNombreTarjetasYDiceCualFalla() {
        assertNull(validarMazo(datos, listOf(pregunta, definicion)))
        assertEquals("Ponle un nombre al mazo", validarMazo(DatosMazo(), listOf(pregunta)))
        assertEquals("Agrega al menos una tarjeta", validarMazo(datos, emptyList()))
        assertEquals(
            "Revisa la tarjeta 2: escribe la definición del reverso",
            validarMazo(datos, listOf(pregunta, definicion.copy(definicion = "")))
        )
    }

    @Test
    fun letrasDeOpciones_sinLimite() {
        assertEquals("A", letraOpcion(0))
        assertEquals("E", letraOpcion(4))
        assertEquals("Z", letraOpcion(25))
        assertEquals("AA", letraOpcion(26))
    }

    @Test
    fun errorDeRed_seTraduce() {
        assertEquals(
            "No hay conexión con el servidor. Revisa tu Internet",
            mensajeError(java.io.IOException("HTTP request failed with message: Software caused connection abort"))
        )
    }
}
