package com.udenar.flashly.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.udenar.flashly.R
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.theme.Paleta

/** Pantalla 1: bienvenida. «Comenzar» lleva al registro y «Ya tengo una cuenta» al inicio de sesión. */
@Composable
fun BienvenidaScreen(onComenzar: () -> Unit, onYaTengoCuenta: () -> Unit) {
    // El logo entra con un pequeño rebote
    val escala = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { escala.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f)) }

    Box(Modifier.fillMaxSize()) {
        FondoDecorado()
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.6f))
            AvataresFlotantes()
            Image(
                painterResource(R.drawable.logo_flashly),
                contentDescription = "Flashly",
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(top = 8.dp)
                    .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Aprende a tu ritmo,\nrecuerda para siempre",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Crea mazos de tarjetas, estudia con cuatro modos de juego y mira cómo crece tu progreso.",
                style = MaterialTheme.typography.bodyLarge,
                color = Paleta.TextoSecundario,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonFlashly("Comenzar", onComenzar, Modifier.fillMaxWidth())
                BotonFlashly("Ya tengo una cuenta", onYaTengoCuenta, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
            }
        }
    }
}
