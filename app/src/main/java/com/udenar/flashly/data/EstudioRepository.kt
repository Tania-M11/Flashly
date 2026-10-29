package com.udenar.flashly.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/** Los 4 modos de juego. [clave] es el valor que guarda la base de datos. */
enum class ModoJuego(val clave: String, val titulo: String) {
    PRACTICA("practica", "Práctica"),
    CONTRARRELOJ("contrarreloj", "Contrarreloj"),
    REPASO("repaso", "Repaso inteligente"),
    RETO("reto", "Reto diario");

    companion object {
        fun de(clave: String?): ModoJuego = entries.firstOrNull { it.clave == clave } ?: PRACTICA
    }
}

/** Tarjeta tal como llega al iniciar una sesión (sin respuesta). [caja] = nivel de repetición espaciada (0-5). */
@Serializable
data class TarjetaSesion(
    val id: String,
    val tipo: String,
    val enunciado: String? = null,
    val opciones: List<String>? = null,
    val termino: String? = null,
    val definicion: String? = null,
    val caja: Int? = null,
) {
    val esPregunta get() = tipo == Tarjeta.TIPO_PREGUNTA
}

@Serializable
data class SesionNueva(
    @SerialName("sesion_id") val sesionId: String,
    val modo: String,
    @SerialName("mazo_nombre") val mazoNombre: String,
    @SerialName("mazo_color") val mazoColor: String = "ROSA",
    val vidas: Int? = null,
    @SerialName("limite_segundos") val limiteSegundos: Int? = null,
    @SerialName("xp_doble_disponible") val xpDobleDisponible: Boolean = false,
    val tarjetas: List<TarjetaSesion> = emptyList(),
)

/**
 * Resumen de una sesión (pantalla 12, historial y detalle). Sale de la tabla sesiones
 * o de las funciones que la cierran; [refuerzo] solo viene en el resumen detallado.
 */
@Serializable
data class ResumenSesion(
    val id: String,
    @SerialName("mazo_id") val mazoId: String? = null,
    @SerialName("mazo_nombre") val mazoNombre: String,
    @SerialName("mazo_color") val mazoColor: String = "ROSA",
    val modo: String,
    val estado: String,
    val vidas: Int? = null,
    @SerialName("limite_segundos") val limiteSegundos: Int? = null,
    val aciertos: Int = 0,
    val errores: Int = 0,
    @SerialName("lo_se") val loSe: Int = 0,
    val practicar: Int = 0,
    @SerialName("mejor_combo") val mejorCombo: Int = 0,
    val xp: Int = 0,
    @SerialName("xp_bono") val xpBono: Int = 0,
    @SerialName("xp_total") val xpTotal: Int = 0,
    @SerialName("xp_doble") val xpDoble: Boolean = false,
    @SerialName("iniciada_en") val iniciadaEn: String,
    @SerialName("terminada_en") val terminadaEn: String? = null,
    @SerialName("total_tarjetas") val totalTarjetas: Int = 0,
    val refuerzo: List<String> = emptyList(),
) {
    val modoJuego get() = ModoJuego.de(modo)
    val preguntas get() = aciertos + errores
    val definiciones get() = loSe + practicar
    val revisadas get() = preguntas + definiciones

    /** Puntuación del cuestionario (0-100); null si no hubo preguntas evaluables. */
    val puntuacion: Int? get() = if (preguntas > 0) aciertos * 100 / preguntas else null
}

/** Lo que devuelve el servidor tras responder una tarjeta. Si [resumen] no es null, la sesión terminó. */
@Serializable
data class ResultadoRespuesta(
    val resultado: String? = null,
    val correcta: Boolean? = null,
    @SerialName("indice_correcto") val indiceCorrecto: Int? = null,
    val explicacion: String? = null,
    val xp: Int = 0,
    @SerialName("xp_sesion") val xpSesion: Int = 0,
    val combo: Int = 0,
    val vidas: Int? = null,
    val caja: Int? = null,
    val resumen: ResumenSesion? = null,
    @SerialName("tiempo_agotado") val tiempoAgotado: Boolean = false,
)

@Serializable
data class InfoModos(
    val total: Int = 0,
    val preguntas: Int = 0,
    val definiciones: Int = 0,
    @SerialName("pendientes_repaso") val pendientesRepaso: Int = 0,
    val dominadas: Int = 0,
    @SerialName("xp_doble_disponible") val xpDobleDisponible: Boolean = false,
)

@Serializable
data class ContinuarEstudio(
    @SerialName("mazo_id") val mazoId: String,
    @SerialName("mazo_nombre") val mazoNombre: String,
    @SerialName("mazo_color") val mazoColor: String = "ROSA",
    val modo: String,
    @SerialName("terminada_en") val terminadaEn: String? = null,
)

