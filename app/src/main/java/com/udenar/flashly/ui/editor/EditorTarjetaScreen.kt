package com.udenar.flashly.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.udenar.flashly.data.Tarjeta
import com.udenar.flashly.data.TarjetaEditable
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TarjetaVolteable
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.mazos.letraOpcion
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

/** Pantalla 9: añadir o editar una tarjeta. Se puede cambiar el tipo sin perder lo escrito. */
@Composable
fun EditorTarjetaScreen(vm: EditorMazoViewModel) {
    val t = vm.enEdicion ?: return
    BackHandler { vm.cerrarTarjeta() }
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(if (vm.esTarjetaNueva) "Nueva tarjeta" else "Editar tarjeta", vm::cerrarTarjeta)
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Tipo de tarjeta", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OpcionTipo("Pregunta de selección", "Varias opciones, una correcta", Icons.Rounded.Quiz, ColorFlashly.LAVANDA, t.esPregunta, Modifier.weight(1f)) {
                    vm.cambiarTarjeta { it.copy(tipo = Tarjeta.TIPO_PREGUNTA) }
                }
                OpcionTipo("Definición", "Término y su significado", Icons.AutoMirrored.Rounded.MenuBook, ColorFlashly.MELOCOTON, !t.esPregunta, Modifier.weight(1f)) {
                    vm.cambiarTarjeta { it.copy(tipo = Tarjeta.TIPO_DEFINICION) }
                }
            }

            if (t.esPregunta) EditorPregunta(t, vm) else EditorDefinicion(t, vm)

            MensajeAviso(vm.errorTarjeta, TipoMensaje.Error)
            BotonFlashly("Guardar tarjeta", { vm.guardarTarjeta() }, Modifier.fillMaxWidth(), icono = Icons.Rounded.Check)
            BotonFlashly("Cancelar", vm::cerrarTarjeta, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Tipo A: enunciado, lista dinámica de opciones (sin máximo, mínimo 2), correcta y explicación. */
@Composable
private fun EditorPregunta(t: TarjetaEditable, vm: EditorMazoViewModel) {
    CampoTexto(
        t.enunciado, { s -> vm.cambiarTarjeta { it.copy(enunciado = s.take(300)) } }, "Enunciado de la pregunta",
        icono = Icons.Rounded.Quiz, marcador = "¿Qué organelo produce la energía de la célula?", unaLinea = false,
        mayusculas = KeyboardCapitalization.Sentences, ayuda = "${t.enunciado.length}/300"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Opciones de respuesta (${t.opciones.size})", style = MaterialTheme.typography.labelLarge)
        Text("Toca la letra de la opción correcta para marcarla.", style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario)
    }
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        t.opciones.forEachIndexed { i, opcion ->
            FilaOpcionEditable(
                letra = letraOpcion(i), texto = opcion, correcta = t.correcta == i,
                puedeQuitarse = t.opciones.size > 2,
                onTexto = { s -> vm.cambiarTarjeta { it.copy(opciones = it.opciones.mapIndexed { k, o -> if (k == i) s.take(200) else o }) } },
                onMarcar = { vm.cambiarTarjeta { it.copy(correcta = i) } },
                onQuitar = { vm.cambiarTarjeta { it.sinOpcion(i) } },
            )
        }
    }
    BotonFlashly(
        "Añadir opción", { vm.cambiarTarjeta { it.copy(opciones = it.opciones + "") } },
        Modifier.fillMaxWidth(), estilo = EstiloBoton.Suave, icono = Icons.Rounded.Add
    )
    CampoTexto(
        t.explicacion, { s -> vm.cambiarTarjeta { it.copy(explicacion = s.take(600)) } }, "Explicación (opcional)",
        icono = Icons.Rounded.Lightbulb, marcador = "Por qué esa es la respuesta correcta", unaLinea = false,
        mayusculas = KeyboardCapitalization.Sentences, ayuda = "Se muestra después de responder. No es visible antes de contestar."
    )
}

