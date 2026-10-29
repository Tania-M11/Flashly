package com.udenar.flashly.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.udenar.flashly.R
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.ui.auth.BienvenidaScreen
import com.udenar.flashly.ui.auth.CuentaCreadaScreen
import com.udenar.flashly.ui.auth.LoginScreen
import com.udenar.flashly.ui.auth.RegistroReciente
import com.udenar.flashly.ui.auth.RegistroScreen
import com.udenar.flashly.ui.main.MainShell
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.serialization.Serializable

/** Según el estado de la sesión de Supabase muestra el arranque, las pantallas de cuenta o la app. */
@Composable
fun FlashlyApp() {
    val estado by SupabaseProvider.client.auth.sessionStatus.collectAsState()
    when (estado) {
        is SessionStatus.Initializing -> PantallaArranque()
        is SessionStatus.Authenticated ->
            if (RegistroReciente.activo) CuentaCreadaScreen(onEmpezar = { RegistroReciente.activo = false })
            else MainShell()
        else -> AuthNav()
    }
}

/** Mientras Supabase recupera la sesión guardada se ve el logo sobre el fondo crema. */
@Composable
private fun PantallaArranque() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.logo_flashly), contentDescription = "Flashly", modifier = Modifier.fillMaxWidth(0.6f))
    }
}

// Rutas de las pantallas de cuenta (navegación con tipos de Navigation Compose)
@Serializable private data object RutaBienvenida
@Serializable private data object RutaRegistro
@Serializable private data object RutaLogin

@Composable
private fun AuthNav() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = RutaBienvenida,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(320)) + fadeIn(tween(320)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(320)) + fadeOut(tween(320)) },
    ) {
        composable<RutaBienvenida> {
            BienvenidaScreen(
                onComenzar = { nav.navigate(RutaRegistro) },
                onYaTengoCuenta = { nav.navigate(RutaLogin) }
            )
        }
        composable<RutaRegistro> {
            RegistroScreen(
                onVolver = { nav.popBackStack() },
                onIrALogin = { nav.navigate(RutaLogin) { popUpTo(RutaBienvenida) } }
            )
        }
        composable<RutaLogin> {
            LoginScreen(
                onVolver = { nav.popBackStack() },
                onCrearCuenta = { nav.navigate(RutaRegistro) { popUpTo(RutaBienvenida) } }
            )
        }
    }
}
