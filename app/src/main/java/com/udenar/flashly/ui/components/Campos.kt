package com.udenar.flashly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.Paleta

/**
 * Campo de texto con etiqueta visible encima, ícono discreto y tres estados:
 * normal, válido (check verde) y con error (borde y mensaje en rojo, con ícono para no depender del color).
 * Si [esContrasena] es true incluye el botón para mostrar u ocultar.
 */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    marcador: String? = null,
    error: String? = null,
    valido: Boolean = false,
    ayuda: String? = null,
    esContrasena: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    mayusculas: KeyboardCapitalization = KeyboardCapitalization.None,
    accionTeclado: ImeAction = ImeAction.Next,
    alAccion: () -> Unit = {},
    unaLinea: Boolean = true,
    habilitado: Boolean = true,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = Paleta.Texto)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado,
            singleLine = unaLinea,
            minLines = if (unaLinea) 1 else 3,
            placeholder = marcador?.let { { Text(it, color = Paleta.TextoSecundario.copy(alpha = 0.7f)) } },
            leadingIcon = icono?.let { { Icon(it, contentDescription = null, tint = Paleta.TextoSecundario) } },
            trailingIcon = when {
                esContrasena -> {
                    {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = Paleta.TextoSecundario
                            )
                        }
                    }
                }
                error != null -> { { Icon(Icons.Rounded.ErrorOutline, contentDescription = "Campo con error", tint = Paleta.Error) } }
                valido -> { { Icon(Icons.Rounded.CheckCircle, contentDescription = "Campo válido", tint = Paleta.Exito) } }
                else -> null
            },
            isError = error != null,
            visualTransformation = if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
                capitalization = mayusculas,
                autoCorrectEnabled = !esContrasena && tipoTeclado == KeyboardType.Text,
                imeAction = accionTeclado
            ),
            keyboardActions = KeyboardActions(onAny = { alAccion() }),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Paleta.Blanco,
                unfocusedContainerColor = Paleta.Blanco,
                disabledContainerColor = Paleta.BordeSuave,
                errorContainerColor = Paleta.Blanco,
                focusedBorderColor = Paleta.VerdeAzulado,
                unfocusedBorderColor = if (valido) Paleta.Exito.copy(alpha = 0.6f) else Paleta.Borde,
                errorBorderColor = Paleta.Error,
                cursorColor = Paleta.VerdeAzuladoOscuro,
            ),
        )
        when {
            error != null -> MensajeCampo(error, esError = true)
            ayuda != null -> Text(ayuda, style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario)
        }
    }
}

@Composable
private fun MensajeCampo(texto: String, esError: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Icon(
            Icons.Rounded.ErrorOutline, contentDescription = null,
            tint = if (esError) Paleta.Error else Paleta.TextoSecundario,
            modifier = Modifier.size(16.dp).padding(end = 2.dp)
        )
        Text(" $texto", style = MaterialTheme.typography.bodySmall, color = if (esError) Paleta.Error else Paleta.TextoSecundario)
    }
}
