package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.AuthSessionDTO
import com.ajrpachon.chatapp.data.remote.dto.MfaAssuranceDTO
import com.ajrpachon.chatapp.data.remote.dto.TotpEnrollmentDTO
import com.ajrpachon.chatapp.domain.repository.MfaAssuranceLevel
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.TotpEnrollment
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMapperTest {

    @Test
    fun `session keeps the user id and a nullable email`() {
        assertEquals(SessionInfo("u1", "a@b.c"), AuthSessionDTO("u1", "a@b.c").toBO())
        assertEquals(SessionInfo("u1", null), AuthSessionDTO("u1", null).toBO())
    }

    @Test
    fun `mfa assurance keeps current and next levels`() {
        assertEquals(MfaAssuranceLevel("aal1", "aal2"), MfaAssuranceDTO("aal1", "aal2").toBO())
    }

    @Test
    fun `totp enrollment keeps factor id, qr and secret`() {
        assertEquals(
            TotpEnrollment("f1", "<svg/>", "JBSWY3DP"),
            TotpEnrollmentDTO("f1", "<svg/>", "JBSWY3DP").toBO(),
        )
    }
}
