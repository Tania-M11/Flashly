package com.udenar.flashly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.udenar.flashly.ui.theme.Paleta

/**
 * Diálogo de confirmación (eliminar mazo o tarjeta, cerrar sesión, abandonar una sesión de estudio).
 * Con [peligroso] el botón de confirmar es rojo.
 */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoCancelar: String = "Cancelar",
    peligroso: Boolean = false,
    icono: ImageVector = if (peligroso) Icons.Rounded.DeleteOutline else Icons.AutoMirrored.Rounded.HelpOutline,
) {
    Dialog(onDismissRequest = onCancelar) {
        Surface(shape = RoundedCornerShape(32.dp), color = Paleta.Fondo, tonalElevation = 0.dp) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(if (peligroso) Paleta.RosaSuave else Paleta.Menta),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = if (peligroso) Paleta.Error else Paleta.VerdeAzuladoOscuro, modifier = Modifier.size(30.dp))
                }
                Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario, textAlign = TextAlign.Center)
                // Botones apilados a todo el ancho: los textos largos («Seguir editando») no se parten
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonFlashly(
                        textoConfirmar, onConfirmar, Modifier.fillMaxWidth(),
                        estilo = if (peligroso) EstiloBoton.Peligro else EstiloBoton.Principal
                    )
                    BotonFlashly(textoCancelar, onCancelar, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
                }
            }
        }
    }
}