/** Una opción: letra que sirve para marcarla como correcta, su texto y el botón para quitarla. */
@Composable
private fun FilaOpcionEditable(
    letra: String,
    texto: String,
    correcta: Boolean,
    puedeQuitarse: Boolean,
    onTexto: (String) -> Unit,
    onMarcar: () -> Unit,
    onQuitar: () -> Unit,
) {
    val fondo by animateColorAsState(if (correcta) Paleta.Exito else Paleta.Crema, label = "correcta")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(fondo)
                .then(if (!correcta) Modifier.border(1.dp, Paleta.Borde, CircleShape) else Modifier)
                .selectable(selected = correcta, role = Role.RadioButton, onClick = onMarcar)
                .semantics { contentDescription = if (correcta) "Opción $letra, respuesta correcta" else "Marcar la opción $letra como correcta" },
            contentAlignment = Alignment.Center
        ) {
            if (correcta) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
            else Text(letra, style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(
            value = texto,
            onValueChange = onTexto,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Opción $letra", color = Paleta.TextoSecundario.copy(alpha = 0.7f)) },
            supportingText = if (correcta) { { Text("Respuesta correcta", color = Paleta.Exito) } } else null,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Paleta.Blanco, unfocusedContainerColor = Paleta.Blanco,
                focusedBorderColor = if (correcta) Paleta.Exito else Paleta.VerdeAzulado,
                unfocusedBorderColor = if (correcta) Paleta.Exito.copy(alpha = 0.7f) else Paleta.Borde,
                cursorColor = Paleta.VerdeAzuladoOscuro,
            ),
        )
        IconButton(onClick = onQuitar, enabled = puedeQuitarse) {
            Icon(
                Icons.Rounded.Close, contentDescription = "Quitar la opción $letra",
                tint = if (puedeQuitarse) Paleta.TextoSecundario else Paleta.Borde
            )
        }
    }
}

/** Tipo B: término (anverso) y definición (reverso), con vista previa que se puede girar. */
@Composable
private fun EditorDefinicion(t: TarjetaEditable, vm: EditorMazoViewModel) {
    var volteada by remember { mutableStateOf(false) }
    CampoTexto(
        t.termino, { s -> vm.cambiarTarjeta { it.copy(termino = s.take(120)) } }, "Término (anverso)",
        icono = Icons.AutoMirrored.Rounded.MenuBook, marcador = "Mitocondria",
        mayusculas = KeyboardCapitalization.Sentences, ayuda = "${t.termino.length}/120"
    )
    CampoTexto(
        t.definicion, { s -> vm.cambiarTarjeta { it.copy(definicion = s.take(600)) } }, "Definición (reverso)",
        marcador = "Organelo que produce la energía de la célula.", unaLinea = false,
        mayusculas = KeyboardCapitalization.Sentences, ayuda = "${t.definicion.length}/600"
    )
    Text("Vista previa", style = MaterialTheme.typography.labelLarge)
    TarjetaVolteable(
        volteada = volteada, onVoltear = { volteada = !volteada },
        modifier = Modifier.fillMaxWidth().height(190.dp),
        colorAnverso = Paleta.Blanco, colorReverso = ColorFlashly.MELOCOTON.fondo,
        anverso = {
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(t.termino.ifBlank { "Término" }, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                        color = if (t.termino.isBlank()) Paleta.TextoSecundario else Paleta.Texto)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TouchApp, null, tint = Paleta.TextoSecundario, modifier = Modifier.size(16.dp))
                    Text(" Toca para ver la definición", style = MaterialTheme.typography.labelMedium, color = Paleta.TextoSecundario)
                }
            }
        },
        reverso = {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(t.definicion.ifBlank { "Definición" }, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = ColorFlashly.MELOCOTON.contenido)
            }
        }
    )
}

/** Selector visual del tipo de tarjeta (ícono, título y descripción; el elegido lleva borde y check). */
@Composable
private fun OpcionTipo(titulo: String, detalle: String, icono: ImageVector, color: ColorFlashly, elegido: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(forma)
            .background(if (elegido) color.fondo else Paleta.Blanco)
            .border(if (elegido) 2.dp else 1.dp, if (elegido) color.fuerte else Paleta.Borde, forma)
            .selectable(selected = elegido, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = if (elegido) color.contenido else color.fuerte)
            Spacer(Modifier.weight(1f))
            if (elegido) Icon(Icons.Rounded.Check, contentDescription = "Elegido", tint = color.contenido, modifier = Modifier.size(18.dp))
        }
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = if (elegido) color.contenido else Paleta.Texto)
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = if (elegido) color.contenido.copy(alpha = 0.8f) else Paleta.TextoSecundario)
    }
}
