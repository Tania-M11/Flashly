package com.udenar.flashly.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

// ---------- Modelos (esquema de mazos v2) ----------

@Serializable
data class Categoria(
    val id: String,
    val nombre: String,
    val icono: String,
    val color: String,
    val orden: Int,
)

/** Avatar embebido en los datos públicos del creador (solo archivo y color). */
@Serializable
data class AvatarPublico(val archivo: String, val color: String)

/** Lo que cualquier usuario puede ver de otro: nombre de usuario y avatar (tabla perfiles_publicos). */
@Serializable
data class CreadorPublico(
    @SerialName("nombre_usuario") val nombreUsuario: String,
    @SerialName("avatares") val avatar: AvatarPublico? = null,
)

@Serializable
data class Conteo(val count: Int)

/** Mazo como se muestra en listas: con su creador y el número de tarjetas. */
@Serializable
data class MazoResumen(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val color: String = "ROSA",
    @SerialName("categoria_id") val categoriaId: String = "otros",
    @SerialName("es_publico") val esPublico: Boolean,
    @SerialName("creador_id") val creadorId: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    val creador: CreadorPublico? = null,
    val tarjetas: List<Conteo> = emptyList(),
) {
    val numTarjetas get() = tarjetas.firstOrNull()?.count ?: 0
}

/** Tarjeta tal como la ve la app: nunca incluye la respuesta correcta ni la explicación. */
@Serializable
data class Tarjeta(
    val id: String,
    @SerialName("mazo_id") val mazoId: String,
    val tipo: String,
    val posicion: Int,
    val enunciado: String? = null,
    val opciones: List<String>? = null,
    val termino: String? = null,
    val definicion: String? = null,
) {
    val esPregunta get() = tipo == TIPO_PREGUNTA

    companion object {
        const val TIPO_PREGUNTA = "opcion_multiple"
        const val TIPO_DEFINICION = "definicion"
    }
}

/** Respuesta correcta y explicación de una pregunta propia (solo para el editor). */
@Serializable
data class Clave(
    @SerialName("tarjeta_id") val tarjetaId: String,
    @SerialName("indice_correcto") val indiceCorrecto: Int,
    val explicacion: String? = null,
)

@Serializable
private data class FilaGuardado(
    @SerialName("created_at") val createdAt: String,
    val mazo: MazoResumen? = null,
)

/** Mazos, tarjetas, categorías y biblioteca personal (esquema v2 de la base de datos). */
object MazosRepository {
    private val client get() = SupabaseProvider.client

    // Creador con sus datos públicos y el número de tarjetas, en una sola consulta
    private val columnasResumen = Columns.raw(
        "id, nombre, descripcion, color, categoria_id, es_publico, creador_id, updated_at, " +
            "creador:perfiles_publicos!mazos_creador_publico_fkey(nombre_usuario, avatares(archivo, color)), " +
            "tarjetas(count)"
    )

    private var categoriasCache: List<Categoria>? = null

    fun usuarioId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun categorias(): List<Categoria> =
        categoriasCache ?: client.from("categorias").select { order("orden", Order.ASCENDING) }
            .decodeList<Categoria>().also { categoriasCache = it }

    suspend fun misMazos(): List<MazoResumen> {
        val uid = usuarioId() ?: return emptyList()
        return client.from("mazos").select(columnasResumen) {
            filter { eq("creador_id", uid) }
            order("updated_at", Order.DESCENDING)
        }.decodeList<MazoResumen>()
    }

    /** Mazos guardados (si el creador lo volvió privado, RLS lo oculta y se descarta). */
    suspend fun guardados(): List<MazoResumen> =
        client.from("mazos_guardados").select(Columns.raw("created_at, mazo:mazos(${columnasResumen.value})")) {
            order("created_at", Order.DESCENDING)
        }.decodeList<FilaGuardado>().mapNotNull { it.mazo }

