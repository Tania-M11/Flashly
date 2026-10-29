package com.udenar.flashly.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.Tarjeta
import com.udenar.flashly.data.TarjetaEditable
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.ChipCategoria
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EncabezadoPantalla
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TarjetaMazo
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.components.iconoCategoria
import com.udenar.flashly.ui.mazos.letraOpcion
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.Paleta

// Colores de portada disponibles (los mismos que admite la base de datos)
private val coloresPortada = listOf(
    ColorFlashly.ROSA to "Rosa", ColorFlashly.MENTA to "Menta", ColorFlashly.LAVANDA to "Lavanda",
    ColorFlashly.MELOCOTON to "Melocotón", ColorFlashly.MANTEQUILLA to "Mantequilla", ColorFlashly.PERIWINKLE to "Periwinkle",
)

/** Pantallas 8 y 9: crear o editar un mazo y sus tarjetas (preguntas y definiciones mezcladas). */
@Composable
fun EditorMazoScreen(onVolver: () -> Unit, onGuardado: (String) -> Unit, vm: EditorMazoViewModel = viewModel()) {
    var descartar by remember { mutableStateOf(false) }
    val salir = { if (vm.hayCambios) descartar = true else onVolver() }
    BackHandler(enabled = vm.enEdicion == null && vm.hayCambios) { descartar = true }

    AnimatedContent(
        targetState = vm.enEdicion != null,
        transitionSpec = {
            if (targetState) (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            else (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
        },
        label = "editor"
    ) { editandoTarjeta ->
        if (editandoTarjeta) EditorTarjetaScreen(vm)
        else EditorDatos(vm, salir, onGuardado)
    }

    if (descartar) {
        DialogoConfirmacion(
            titulo = "¿Descartar cambios?",
            mensaje = if (vm.esNuevo) "El mazo todavía no se ha guardado. Si sales ahora se perderá." else "Tienes cambios sin guardar en este mazo.",
            textoConfirmar = "Descartar", textoCancelar = "Seguir editando",
            onConfirmar = { descartar = false; onVolver() },
            onCancelar = { descartar = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorDatos(vm: EditorMazoViewModel, salir: () -> Unit, onGuardado: (String) -> Unit) {
    var porBorrar by remember { mutableStateOf<TarjetaEditable?>(null) }
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(if (vm.esNuevo) "Nuevo mazo" else "Editar mazo", salir)
        when {
            vm.errorCarga != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoError(vm.errorCarga.orEmpty(), vm::cargar) }
            vm.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { EstadoCarga(texto = "Abriendo el mazo…") }
            else -> Column(
                Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val d = vm.datos
                // Vista previa en vivo de cómo se verá el mazo en la biblioteca
                TarjetaMazo(
                    titulo = d.nombre.ifBlank { "Nombre del mazo" }, numTarjetas = vm.tarjetas.size,
                    color = ColorFlashly.porNombre(d.color), onClick = {}, interactiva = false,
                    descripcion = d.descripcion.ifBlank { null }, esPublico = d.esPublico,
                )

                Text("Datos del mazo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
                CampoTexto(
                    d.nombre, { t -> vm.cambiarDatos { it.copy(nombre = t.take(60)) } }, "Nombre",
                    icono = Icons.Rounded.Title, marcador = "Biología celular", ayuda = "${d.nombre.length}/60"
                )
                CampoTexto(
                    d.descripcion, { t -> vm.cambiarDatos { it.copy(descripcion = t.take(200)) } }, "Descripción (opcional)",
                    marcador = "¿De qué trata este mazo?", unaLinea = false, ayuda = "${d.descripcion.length}/200"
                )

                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.categorias.forEach { c ->
                        ChipCategoria(c.nombre, iconoCategoria(c.icono), ColorFlashly.porNombre(c.color), d.categoriaId == c.id) {
                            vm.cambiarDatos { it.copy(categoriaId = c.id) }
                        }
                    }
                }

                Text("Color de portada", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    coloresPortada.forEach { (color, nombre) ->
                        val elegido = d.color == color.name
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color.fondo)
                                .then(if (elegido) Modifier.border(3.dp, Paleta.VerdeAzuladoOscuro, CircleShape) else Modifier)
                                .selectable(selected = elegido, role = Role.RadioButton) { vm.cambiarDatos { it.copy(color = color.name) } }
                                .semantics { contentDescription = "Color $nombre" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (elegido) Icon(Icons.Rounded.Check, contentDescription = null, tint = color.contenido)
                        }
                    }
                }

                Text("Visibilidad", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OpcionVisibilidad("Privado", "Solo tú puedes verlo", Icons.Rounded.Lock, !d.esPublico, Modifier.weight(1f)) {
                        vm.cambiarDatos { it.copy(esPublico = false) }
                    }
                    OpcionVisibilidad("Público", "Otros pueden verlo y estudiarlo", Icons.Rounded.Public, d.esPublico, Modifier.weight(1f)) {
                        vm.cambiarDatos { it.copy(esPublico = true) }
                    }
                }

                // Tarjetas: se pueden mezclar preguntas y definiciones
                Text("Tarjetas (${vm.tarjetas.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                if (vm.tarjetas.isEmpty()) {
                    Text(
                        "Aún no hay tarjetas. Agrega preguntas de selección múltiple o tarjetas de definición; puedes mezclarlas en el mismo mazo.",
                        style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario
                    )
                }
                vm.tarjetas.forEachIndexed { i, t ->
                    FilaTarjeta(i + 1, t, onEditar = { vm.abrirTarjeta(t) }, onEliminar = { porBorrar = t })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonFlashly("Pregunta", { vm.nuevaTarjeta(Tarjeta.TIPO_PREGUNTA) }, Modifier.weight(1f), estilo = EstiloBoton.Suave, icono = Icons.Rounded.Quiz)
                    BotonFlashly("Definición", { vm.nuevaTarjeta(Tarjeta.TIPO_DEFINICION) }, Modifier.weight(1f), estilo = EstiloBoton.Suave, icono = Icons.AutoMirrored.Rounded.MenuBook)
                }

                MensajeAviso(vm.error, TipoMensaje.Error)
                BotonFlashly(
                    when {
                        vm.guardando -> "Guardando…"
                        vm.esNuevo -> "Guardar mazo"
                        else -> "Guardar cambios"
                    },
                    { vm.guardar(onGuardado) }, Modifier.fillMaxWidth().padding(top = 4.dp),
                    cargando = vm.guardando, habilitado = vm.esNuevo || vm.hayCambios
                )
                BotonFlashly("Cancelar", salir, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    porBorrar?.let { t ->
        DialogoConfirmacion(
            titulo = "¿Eliminar esta tarjeta?",
            mensaje = "«${(if (t.esPregunta) t.enunciado else t.termino).take(60)}» se quitará del mazo al guardar los cambios.",
            textoConfirmar = "Eliminar", peligroso = true,
            onConfirmar = { vm.eliminarTarjeta(t); porBorrar = null },
            onCancelar = { porBorrar = null }
        )
    }
}

/** Tarjeta seleccionable de visibilidad (privado / público) con ícono, título y explicación. */
@Composable
private fun OpcionVisibilidad(titulo: String, detalle: String, icono: ImageVector, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(forma)
            .background(if (elegida) Paleta.Menta.copy(alpha = 0.6f) else Paleta.Blanco)
            .border(if (elegida) 2.dp else 1.dp, if (elegida) Paleta.VerdeAzuladoOscuro else Paleta.Borde, forma)
            .selectable(selected = elegida, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icono, contentDescription = null, tint = Paleta.VerdeAzuladoOscuro, modifier = Modifier.size(20.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (elegida) Icon(Icons.Rounded.Check, contentDescription = "Elegida", tint = Paleta.VerdeAzuladoOscuro, modifier = Modifier.size(18.dp))
        }
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario)
    }
}

/** Resumen de una tarjeta en la lista del mazo: tipo (ícono y texto), contenido y acciones. */
@Composable
private fun FilaTarjeta(numero: Int, t: TarjetaEditable, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val color = if (t.esPregunta) ColorFlashly.LAVANDA else ColorFlashly.MELOCOTON
    TarjetaFlashly(Modifier.fillMaxWidth(), esquinas = 20.dp, onClick = onEditar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.fondo.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Icon(if (t.esPregunta) Icons.Rounded.Quiz else Icons.AutoMirrored.Rounded.MenuBook, null, tint = color.contenido, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    "$numero · ${if (t.esPregunta) "Pregunta de selección" else "Definición"}",
                    style = MaterialTheme.typography.labelMedium, color = color.fuerte
                )
                Text(
                    if (t.esPregunta) t.enunciado else t.termino,
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (t.esPregunta) "${t.opciones.size} opciones · correcta: ${if (t.correcta >= 0) letraOpcion(t.correcta) else "—"}"
                    else t.definicion,
                    style = MaterialTheme.typography.bodySmall, color = Paleta.TextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEditar) { Icon(Icons.Rounded.Edit, contentDescription = "Editar tarjeta $numero", tint = Paleta.TextoSecundario) }
            IconButton(onClick = onEliminar) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar tarjeta $numero", tint = Paleta.Error) }
        }
    }
}
