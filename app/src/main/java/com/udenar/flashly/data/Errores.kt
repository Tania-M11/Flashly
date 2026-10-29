package com.udenar.flashly.data

import java.io.IOException

/** Convierte excepciones técnicas en mensajes entendibles para el usuario. */
fun mensajeError(e: Throwable): String {
    val m = e.message.orEmpty()
    val bajo = m.lowercase()
    return when {
        "invalid login credentials" in bajo -> "Correo o contraseña incorrectos"
        "already registered" in bajo || "already been registered" in bajo -> "Ese correo ya está registrado"
        "perfiles_nombre_usuario_key" in bajo -> "Ese nombre de usuario ya está en uso"
        "weak_password" in bajo || "password should" in bajo -> "La contraseña no cumple los requisitos de seguridad"
        "email_address_invalid" in bajo || "invalid format" in bajo -> "Ese correo no es válido"
        "rate limit" in bajo -> "Demasiados intentos, espera un momento"
        // Los fallos de red de supabase-kt (HttpRequestException) son IOException
        e is IOException || "unable to resolve host" in bajo || "timeout" in bajo ->
            "No hay conexión con el servidor. Revisa tu Internet"
        m.isBlank() -> "Ocurrió un error inesperado"
        else -> m.lineSequence().first().take(200)
    }
}
