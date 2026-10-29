package com.udenar.flashly

import android.graphics.Color as ColorAndroid
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.ui.components.AvatarCirculo
import com.udenar.flashly.ui.components.BarraProgreso
import com.udenar.flashly.ui.components.BotonFlashly
import com.udenar.flashly.ui.components.CampoTexto
import com.udenar.flashly.ui.components.DialogoConfirmacion
import com.udenar.flashly.ui.components.EstadoCarga
import com.udenar.flashly.ui.components.EstadoError
import com.udenar.flashly.ui.components.EstadoVacio
import com.udenar.flashly.ui.components.EstiloBoton
import com.udenar.flashly.ui.components.EtiquetaVisibilidad
import com.udenar.flashly.ui.components.FiltroChip
import com.udenar.flashly.ui.components.MensajeAviso
import com.udenar.flashly.ui.components.ProgresoConTexto
import com.udenar.flashly.ui.components.TarjetaFlashly
import com.udenar.flashly.ui.components.TarjetaMazo
import com.udenar.flashly.ui.components.TipoMensaje
import com.udenar.flashly.ui.components.TituloSeccion
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.FlashlyTheme
import com.udenar.flashly.ui.theme.Paleta

/** Catálogo visual del paso 1: muestra la paleta, la tipografía y todos los componentes base. */
class CatalogoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(ColorAndroid.TRANSPARENT, ColorAndroid.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(ColorAndroid.TRANSPARENT, ColorAndroid.TRANSPARENT),
        )
        setContent { FlashlyTheme { Catalogo() } }
    }
}

