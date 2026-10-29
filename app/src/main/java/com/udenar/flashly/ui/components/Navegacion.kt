package com.udenar.flashly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.udenar.flashly.ui.theme.Paleta

data class ItemBarra(val etiqueta: String, val icono: ImageVector, val iconoActivo: ImageVector = icono)

/**
 * Barra inferior de las 4 secciones. La sección activa lleva una píldora menta detrás del ícono
 * y la etiqueta en negrita (no depende solo del color). Respeta la barra de navegación del sistema.
 */
@Composable
fun BarraInferior(items: List<ItemBarra>, seleccionado: Int, onSeleccion: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Paleta.Blanco,
        shadowElevation = 16.dp,
    ) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEachIndexed { i, item ->
                val activo = i == seleccionado
                val ancho by animateDpAsState(if (activo) 64.dp else 40.dp, label = "pildora")
                val fondo by animateColorAsState(if (activo) Paleta.Menta else Color.Transparent, label = "fondo")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .selectable(selected = activo, role = Role.Tab, onClick = { onSeleccion(i) })
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.width(ancho).height(34.dp).clip(CircleShape).background(fondo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (activo) item.iconoActivo else item.icono,
                            contentDescription = null,
                            tint = if (activo) Paleta.VerdeAzuladoOscuro else Paleta.TextoSecundario,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        item.etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activo) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (activo) Paleta.VerdeAzuladoOscuro else Paleta.TextoSecundario
                    )
                }
            }
        }
    }
}

/** Encabezado de pantallas secundarias: botón circular de volver, título y acción opcional. */
@Composable
fun EncabezadoPantalla(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BotonCircular(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onVolver)
        Text(titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        accion?.invoke()
    }
}

/** Botón redondo blanco con sombra suave, para volver, configuración, etc. */
@Composable
fun BotonCircular(icono: ImageVector, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .shadow(4.dp, CircleShape, ambientColor = Color(0x22B08A70), spotColor = Color(0x22B08A70))
            .clip(CircleShape)
            .background(Paleta.Blanco)
    ) {
        Icon(icono, contentDescription = descripcion, tint = Paleta.Texto)
    }
}