    suspend fun idsGuardados(): Set<String> =
        client.from("mazos_guardados").select(Columns.list("mazo_id")).decodeList<Map<String, String>>()
            .mapNotNull { it["mazo_id"] }.toSet()

    /**
     * Mazos públicos de otros usuarios. El texto busca por coincidencia parcial en el nombre o por
     * palabras (búsqueda de texto completo en español) en el nombre y la descripción.
     */
    suspend fun explorar(texto: String, categoriaId: String?): List<MazoResumen> {
        val uid = usuarioId()
        val consulta = texto.trim()
        return client.from("mazos").select(columnasResumen) {
            filter {
                eq("es_publico", true)
                if (uid != null) neq("creador_id", uid)
                if (categoriaId != null) eq("categoria_id", categoriaId)
                if (consulta.isNotEmpty()) or {
                    ilike("nombre", "%${consulta.replace(",", " ")}%")
                    textSearch("busqueda", consulta.replace(",", " "), TextSearchType.WEBSEARCH, "spanish")
                }
            }
            order("updated_at", Order.DESCENDING)
            limit(50)
        }.decodeList<MazoResumen>()
    }

    suspend fun mazo(id: String): MazoResumen =
        client.from("mazos").select(columnasResumen) { filter { eq("id", id) } }.decodeSingle<MazoResumen>()

    suspend fun tarjetas(mazoId: String): List<Tarjeta> =
        client.from("tarjetas").select {
            filter { eq("mazo_id", mazoId) }
            order("posicion", Order.ASCENDING)
        }.decodeList<Tarjeta>()

    suspend fun guardar(mazoId: String) {
        client.from("mazos_guardados").insert(mapOf("mazo_id" to mazoId))
    }

    suspend fun quitarGuardado(mazoId: String) {
        val uid = usuarioId() ?: return
        client.from("mazos_guardados").delete { filter { eq("mazo_id", mazoId); eq("usuario_id", uid) } }
    }

    suspend fun cambiarVisibilidad(mazoId: String, esPublico: Boolean) {
        client.from("mazos").update({ set("es_publico", esPublico) }) { filter { eq("id", mazoId) } }
    }

    /** Claves de las preguntas de un mazo propio (la función SQL no responde nada si el mazo es ajeno). */
    suspend fun clavesDeMiMazo(mazoId: String): List<Clave> =
        client.postgrest.rpc("claves_de_mi_mazo", buildJsonObject { put("p_mazo_id", mazoId) }).decodeList<Clave>()

    /**
     * Crea ([mazoId] = null) o edita un mazo con todas sus tarjetas en una sola transacción
     * (función SQL guardar_mazo). Devuelve el id del mazo.
     */
    suspend fun guardarMazo(mazoId: String?, datos: DatosMazo, tarjetas: List<TarjetaEditable>): String =
        client.postgrest.rpc(
            "guardar_mazo",
            buildJsonObject {
                put("p_mazo_id", mazoId)
                putJsonObject("p_mazo") {
                    put("nombre", datos.nombre.trim())
                    put("descripcion", datos.descripcion.trim())
                    put("categoria_id", datos.categoriaId)
                    put("color", datos.color)
                    put("es_publico", datos.esPublico)
                }
                putJsonArray("p_tarjetas") {
                    tarjetas.forEach { t ->
                        addJsonObject {
                            if (t.id != null) put("id", t.id)
                            put("tipo", t.tipo)
                            if (t.esPregunta) {
                                put("enunciado", t.enunciado.trim())
                                putJsonArray("opciones") { t.opciones.forEach { add(it.trim()) } }
                                put("correcta", t.correcta)
                                put("explicacion", t.explicacion.trim())
                            } else {
                                put("termino", t.termino.trim())
                                put("definicion", t.definicion.trim())
                            }
                        }
                    }
                }
            }
        ).decodeAs<String>()

    /** Borra el mazo; la base de datos elimina en cascada sus tarjetas, claves y guardados. */
    suspend fun eliminar(mazoId: String) {
        client.from("mazos").delete { filter { eq("id", mazoId) } }
    }
}
