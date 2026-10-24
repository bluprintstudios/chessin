package com.pro.chess_review.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val hasAiConsent: Boolean = false,
    val aiUsageCount: Int = 0,
    val chessPieceSetId: String = "chessnut",
    val hudThemeId: String = "arctic",
)

class UserPreferencesStore(context: Context) : com.pro.chess_review.domain.repository.UserPreferencesRepository {
    private val dataStore = context.applicationContext.userPreferencesDataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { values ->
            UserPreferences(
                hasCompletedOnboarding = values[Keys.onboardingComplete] ?: false,
                hasAiConsent = values[Keys.aiConsent] ?: false,
                aiUsageCount = values[Keys.aiUsageCount] ?: 0,
                chessPieceSetId = values[Keys.chessPieceSetId] ?: "chessnut",
                hudThemeId = values[Keys.hudThemeId] ?: "arctic",
            )
        }

    override suspend fun getAiConsent(): Boolean {
        return preferences.first().hasAiConsent
    }

    override suspend fun setAiConsent(granted: Boolean) {
        dataStore.edit { values ->
            values[Keys.aiConsent] = granted
        }
    }

    override suspend fun getAiUsageCount(): Int {
        return preferences.first().aiUsageCount
    }

    override suspend fun incrementAiUsageCount() {
        dataStore.edit { values ->
            val current = values[Keys.aiUsageCount] ?: 0
            values[Keys.aiUsageCount] = current + 1
        }
    }

    override suspend fun setChessPieceSetId(id: String) {
        dataStore.edit { values ->
            values[Keys.chessPieceSetId] = id
        }
    }

    override suspend fun setHudThemeId(id: String) {
        dataStore.edit { values ->
            values[Keys.hudThemeId] = id
        }
    }

    suspend fun setOnboardingComplete(completed: Boolean) {
        dataStore.edit { values ->
            values[Keys.onboardingComplete] = completed
        }
    }

    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val aiConsent = booleanPreferencesKey("ai_consent")
        val aiUsageCount = intPreferencesKey("ai_usage_count")
        val chessPieceSetId = stringPreferencesKey("chess_piece_set_id")
        val hudThemeId = stringPreferencesKey("hud_theme_id")
    }
}