private val avatares = listOf(
    "avatar_1" to Paleta.RosaPastel, "avatar_2" to Paleta.Menta, "avatar_3" to Paleta.Lavanda,
    "avatar_4" to Paleta.Melocoton, "avatar_5" to Paleta.Mantequilla, "avatar_6" to Paleta.Periwinkle,
    "avatar_7" to Paleta.RosaSuave,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Catalogo() {
    var correo by remember { mutableStateOf("laura@") }
    var usuario by remember { mutableStateOf("laura_g") }
    var clave by remember { mutableStateOf("Flashly#2026") }
    var filtro by remember { mutableIntStateOf(0) }
    var avatarElegido by remember { mutableIntStateOf(2) }
    var dialogo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(painterResource(R.drawable.logo_flashly), contentDescription = "Flashly", modifier = Modifier.fillMaxWidth().height(150.dp))
        Text("Aprende a tu ritmo, recuerda para siempre", style = MaterialTheme.typography.displaySmall)
        Text("Montserrat para el contenido: legible y amigable, con jerarquía clara.", style = MaterialTheme.typography.bodyLarge, color = Paleta.TextoSecundario)

        TituloSeccion("Paleta")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Paleta.RosaPastel, Paleta.RosaSuave, Paleta.Crema, Paleta.Menta, Paleta.VerdeAzulado, Paleta.VerdeAzuladoOscuro,
                Paleta.Lavanda, Paleta.Periwinkle, Paleta.Melocoton, Paleta.Mantequilla, Paleta.Texto, Paleta.TextoSecundario
            ).forEach { Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(it)) }
        }

        TituloSeccion("Avatares (Supabase Storage)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            avatares.forEachIndexed { i, (id, color) ->
                AvatarCirculo(
                    url = SupabaseProvider.urlAvatar("$id.png"),
                    colorFondo = color, tamano = 64.dp, iniciales = "LG",
                    seleccionado = i == avatarElegido,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonFlashly("Avatar anterior", { avatarElegido = (avatarElegido + 6) % 7 }, estilo = EstiloBoton.Secundario)
            BotonFlashly("Siguiente", { avatarElegido = (avatarElegido + 1) % 7 }, estilo = EstiloBoton.Suave)
        }

        TituloSeccion("Botones")
        BotonFlashly("Comenzar", {}, Modifier.fillMaxWidth())
        BotonFlashly("Ya tengo una cuenta", {}, Modifier.fillMaxWidth(), estilo = EstiloBoton.Secundario)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonFlashly("Crear mazo", {}, estilo = EstiloBoton.Suave, icono = Icons.Rounded.Add)
            BotonFlashly("Guardando", {}, cargando = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonFlashly("Eliminar", { dialogo = true }, estilo = EstiloBoton.Peligro)
            BotonFlashly("Deshabilitado", {}, habilitado = false)
        }
        BotonFlashly("¿Olvidaste tu contraseña?", {}, estilo = EstiloBoton.Texto)

        TituloSeccion("Campos de texto")
        CampoTexto(usuario, { usuario = it }, "Nombre de usuario", icono = Icons.Rounded.AlternateEmail, valido = true, ayuda = "Así te verán los demás")
        CampoTexto(correo, { correo = it }, "Correo electrónico", icono = Icons.Rounded.Mail, error = "Escribe un correo válido")
        CampoTexto(clave, { clave = it }, "Contraseña", icono = Icons.Rounded.Lock, esContrasena = true)
        CampoTexto("", {}, "Teléfono", icono = Icons.Rounded.Phone, marcador = "300 123 4567")
        CampoTexto("Laura Gómez", {}, "Nombre completo", icono = Icons.Rounded.Person, habilitado = false)

        TituloSeccion("Filtros")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Propios", "Guardados").forEachIndexed { i, t -> FiltroChip(t, filtro == i, { filtro = i }) }
        }

        TituloSeccion("Tarjetas de mazo", textoAccion = "Ver todo")
        TarjetaMazo("Capitales de Sudamérica", 12, ColorFlashly.ROSA, {}, descripcion = "Las 12 capitales con preguntas y definiciones.", creador = "mateo_r", esPublico = true)
        TarjetaMazo("Biología celular", 24, ColorFlashly.LAVANDA, {}, descripcion = "Organelos y sus funciones.", esPublico = false)
        TarjetaMazo("Inglés: verbos irregulares", 40, ColorFlashly.MENTA, {}, creador = "sofia_p", esPublico = true)
        TarjetaMazo("Historia de Colombia", 8, ColorFlashly.MELOCOTON, {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetaVisibilidad(true); EtiquetaVisibilidad(false)
        }

        TituloSeccion("Progreso")
        ProgresoConTexto(4, 20)
        BarraProgreso(0.7f, color = Paleta.Melocoton, fondo = Paleta.Melocoton.copy(alpha = 0.25f))
        BarraProgreso(0.35f, color = Paleta.Periwinkle, fondo = Paleta.Lavanda.copy(alpha = 0.5f))

        TituloSeccion("Mensajes")
        MensajeAviso("Cambios guardados correctamente", TipoMensaje.Exito)
        MensajeAviso("No hay conexión con el servidor. Revisa tu Internet", TipoMensaje.Error)
        MensajeAviso("Voltear una tarjeta no cuenta como error", TipoMensaje.Info)

        TituloSeccion("Estados")
        TarjetaFlashly(Modifier.fillMaxWidth()) { EstadoCarga(Modifier.align(Alignment.CenterHorizontally), "Cargando tus mazos…") }
        TarjetaFlashly(Modifier.fillMaxWidth()) {
            EstadoVacio("Aún no tienes mazos", "Crea tu primer mazo y empieza a estudiar a tu ritmo.", textoAccion = "Crear mi primer mazo")
        }
        TarjetaFlashly(Modifier.fillMaxWidth()) { EstadoError("No hay conexión con el servidor. Revisa tu Internet", {}) }
        TarjetaFlashly(Modifier.fillMaxWidth(), color = Paleta.Crema) {
            Text("Tarjeta base", style = MaterialTheme.typography.titleLarge)
            Text("Superficie con esquinas de 24 dp y sombra suave.", style = MaterialTheme.typography.bodyMedium, color = Paleta.TextoSecundario)
            BotonFlashly("Estudiar", {}, Modifier.padding(top = 12.dp), icono = Icons.Rounded.PlayArrow)
        }
        Box(Modifier.height(24.dp).background(Color.Transparent))
    }

    if (dialogo) {
        DialogoConfirmacion(
            titulo = "¿Eliminar este mazo?",
            mensaje = "Se borrarán sus tarjetas. Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar", peligroso = true,
            onConfirmar = { dialogo = false }, onCancelar = { dialogo = false }
        )
    }
}
