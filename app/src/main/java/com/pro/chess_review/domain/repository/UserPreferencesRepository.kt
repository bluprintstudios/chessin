package com.pro.chess_review.domain.repository

import com.pro.chess_review.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun getAiConsent(): Boolean
    suspend fun setAiConsent(granted: Boolean)
    suspend fun getAiUsageCount(): Int
    suspend fun incrementAiUsageCount()
    suspend fun setChessPieceSetId(id: String)
    suspend fun setHudThemeId(id: String)
}
