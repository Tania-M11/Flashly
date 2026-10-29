package com.udenar.flashly.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tarjeta que gira en 3D al tocarla: muestra [anverso] y, tras el giro, [reverso].
 * Se usa en la vista previa de mazos y en las sesiones de estudio (tarjetas de definición).
 */
@Composable
fun TarjetaVolteable(
    volteada: Boolean,
    onVoltear: () -> Unit,
    modifier: Modifier = Modifier,
    colorAnverso: Color,
    colorReverso: Color,
    esquinas: Dp = 28.dp,
    anverso: @Composable BoxScope.() -> Unit,
    reverso: @Composable BoxScope.() -> Unit,
) {
    val giro by animateFloatAsState(
        targetValue = if (volteada) 180f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "giro"
    )
    val forma = RoundedCornerShape(esquinas)
    val mostrandoReverso = giro > 90f
    Box(
        modifier
            .graphicsLayer {
                rotationY = giro
                cameraDistance = 14f * density
            }
            .shadow(8.dp, forma, ambientColor = Color(0x22B08A70), spotColor = Color(0x22B08A70))
            .clip(forma)
            .background(if (mostrandoReverso) colorReverso else colorAnverso)
            .clickable(role = Role.Button, onClickLabel = if (volteada) "Ver el anverso" else "Voltear la tarjeta", onClick = onVoltear)
            .semantics { stateDescription = if (volteada) "Mostrando el reverso" else "Mostrando el anverso" }
    ) {
        // El reverso se dibuja girado otros 180° para que no se lea al revés
        Box(
            Modifier.fillMaxSize().graphicsLayer { if (mostrandoReverso) rotationY = 180f },
            content = if (mostrandoReverso) reverso else anverso
        )
    }
}
