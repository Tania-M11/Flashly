package com.udenar.flashly.data

import com.udenar.flashly.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Cliente único de Supabase para toda la app.
 * La URL y la clave publicable vienen de local.properties (vía BuildConfig).
 */
object SupabaseProvider {
    val client: SupabaseClient by lazy {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "Faltan SUPABASE_URL o SUPABASE_PUBLISHABLE_KEY en local.properties"
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    /** URL pública de un avatar del bucket "avatares" (bucket público, solo lectura). */
    fun urlAvatar(archivo: String): String = client.storage.from("avatares").publicUrl(archivo)

    fun urlAvatar(avatar: Avatar): String = urlAvatar(avatar.archivo)
}