@Serializable
data class ResumenInicio(
    val xp: Int = 0,
    @SerialName("xp_semana") val xpSemana: Int = 0,
    val racha: Int = 0,
    @SerialName("estudio_hoy") val estudioHoy: Boolean = false,
    val sesiones: Int = 0,
    @SerialName("tarjetas_estudiadas") val tarjetasEstudiadas: Int = 0,
    @SerialName("reto_hoy") val retoHoy: Boolean = false,
    @SerialName("pendientes_repaso") val pendientesRepaso: Int = 0,
    val continuar: ContinuarEstudio? = null,
)

@Serializable
data class FilaRanking(
    @SerialName("usuario_id") val usuarioId: String,
    val xp: Int,
    val perfil: CreadorPublico? = null,
)

/**
 * Sesiones de estudio. Toda la lógica del juego (validar respuestas, XP, combos, vidas, tiempo
 * y repetición espaciada) vive en funciones SQL; la app solo envía lo que eligió el usuario.
 */
object EstudioRepository {
    private val client get() = SupabaseProvider.client

    suspend fun infoModos(mazoId: String): InfoModos =
        client.postgrest.rpc("info_modos", buildJsonObject { put("p_mazo_id", mazoId) }).decodeAs<InfoModos>()

    suspend fun iniciar(mazoId: String, modo: ModoJuego): SesionNueva =
        client.postgrest.rpc("iniciar_sesion", buildJsonObject {
            put("p_mazo_id", mazoId)
            put("p_modo", modo.clave)
        }).decodeAs<SesionNueva>()

    /** Responde una pregunta ([opcion]) o califica una definición ([loSe]). */
    suspend fun responder(sesionId: String, tarjetaId: String, opcion: Int? = null, loSe: Boolean? = null): ResultadoRespuesta =
        client.postgrest.rpc("responder_tarjeta", buildJsonObject {
            put("p_sesion_id", sesionId)
            put("p_tarjeta_id", tarjetaId)
            put("p_opcion", opcion)
            put("p_lo_se", loSe)
        }).decodeAs<ResultadoRespuesta>()

    /** Abandona la sesión (o la cierra por tiempo en Contrarreloj). */
    suspend fun terminar(sesionId: String): ResumenSesion =
        client.postgrest.rpc("terminar_sesion", buildJsonObject { put("p_sesion_id", sesionId) }).decodeAs<ResumenSesion>()

    suspend fun resumen(sesionId: String): ResumenSesion =
        client.postgrest.rpc("resumen_sesion", buildJsonObject { put("p_sesion_id", sesionId) }).decodeAs<ResumenSesion>()

    suspend fun resumenInicio(): ResumenInicio =
        client.postgrest.rpc("resumen_inicio").decodeAs<ResumenInicio>()

    /** Sesiones terminadas (sin las abandonadas), de la más reciente a la más antigua. */
    suspend fun historial(): List<ResumenSesion> =
        client.from("sesiones").select(
            Columns.raw(
                "id, mazo_id, mazo_nombre, mazo_color, modo, estado, vidas, limite_segundos, aciertos, errores, " +
                    "lo_se, practicar, mejor_combo, xp, xp_bono, xp_total, xp_doble, iniciada_en, terminada_en"
            )
        ) {
            filter { isIn("estado", listOf("completada", "fallida", "tiempo_agotado")) }
            order("iniciada_en", Order.DESCENDING)
            limit(500)
        }.decodeList<ResumenSesion>()

    /** Lunes de esta semana en la hora de Colombia (la misma regla que usa el servidor). */
    fun semanaActual(): String =
        LocalDate.now(ZoneId.of("America/Bogota")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    suspend fun ranking(): List<FilaRanking> =
        client.from("xp_semanal").select(
            Columns.raw("usuario_id, xp, perfil:perfiles_publicos(nombre_usuario, avatares(archivo, color))")
        ) {
            filter { eq("semana", semanaActual()) }
            order("xp", Order.DESCENDING)
            limit(50)
        }.decodeList<FilaRanking>()

    fun usuarioId(): String? = client.auth.currentUserOrNull()?.id

    /**
     * Avisa cada vez que cambia el XP semanal de cualquier usuario (Supabase Realtime).
     * El canal se abre al empezar a recolectar el Flow y se cierra al dejar de hacerlo.
     */
    fun cambiosRanking(): Flow<Unit> = flow {
        val canal = client.channel("ranking-${UUID.randomUUID()}")
        try {
            val cambios = canal.postgresChangeFlow<PostgresAction>(schema = "public") { table = "xp_semanal" }
            canal.subscribe()
            emitAll(cambios.map { })
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(canal) }
        }
    }
}
