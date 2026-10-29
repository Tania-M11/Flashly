package com.udenar.flashly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.Paleta

enum class EstiloBoton { Principal, Secundario, Suave, Texto, Peligro }

/**
 * Botón de Flashly: forma de píldora, 56 dp de alto y estado de carga integrado.
 * - Principal: verde azulado oscuro, para la acción más importante de la pantalla.
 * - Secundario: contorno, para la alternativa.
 * - Suave: menta, para acciones frecuentes que no deben competir con la principal.
 * - Texto: enlaces como «Ya tengo una cuenta».
 * - Peligro: acciones destructivas (eliminar).
 */
@Composable
fun BotonFlashly(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    estilo: EstiloBoton = EstiloBoton.Principal,
    icono: ImageVector? = null,
    cargando: Boolean = false,
    habilitado: Boolean = true,
) {
    // Mientras carga conserva su color (no se ve "deshabilitado"), pero ignora toques repetidos
    val activo = habilitado
    val alPulsar = { if (!cargando) onClick() }
    val contenido: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = if (estilo == EstiloBoton.Principal || estilo == EstiloBoton.Peligro) Color.White
                    else Paleta.VerdeAzuladoOscuro
                )
                Spacer(Modifier.width(10.dp))
            } else if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto)
        }
    }
    val alto = Modifier.defaultMinSize(minHeight = if (estilo == EstiloBoton.Texto) 44.dp else 56.dp)
    val relleno = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

    when (estilo) {
        EstiloBoton.Principal, EstiloBoton.Suave, EstiloBoton.Peligro -> Button(
            onClick = alPulsar,
            enabled = activo,
            modifier = modifier.then(alto),
            shape = CircleShape,
            contentPadding = relleno,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (estilo) {
                    EstiloBoton.Principal -> Paleta.VerdeAzuladoOscuro
                    EstiloBoton.Peligro -> Paleta.Error
                    else -> Paleta.Menta
                },
                contentColor = if (estilo == EstiloBoton.Suave) Color(0xFF0F3D37) else Color.White,
                disabledContainerColor = Paleta.BordeSuave,
                disabledContentColor = Paleta.TextoSecundario,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        ) { contenido() }

        EstiloBoton.Secundario -> OutlinedButton(
            onClick = alPulsar,
            enabled = activo,
            modifier = modifier.then(alto),
            shape = CircleShape,
            contentPadding = relleno,
            border = BorderStroke(1.5.dp, if (activo) Paleta.VerdeAzuladoOscuro else Paleta.Borde),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Paleta.Blanco,
                contentColor = Paleta.VerdeAzuladoOscuro,
            ),
        ) { contenido() }

        EstiloBoton.Texto -> TextButton(
            onClick = alPulsar,
            enabled = activo,
            modifier = modifier.then(alto),
            colors = ButtonDefaults.textButtonColors(contentColor = Paleta.VerdeAzuladoOscuro),
        ) { contenido() }
    }
}
