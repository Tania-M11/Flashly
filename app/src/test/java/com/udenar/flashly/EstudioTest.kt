package com.udenar.flashly

import com.udenar.flashly.data.ModoJuego
import com.udenar.flashly.data.ResultadoRespuesta
import com.udenar.flashly.ui.estudio.textoProximoRepaso
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstudioTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `la respuesta del servidor con resumen se decodifica y separa preguntas de definiciones`() {
        val r = json.decodeFromString<ResultadoRespuesta>(
            """{"resultado":"correcta","correcta":true,"indice_correcto":1,"explicacion":null,"xp":10,"xp_sesion":24,
               "combo":2,"vidas":2,"caja":1,"resumen":{"id":"s1","mazo_id":"m1","mazo_nombre":"Biología","mazo_color":"MENTA",
               "modo":"reto","estado":"completada","vidas":2,"aciertos":3,"errores":1,"lo_se":2,"practicar":1,"mejor_combo":2,
               "xp":34,"xp_bono":34,"xp_total":68,"xp_doble":true,"iniciada_en":"2026-10-29T15:00:00+00:00",
               "total_tarjetas":7,"revisadas":7,"refuerzo":["Mitocondria"]}}"""
        )
        val s = r.resumen!!
        assertEquals(ModoJuego.RETO, s.modoJuego)
        assertEquals(75, s.puntuacion)          // 3 de 4 preguntas
        assertEquals(3, s.definiciones)          // la autoevaluación va aparte
        assertEquals(7, s.revisadas)
        assertTrue(s.xpDoble)
    }

    @Test
    fun `sin preguntas evaluables no se inventa una puntuacion`() {
        val r = json.decodeFromString<ResultadoRespuesta>(
            """{"resultado":"lo_se","xp":2,"resumen":{"id":"s","mazo_nombre":"Verbos","modo":"practica","estado":"completada",
               "lo_se":4,"practicar":1,"iniciada_en":"2026-10-29T15:00:00+00:00"}}"""
        )
        assertNull(r.correcta)
        assertNull(r.resumen!!.puntuacion)
    }

    @Test
    fun `modo desconocido cae en practica y los textos de repaso siguen las cajas`() {
        assertEquals(ModoJuego.PRACTICA, ModoJuego.de("otro"))
        assertEquals(ModoJuego.CONTRARRELOJ, ModoJuego.de("contrarreloj"))
        assertEquals("Volverá hoy para reforzarla", textoProximoRepaso(0))
        assertEquals("Volverá mañana", textoProximoRepaso(1))
        assertEquals("Volverá en 30 días", textoProximoRepaso(5))
    }
}
