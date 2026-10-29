package com.udenar.flashly.data

/** Requisitos de la contraseña; la pantalla de registro marca cuáles se cumplen mientras se escribe. */
data class RequisitosContrasena(
    val largo: Boolean,
    val mayuscula: Boolean,
    val minuscula: Boolean,
    val numero: Boolean,
    val especial: Boolean,
) {
    val cumpleTodos get() = largo && mayuscula && minuscula && numero && especial
}

fun requisitosContrasena(clave: String) = RequisitosContrasena(
    largo = clave.length >= 8,
    mayuscula = clave.any { it.isUpperCase() },
    minuscula = clave.any { it.isLowerCase() },
    numero = clave.any { it.isDigit() },
    especial = clave.any { !it.isLetterOrDigit() && !it.isWhitespace() },
)

/** El nombre de usuario se guarda en minúsculas y sin espacios alrededor. */
fun normalizarUsuario(texto: String) = texto.trim().lowercase()

private val formatoUsuario = Regex("^[a-z0-9_.]{3,20}$")
private val formatoCorreo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
private val formatoTelefono = Regex("^\\+?[0-9 ]+$")

fun errorNombre(nombre: String): String? = when {
    nombre.isBlank() -> "Escribe tu nombre completo"
    nombre.trim().length < 2 -> "El nombre es demasiado corto"
    nombre.trim().length > 80 -> "Máximo 80 caracteres"
    else -> null
}

fun errorUsuario(usuario: String): String? {
    val u = normalizarUsuario(usuario)
    return when {
        u.isEmpty() -> "Elige un nombre de usuario"
        u.length < 3 -> "Mínimo 3 caracteres"
        u.length > 20 -> "Máximo 20 caracteres"
        !formatoUsuario.matches(u) -> "Solo letras sin tilde, números, punto y guion bajo"
        else -> null
    }
}

fun errorTelefono(telefono: String): String? {
    val t = telefono.trim()
    val digitos = t.count { it.isDigit() }
    return when {
        t.isEmpty() -> "El teléfono es obligatorio"
        !formatoTelefono.matches(t) -> "Usa solo números (puede empezar con +)"
        digitos !in 7..15 -> "Debe tener entre 7 y 15 dígitos"
        else -> null
    }
}

fun errorCorreo(correo: String): String? = when {
    correo.isBlank() -> "Escribe tu correo"
    !formatoCorreo.matches(correo.trim()) -> "Escribe un correo válido"
    else -> null
}

fun errorConfirmacion(clave: String, repetida: String): String? = when {
    repetida.isEmpty() -> "Repite la contraseña"
    clave != repetida -> "Las contraseñas no coinciden"
    else -> null
}
