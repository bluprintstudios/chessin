package com.pro.chessin.di

import com.pro.chessin.data.coach.BackendAiCoachProvider
import com.pro.chessin.data.coach.FirebaseAuthSessionTokenProvider
import com.pro.chessin.domain.coach.AiCoachProvider
import com.pro.chessin.domain.coach.SessionTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module providing AI Coach dependencies.
 * Binds AiCoachProvider interface to BackendAiCoachProvider.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoachModule {

    @Binds
    @Singleton
    abstract fun bindSessionTokenProvider(
        impl: FirebaseAuthSessionTokenProvider
    ): SessionTokenProvider

    @Binds
    @Singleton
    abstract fun bindAiCoachProvider(
        impl: BackendAiCoachProvider
    ): AiCoachProvider
}
