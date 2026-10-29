package com.udenar.flashly.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Cuenta del usuario: Supabase Auth (correo y contraseña) y su perfil (tabla perfiles).
 * El perfil lo crea un trigger en la base de datos con los datos enviados al registrarse.
 */
object CuentaRepository {
    private val client get() = SupabaseProvider.client

    // Perfil del usuario con sesión, compartido por Inicio, Perfil y Editar perfil
    private val _perfil = MutableStateFlow<Perfil?>(null)
    val perfil: StateFlow<Perfil?> = _perfil.asStateFlow()

    /** Devuelve true si la sesión quedó iniciada (false si Supabase exige confirmar el correo). */
    suspend fun registrar(nombreCompleto: String, nombreUsuario: String, telefono: String, correo: String, clave: String): Boolean {
        client.auth.signUpWith(Email) {
            email = correo.trim()
            password = clave
            // Metadatos del usuario: el trigger crear_perfil_nuevo_usuario los copia a perfiles
            data = buildJsonObject {
                put("nombre_completo", nombreCompleto.trim())
                put("nombre_usuario", normalizarUsuario(nombreUsuario))
                put("telefono", telefono.trim())
            }
        }
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun iniciarSesion(correo: String, clave: String) {
        client.auth.signInWith(Email) {
            email = correo.trim()
            password = clave
        }
    }

    suspend fun cerrarSesion() {
        client.auth.signOut()
        _perfil.value = null
    }

    /** Consulta pública (sin sesión) para validar el nombre de usuario mientras se escribe. */
    suspend fun nombreUsuarioDisponible(nombre: String): Boolean =
        client.postgrest.rpc("nombre_usuario_disponible", buildJsonObject { put("p_nombre", normalizarUsuario(nombre)) })
            .decodeAs<Boolean>()

    /** Perfil del usuario con sesión, incluido su avatar. También actualiza [perfil]. */
    suspend fun miPerfil(): Perfil {
        val uid = client.auth.currentUserOrNull()?.id ?: error("No hay sesión iniciada")
        return client.from("perfiles").select(Columns.raw("*, avatares(*)")) {
            filter { eq("id", uid) }
        }.decodeSingle<Perfil>().also { _perfil.value = it }
    }

    /** Catálogo de avatares predefinidos (tabla avatares, imágenes en Storage). */
    suspend fun avatares(): List<Avatar> =
        client.from("avatares").select { order("orden", Order.ASCENDING) }.decodeList<Avatar>()

    /**
     * Guarda los datos personales y el avatar. RLS solo deja editar el perfil propio y el permiso
     * por columnas impide tocar el XP.
     */
    suspend fun actualizarPerfil(nombreCompleto: String, nombreUsuario: String, telefono: String, avatarId: String): Perfil {
        val uid = client.auth.currentUserOrNull()?.id ?: error("No hay sesión iniciada")
        client.from("perfiles").update({
            set("nombre_completo", nombreCompleto.trim())
            set("nombre_usuario", normalizarUsuario(nombreUsuario))
            set("telefono", telefono.trim())
            set("avatar_id", avatarId)
        }) {
            filter { eq("id", uid) }
        }
        return miPerfil()
    }

    /**
     * Cambia la contraseña comprobando antes la actual (se vuelve a iniciar sesión con ella),
     * para que nadie pueda cambiarla con un teléfono desbloqueado ajeno.
     */
    suspend fun cambiarContrasena(actual: String, nueva: String) {
        val correo = correoActual() ?: error("No hay sesión iniciada")
        try {
            iniciarSesion(correo, actual)
        } catch (e: Exception) {
            throw IllegalArgumentException("La contraseña actual no es correcta")
        }
        client.auth.updateUser { password = nueva }
    }

    fun correoActual(): String? = client.auth.currentUserOrNull()?.email
}
