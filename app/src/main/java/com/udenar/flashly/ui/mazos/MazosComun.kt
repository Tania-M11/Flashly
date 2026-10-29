package com.udenar.flashly.ui.mazos

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.udenar.flashly.data.MazoResumen
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.ui.components.TarjetaMazo
import com.udenar.flashly.ui.haceCuanto
import com.udenar.flashly.ui.theme.ColorFlashly
import com.udenar.flashly.ui.theme.colorDeHex
import java.text.Normalizer

/** Tarjeta de un mazo de la base de datos: color, creador con su avatar, número de tarjetas y fecha. */
@Composable
fun TarjetaDeMazo(
    m: MazoResumen,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarCreador: Boolean = true,
    mostrarVisibilidad: Boolean = false,
    accesorio: (@Composable () -> Unit)? = null,
) {
    val avatar = m.creador?.avatar
    TarjetaMazo(
        titulo = m.nombre,
        numTarjetas = m.numTarjetas,
        color = ColorFlashly.porNombre(m.color),
        onClick = onClick,
        modifier = modifier,
        descripcion = m.descripcion,
        creador = if (mostrarCreador) m.creador?.nombreUsuario else null,
        esPublico = if (mostrarVisibilidad) m.esPublico else null,
        avatarCreadorUrl = avatar?.let { SupabaseProvider.urlAvatar(it.archivo) },
        colorAvatarCreador = avatar?.let { colorDeHex(it.color) },
        fecha = haceCuanto(m.updatedAt),
        accesorio = accesorio,
    )
}

/** Comparar sin mayúsculas ni tildes: "biologia" encuentra "Biología". */
fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

/** Abre el menú de compartir de Android con una invitación al mazo público. */
fun compartirMazo(context: Context, m: MazoResumen) {
    val creador = m.creador?.nombreUsuario?.let { " de @$it" }.orEmpty()
    val texto = "Te invito a estudiar el mazo «${m.nombre}»$creador en Flashly (${m.numTarjetas} tarjetas). " +
        "Búscalo en la sección Explorar."
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir mazo"))
}

/** Aviso breve para las acciones que se completan en los siguientes pasos del plan. */
fun avisoProximo(context: Context, texto: String) {
    Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
}
