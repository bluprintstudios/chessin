package com.pro.chessin.data

import com.pro.chessin.data.engine.EngineSessionManager
import com.pro.chessin.domain.engine.EngineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {
    @Binds
    @Singleton
    abstract fun bindEngineRepository(
        impl: EngineSessionManager
    ): EngineRepository
}

