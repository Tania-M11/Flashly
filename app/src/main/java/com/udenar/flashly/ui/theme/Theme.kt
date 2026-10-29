package com.udenar.flashly.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Flashly usa siempre su paleta clara (sin color dinámico ni modo oscuro) para conservar su identidad.
private val Esquema = lightColorScheme(
    primary = Paleta.VerdeAzuladoOscuro,
    onPrimary = Paleta.Blanco,
    primaryContainer = Paleta.Menta,
    onPrimaryContainer = Color(0xFF0F3D37),
    secondary = Paleta.RosaPastel,
    onSecondary = Color(0xFF4A1625),
    secondaryContainer = Paleta.RosaSuave,
    onSecondaryContainer = Color(0xFF4A1625),
    tertiary = Paleta.Periwinkle,
    onTertiary = Color(0xFF15194D),
    tertiaryContainer = Paleta.Lavanda,
    onTertiaryContainer = Color(0xFF26215C),
    background = Paleta.Fondo,
    onBackground = Paleta.Texto,
    surface = Paleta.Blanco,
    onSurface = Paleta.Texto,
    surfaceVariant = Paleta.Crema,
    onSurfaceVariant = Paleta.TextoSecundario,
    surfaceContainerLowest = Paleta.Blanco,
    surfaceContainerLow = Color(0xFFFFFCF8),
    surfaceContainer = Color(0xFFFFF6EF),
    surfaceContainerHigh = Color(0xFFFBF0E7),
    surfaceContainerHighest = Color(0xFFF6EAE0),
    outline = Paleta.Borde,
    outlineVariant = Paleta.BordeSuave,
    error = Paleta.Error,
    onError = Paleta.Blanco,
    errorContainer = Paleta.ErrorSuave,
    onErrorContainer = Color(0xFF5C1020),
    inverseSurface = Paleta.Carbon,
    inverseOnSurface = Paleta.Blanco,
)

val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun FlashlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Esquema,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
