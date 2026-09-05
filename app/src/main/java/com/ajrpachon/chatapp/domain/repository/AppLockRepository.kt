package com.ajrpachon.chatapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    val isEnabled: Flow<Boolean>
    val backgroundedAt: Flow<Long>
    suspend fun enable()
    suspend fun disable()
    suspend fun recordBackgroundedAt(timestamp: Long)

    /**
     * Whether the device currently has a biometric or device credential (PIN/pattern/
     * password) enrolled that AppLockScreen could actually authenticate against. Must be
     * checked before [enable] — enabling app lock with nothing enrolled would strand the
     * user on AppLockScreen with no way back in (the biometric prompt never auto-launches,
     * and since the back-press bypass was fixed, there is no other UI-driven way out).
     */
    fun canUseDeviceCredential(): Boolean
}
