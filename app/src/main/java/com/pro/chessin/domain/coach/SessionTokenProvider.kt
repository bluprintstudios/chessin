package com.pro.chessin.domain.coach

import javax.inject.Inject

/**
 * Interface for obtaining authenticated user session tokens.
 */
interface SessionTokenProvider {
    /**
     * Returns the active session token string or null if unauthenticated.
     */
    suspend fun getSessionToken(): String?
}

/**
 * Default GuestSessionTokenProvider returning a placeholder guest token.
 */
class GuestSessionTokenProvider @Inject constructor() : SessionTokenProvider {
    override suspend fun getSessionToken(): String = "guest_session_placeholder"
}
