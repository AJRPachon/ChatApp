package com.ajrpachon.chatapp.data.remote.dto

/**
 * Value types AuthRemoteSource hands back, reduced to what the app reads from supabase-kt's own
 * auth types (so nothing above data/remote has to import them). Not wire DTOs: they are built from
 * the SDK's responses, not deserialised from JSON.
 */
data class AuthSessionDTO(val userId: String, val email: String?)

data class MfaAssuranceDTO(val current: String, val next: String)

data class TotpEnrollmentDTO(val factorId: String, val qrCodeSvg: String, val secret: String)
