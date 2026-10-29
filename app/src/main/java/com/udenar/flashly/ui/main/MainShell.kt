package com.udenar.flashly.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.udenar.flashly.ui.components.BarraInferior
import com.udenar.flashly.ui.components.ItemBarra
import androidx.navigation.toRoute
import com.udenar.flashly.ui.editor.EditorMazoScreen
import com.udenar.flashly.ui.estudio.DetalleSesionScreen
import com.udenar.flashly.ui.estudio.ModosScreen
import com.udenar.flashly.ui.estudio.SesionScreen
import com.udenar.flashly.ui.historial.HistorialScreen
import com.udenar.flashly.ui.mazos.BibliotecaScreen
import com.udenar.flashly.ui.mazos.DetalleMazoScreen
import com.udenar.flashly.ui.mazos.ExplorarScreen
import com.udenar.flashly.ui.perfil.CambiarContrasenaScreen
import com.udenar.flashly.ui.perfil.ConfiguracionScreen
import com.udenar.flashly.ui.perfil.EditarPerfilScreen
import com.udenar.flashly.ui.perfil.PerfilScreen
import kotlinx.serialization.Serializable

// Secciones de la barra inferior
@Serializable data object RutaInicio
@Serializable data object RutaMazos
@Serializable data object RutaHistorial
@Serializable data object RutaPerfil

// Pantallas secundarias (sin barra inferior)
@Serializable data object RutaEditarPerfil
@Serializable data object RutaConfiguracion
@Serializable data object RutaCambiarContrasena
@Serializable data object RutaExplorar          // parte de la sección Mazos: conserva la barra
@Serializable data class RutaDetalleMazo(val mazoId: String)
@Serializable data class RutaEditorMazo(val mazoId: String? = null)   // null = crear un mazo nuevo
@Serializable data class RutaModos(val mazoId: String)                // pantalla 10: elegir modo
@Serializable data class RutaSesion(val mazoId: String, val modo: String)   // pantallas 11 y 12
@Serializable data class RutaDetalleSesion(val sesionId: String)      // detalle desde el historial

private val secciones = listOf<Any>(RutaInicio, RutaMazos, RutaHistorial, RutaPerfil)
private val itemsBarra = listOf(
    ItemBarra("Inicio", Icons.Outlined.Home, Icons.Rounded.Home),
    ItemBarra("Mazos", Icons.Outlined.Style, Icons.Rounded.Style),
    ItemBarra("Historial", Icons.Outlined.InsertChart, Icons.Rounded.InsertChart),
    ItemBarra("Perfil", Icons.Outlined.Person, Icons.Rounded.Person),
)

/**
 * Cambia de sección conservando el estado de cada una (como las apps de Android con barra inferior).
 * Inicio nunca restaura una pila guardada: lo que se abre desde ahí (detalle, modos, sesión)
 * es un recorrido que se cierra, y restaurarlo devolvía al usuario al resumen de la sesión.
 */
private fun NavHostController.irASeccion(ruta: Any) = navigate(ruta) {
    popUpTo(graph.findStartDestination().id) { saveState = ruta != RutaInicio }
    launchSingleTop = true
    restoreState = ruta != RutaInicio
}

/** Sale de un recorrido (por ejemplo, el resumen de una sesión) hacia una sección, sin guardar las pantallas que cierra. */
private fun NavHostController.terminarEn(ruta: Any) = navigate(ruta) {
    popUpTo(graph.findStartDestination().id)
    launchSingleTop = true
}

