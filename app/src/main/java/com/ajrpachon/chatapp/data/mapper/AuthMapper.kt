package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.AuthSessionDTO
import com.ajrpachon.chatapp.data.remote.dto.MfaAssuranceDTO
import com.ajrpachon.chatapp.data.remote.dto.TotpEnrollmentDTO
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment

fun AuthSessionDTO.toBO() = SessionInfo(userId = userId, email = email)

fun MfaAssuranceDTO.toBO() = MfaAssuranceLevel(current = current, next = next)

fun TotpEnrollmentDTO.toBO() = TotpEnrollment(factorId = factorId, qrCodeSvg = qrCodeSvg, secret = secret)
