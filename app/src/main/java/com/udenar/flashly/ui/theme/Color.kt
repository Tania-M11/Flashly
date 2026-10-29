package com.udenar.flashly.ui.theme

import androidx.compose.ui.graphics.Color

/** Paleta pastel de Flashly (documento "Contexto Flashly", sección 2). */
object Paleta {
    val RosaPastel = Color(0xFFF4A0B5)
    val RosaSuave = Color(0xFFFBE3ED)
    val Crema = Color(0xFFFFFBEA)
    val Menta = Color(0xFFBDE8D5)
    val VerdeAzulado = Color(0xFF398F83)
    val VerdeAzuladoOscuro = Color(0xFF246B65)
    val Lavanda = Color(0xFFC8C5FF)
    val Periwinkle = Color(0xFF9299F5)
    val Melocoton = Color(0xFFFFB477)
    val Mantequilla = Color(0xFFF7D66D)
    val Fondo = Color(0xFFFFF9F5)
    val Texto = Color(0xFF252525)
    // Un poco más oscuro que #777777 para cumplir contraste AA sobre el fondo claro
    val TextoSecundario = Color(0xFF6B6B6B)
    val Blanco = Color(0xFFFFFFFF)

    // Apoyo (derivados de la paleta para estados y bordes)
    val Borde = Color(0xFFEADFD6)
    val BordeSuave = Color(0xFFF3EAE2)
    val Error = Color(0xFFC4425A)
    val ErrorSuave = Color(0xFFFDE1E6)
    val Exito = Color(0xFF2E7D5B)
    val ExitoSuave = Color(0xFFDDF3E8)
    val Carbon = Color(0xFF1F2A28)
}

/** Convierte "#RRGGBB" (como los guarda la base de datos) en Color; si es inválido usa lavanda. */
fun colorDeHex(hex: String?): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Paleta.Lavanda)

/**
 * Colores de portada para mazos, modos y tarjetas. Cada uno trae su color de texto
 * para mantener el contraste y el tono "fuerte" para íconos o detalles.
 */
enum class ColorFlashly(val fondo: Color, val contenido: Color, val fuerte: Color) {
    ROSA(Paleta.RosaPastel, Color(0xFF4A1625), Color(0xFFB8435F)),
    MENTA(Paleta.Menta, Color(0xFF0F3D37), Paleta.VerdeAzulado),
    LAVANDA(Paleta.Lavanda, Color(0xFF26215C), Color(0xFF5B55C9)),
    MELOCOTON(Paleta.Melocoton, Color(0xFF4A2508), Color(0xFFB8621E)),
    MANTEQUILLA(Paleta.Mantequilla, Color(0xFF3F3205), Color(0xFF9A7B12)),
    PERIWINKLE(Paleta.Periwinkle, Color(0xFF15194D), Color(0xFF3E46B8)),
    VERDE(Paleta.VerdeAzulado, Paleta.Blanco, Paleta.VerdeAzuladoOscuro);

    companion object {
        /** Color estable a partir de un texto (por ejemplo, el id de un mazo). */
        fun desde(clave: String?): ColorFlashly =
            entries[(clave?.hashCode() ?: 0).mod(entries.size - 1)]   // sin VERDE: se reserva para acciones

        fun porNombre(nombre: String?): ColorFlashly =
            entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) } ?: ROSA
    }
}