/** Estructura principal: barra inferior persistente con Inicio, Mazos, Historial y Perfil. */
@Composable
fun MainShell() {
    val nav = rememberNavController()
    val crear = { nav.navigate(RutaEditorMazo()) }
    val editar = { id: String -> nav.navigate(RutaEditorMazo(id)) }
    val estudiar = { id: String -> nav.navigate(RutaModos(id)) }
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val indice = when {
        destino?.hasRoute(RutaInicio::class) == true -> 0
        destino?.hasRoute(RutaMazos::class) == true || destino?.hasRoute(RutaExplorar::class) == true -> 1
        destino?.hasRoute(RutaHistorial::class) == true -> 2
        destino?.hasRoute(RutaPerfil::class) == true -> 3
        else -> -1
    }

    Scaffold(
        // Las pantallas manejan la barra de estado; aquí solo se reserva el alto de la barra inferior
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = indice >= 0 || destino == null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                BarraInferior(itemsBarra, indice.coerceAtLeast(0), { nav.irASeccion(secciones[it]) })
            }
        }
    ) { relleno ->
        NavHost(
            navController = nav,
            startDestination = RutaInicio,
            modifier = Modifier.padding(relleno).consumeWindowInsets(relleno),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
        ) {
            composable<RutaInicio> {
                InicioScreen(
                    onPerfil = { nav.irASeccion(RutaPerfil) },
                    onCrearMazo = crear,
                    onVerMazos = { nav.irASeccion(RutaMazos) },
                    onExplorar = { nav.irASeccion(RutaMazos); nav.navigate(RutaExplorar) },
                    onHistorial = { nav.irASeccion(RutaHistorial) },
                    onAbrirMazo = { nav.navigate(RutaDetalleMazo(it)) },
                    onEstudiar = estudiar,
                )
            }
            composable<RutaMazos> {
                BibliotecaScreen(
                    onAbrir = { nav.navigate(RutaDetalleMazo(it)) },
                    onExplorar = { nav.navigate(RutaExplorar) },
                    onCrear = crear,
                    onEditar = editar,
                )
            }
            composable<RutaExplorar> {
                ExplorarScreen(onVolver = { nav.popBackStack() }, onAbrir = { nav.navigate(RutaDetalleMazo(it)) })
            }
            composable<RutaDetalleMazo>(
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
            ) { entrada ->
                DetalleMazoScreen(
                    mazoId = entrada.toRoute<RutaDetalleMazo>().mazoId,
                    onVolver = { nav.popBackStack() },
                    onEstudiar = estudiar,
                    onEditar = editar,
                )
            }
            composable<RutaEditorMazo>(
                enterTransition = { slideIntoContainer(SlideDirection.Up, tween(320)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.Down, tween(320)) },
            ) { entrada ->
                val esNuevo = entrada.toRoute<RutaEditorMazo>().mazoId == null
                EditorMazoScreen(
                    onVolver = { nav.popBackStack() },
                    onGuardado = { id ->
                        // Al crear se abre el detalle del mazo nuevo; al editar se vuelve a la pantalla anterior
                        if (esNuevo) nav.navigate(RutaDetalleMazo(id)) { popUpTo<RutaEditorMazo> { inclusive = true } }
                        else nav.popBackStack()
                    }
                )
            }
            composable<RutaModos>(
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
            ) { entrada ->
                val mazoId = entrada.toRoute<RutaModos>().mazoId
                ModosScreen(
                    mazoId = mazoId,
                    onVolver = { nav.popBackStack() },
                    onElegir = { modo -> nav.navigate(RutaSesion(mazoId, modo.clave)) },
                )
            }
            composable<RutaSesion>(
                enterTransition = { slideIntoContainer(SlideDirection.Up, tween(320)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.Down, tween(320)) },
            ) {
                SesionScreen(
                    onSalir = { nav.popBackStack() },
                    onRepetir = { nav.popBackStack() },          // vuelve a la selección de modo
                    onInicio = { nav.terminarEn(RutaInicio) },
                    onHistorial = { nav.terminarEn(RutaHistorial) },
                )
            }
            composable<RutaHistorial> {
                HistorialScreen(
                    onAbrirSesion = { nav.navigate(RutaDetalleSesion(it)) },
                    onIrAMazos = { nav.irASeccion(RutaMazos) },
                )
            }
            composable<RutaDetalleSesion>(
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
            ) { entrada ->
                DetalleSesionScreen(
                    sesionId = entrada.toRoute<RutaDetalleSesion>().sesionId,
                    onVolver = { nav.popBackStack() },
                    onEstudiar = estudiar,
                )
            }
            composable<RutaPerfil> {
                PerfilScreen(
                    onEditar = { nav.navigate(RutaEditarPerfil) },
                    onConfiguracion = { nav.navigate(RutaConfiguracion) }
                )
            }

            composable<RutaEditarPerfil>(
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
            ) {
                EditarPerfilScreen(onVolver = { nav.popBackStack() }, onCambiarContrasena = { nav.navigate(RutaCambiarContrasena) })
            }
            composable<RutaConfiguracion>(
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
            ) {
                ConfiguracionScreen(onVolver = { nav.popBackStack() }, onCambiarContrasena = { nav.navigate(RutaCambiarContrasena) })
            }
            composable<RutaCambiarContrasena>(
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
            ) {
                CambiarContrasenaScreen(onVolver = { nav.popBackStack() })
            }
        }
    }
}
