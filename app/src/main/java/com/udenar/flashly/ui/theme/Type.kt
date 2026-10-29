package com.udenar.flashly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.udenar.flashly.R

// Fraunces (títulos) y Montserrat (resto) son fuentes variables: un solo archivo por familia.
// En Fraunces se usa el eje SOFT al máximo para que las serifas se vean redondeadas y amables.
@OptIn(ExperimentalTextApi::class)
private fun fraunces(peso: FontWeight) = Font(
    R.font.fraunces, peso,
    variationSettings = FontVariation.Settings(
        peso, FontStyle.Normal,
        FontVariation.Setting("SOFT", 100f),
        FontVariation.Setting("opsz", 48f),
    )
)

@OptIn(ExperimentalTextApi::class)
private fun montserrat(peso: FontWeight) = Font(
    R.font.montserrat, peso,
    variationSettings = FontVariation.Settings(peso, FontStyle.Normal)
)

val Fraunces = FontFamily(
    fraunces(FontWeight.Medium), fraunces(FontWeight.SemiBold), fraunces(FontWeight.Bold), fraunces(FontWeight.Black)
)

val Montserrat = FontFamily(
    montserrat(FontWeight.Normal), montserrat(FontWeight.Medium), montserrat(FontWeight.SemiBold),
    montserrat(FontWeight.Bold), montserrat(FontWeight.ExtraBold)
)

private fun titulo(tamano: Int, linea: Int, peso: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Fraunces, fontWeight = peso, fontSize = tamano.sp, lineHeight = linea.sp)

private fun texto(tamano: Int, linea: Int, peso: FontWeight = FontWeight.Normal, espaciado: Float = 0f) =
    TextStyle(fontFamily = Montserrat, fontWeight = peso, fontSize = tamano.sp, lineHeight = linea.sp, letterSpacing = espaciado.sp)

val Typography = Typography(
    displayLarge = titulo(52, 56, FontWeight.Black),
    displayMedium = titulo(42, 48, FontWeight.Black),
    displaySmall = titulo(34, 40),
    headlineLarge = titulo(30, 36),
    headlineMedium = titulo(26, 32),
    headlineSmall = titulo(22, 28),
    titleLarge = titulo(20, 26, FontWeight.SemiBold),
    titleMedium = texto(16, 22, FontWeight.SemiBold),
    titleSmall = texto(14, 20, FontWeight.SemiBold),
    bodyLarge = texto(16, 24),
    bodyMedium = texto(14, 20),
    bodySmall = texto(12, 16),
    labelLarge = texto(14, 20, FontWeight.Bold, 0.1f),
    labelMedium = texto(12, 16, FontWeight.SemiBold, 0.2f),
    labelSmall = texto(11, 14, FontWeight.SemiBold, 0.3f),
)
