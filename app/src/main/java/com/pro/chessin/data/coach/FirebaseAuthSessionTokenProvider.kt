package com.pro.chessin.data.coach

import com.google.firebase.auth.FirebaseAuth
import com.pro.chessin.domain.coach.SessionTokenProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseAuthSessionTokenProvider retrieves real Firebase Auth JWT ID Tokens.
 * Returns null if the user is unauthenticated or if token acquisition fails.
 */
@Singleton
class FirebaseAuthSessionTokenProvider @Inject constructor() : SessionTokenProvider {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override suspend fun getSessionToken(): String? {
        val currentUser = auth.currentUser ?: return null
        return try {
            val tokenResult = currentUser.getIdToken(false).await()
            tokenResult.token
        } catch (e: Exception) {
            null
        }
    }
}
