package com.udenar.flashly

import com.udenar.flashly.data.errorConfirmacion
import com.udenar.flashly.data.errorCorreo
import com.udenar.flashly.data.errorTelefono
import com.udenar.flashly.data.errorUsuario
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.normalizarUsuario
import com.udenar.flashly.data.requisitosContrasena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionCuentaTest {

    @Test
    fun contrasenaFuerte_cumpleTodo() {
        assertTrue(requisitosContrasena("Flashly#2026").cumpleTodos)
    }

    @Test
    fun contrasenaSinEspecial_noCumple() {
        val r = requisitosContrasena("Flashly2026")
        assertFalse(r.especial)
        assertFalse(r.cumpleTodos)
        assertTrue(r.largo && r.mayuscula && r.minuscula && r.numero)
    }

    @Test
    fun contrasenaCorta_noCumpleLargo() {
        assertFalse(requisitosContrasena("Fl#1a").largo)
    }

    @Test
    fun usuario_seNormalizaYValida() {
        assertEquals("laura_g", normalizarUsuario("  Laura_G "))
        assertNull(errorUsuario("Laura_G"))
        assertNotNull(errorUsuario("ta"))
        assertNotNull(errorUsuario("laura gomez"))
        assertNotNull(errorUsuario("añoranza"))
    }

    @Test
    fun telefono_aceptaFormatosComunes() {
        assertNull(errorTelefono("300 123 4567"))
        assertNull(errorTelefono("+57 300 123 4567"))
        assertNotNull(errorTelefono(""))
        assertNotNull(errorTelefono("12345"))
        assertNotNull(errorTelefono("300-123-4567"))
    }

    @Test
    fun correoYConfirmacion() {
        assertNull(errorCorreo("laura@gmail.com"))
        assertNotNull(errorCorreo("laura@"))
        assertEquals("Las contraseñas no coinciden", errorConfirmacion("Flashly#2026", "Flashly#2025"))
        assertNull(errorConfirmacion("Flashly#2026", "Flashly#2026"))
    }

    @Test
    fun erroresDeRegistro_seTraducen() {
        assertEquals(
            "Ese nombre de usuario ya está en uso",
            mensajeError(Exception("duplicate key value violates unique constraint \"perfiles_nombre_usuario_key\""))
        )
        assertEquals("El teléfono es obligatorio", mensajeError(Exception("El teléfono es obligatorio")))
    }
}
