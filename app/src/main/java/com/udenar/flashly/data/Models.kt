package com.udenar.flashly.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Avatar del catálogo (tabla avatares); la imagen está en el bucket público «avatares». */
@Serializable
data class Avatar(
    val id: String,
    val archivo: String,
    val color: String,
)

/** Perfil propio, con el avatar embebido por la relación perfiles.avatar_id → avatares.id. */
@Serializable
data class Perfil(
    val id: String,
    @SerialName("nombre_completo") val nombreCompleto: String,
    @SerialName("nombre_usuario") val nombreUsuario: String,
    val telefono: String? = null,
    @SerialName("avatar_id") val avatarId: String,
    val xp: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("avatares") val avatar: Avatar? = null,
)
