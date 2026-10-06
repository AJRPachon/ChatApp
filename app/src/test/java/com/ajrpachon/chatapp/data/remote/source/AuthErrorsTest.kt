package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.domain.model.AuthErrorKind
import com.ajrpachon.chatapp.domain.model.AuthException
import io.github.jan.supabase.exceptions.RestException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthErrorsTest {

    private fun rest(error: String = "", statusCode: Int = 400, message: String? = "boom"): RestException {
        val exception = mockk<RestException>()
        every { exception.error } returns error
        every { exception.statusCode } returns statusCode
        every { exception.message } returns message
        return exception
    }

    private fun kindOf(failure: Throwable): AuthErrorKind? = (failure.toAuthException() as? AuthException)?.kind

    @Test
    fun `supabase error codes map to their kind, ignoring case`() {
        assertEquals(AuthErrorKind.INVALID_CREDENTIALS, kindOf(rest(error = "invalid_credentials")))
        assertEquals(AuthErrorKind.EMAIL_NOT_CONFIRMED, kindOf(rest(error = "Email_Not_Confirmed")))
        assertEquals(AuthErrorKind.EMAIL_ALREADY_REGISTERED, kindOf(rest(error = "user_already_exists")))
    }

    @Test
    fun `the message is enough when the error code is missing`() {
        assertEquals(AuthErrorKind.INVALID_CREDENTIALS, kindOf(IllegalStateException("Invalid login credentials")))
        assertEquals(AuthErrorKind.EMAIL_NOT_CONFIRMED, kindOf(IllegalStateException("Email not confirmed")))
        assertEquals(AuthErrorKind.EMAIL_ALREADY_REGISTERED, kindOf(IllegalStateException("User already registered")))
        assertEquals(AuthErrorKind.EMAIL_ALREADY_REGISTERED, kindOf(IllegalStateException("has already been registered")))
    }

    @Test
    fun `http statuses 401, 429 and 500 map to session, rate limit and server errors`() {
        assertEquals(AuthErrorKind.SESSION_EXPIRED, kindOf(rest(statusCode = 401)))
        assertEquals(AuthErrorKind.TOO_MANY_REQUESTS, kindOf(rest(statusCode = 429)))
        assertEquals(AuthErrorKind.SERVER_ERROR, kindOf(rest(statusCode = 500)))
    }

    @Test
    fun `a failure the app does not react to is returned untouched`() {
        val other = rest(statusCode = 404)
        assertSame(other, other.toAuthException())

        val plain = IllegalStateException("network down")
        assertSame(plain, plain.toAuthException())
        assertNull(kindOf(plain))
    }

    @Test
    fun `the original message and cause survive the mapping`() {
        val original = rest(error = "invalid_credentials", message = "Invalid login credentials")

        val mapped = original.toAuthException() as AuthException

        assertEquals("Invalid login credentials", mapped.message)
        assertSame(original, mapped.cause)
    }

    @Test
    fun `an exception that is already an AuthException is not wrapped again`() {
        val already = AuthException(AuthErrorKind.SERVER_ERROR)

        assertSame(already, already.toAuthException())
    }

    @Test
    fun `coroutine cancellation passes through`() {
        val cancellation = CancellationException("cancelled")

        assertSame(cancellation, cancellation.toAuthException())
    }

    @Test
    fun `mappingAuthErrors rethrows the mapped failure and returns the value otherwise`() {
        assertEquals(7, mappingAuthErrors { 7 })

        val thrown = assertThrows(AuthException::class.java) { mappingAuthErrors { throw rest(statusCode = 429) } }
        assertEquals(AuthErrorKind.TOO_MANY_REQUESTS, thrown.kind)
    }
}
