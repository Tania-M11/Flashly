package com.udenar.flashly.ui

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val espanol: Locale = Locale.forLanguageTag("es-CO")

/** "2026-10-28T23:51:29+00:00" → "28 de octubre de 2026", en la zona horaria del teléfono (null si no se puede leer). */
fun fechaLarga(iso: String?): String? = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", espanol))
}.getOrNull()

/** "2026-10-28T…" → "octubre de 2026". */
fun mesYAnio(iso: String?): String? = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", espanol))
}.getOrNull()

/** Fecha relativa para la última actividad: "hoy", "ayer", "hace 3 días" o "12 de octubre". */
fun haceCuanto(iso: String?): String? = runCatching {
    val fecha = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    when (val dias = java.time.temporal.ChronoUnit.DAYS.between(fecha, LocalDate.now()).toInt()) {
        0 -> "hoy"
        1 -> "ayer"
        in 2..6 -> "hace $dias días"
        else -> fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM", espanol))
    }
}.getOrNull()

/** Fecha de hoy para el saludo de Inicio: "miércoles, 28 de octubre". */
fun hoyLegible(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", espanol)).replaceFirstChar { it.uppercase() }
