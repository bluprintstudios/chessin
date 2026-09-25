package com.pro.chessin.data

import android.content.Context
import com.pro.chessin.data.local.ChessinDatabase
import com.pro.chessin.data.local.dao.GameDao
import com.pro.chessin.data.local.dao.MoveDao
import com.pro.chessin.data.local.dao.PuzzleDao
import com.pro.chessin.data.local.dao.RepertoireDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and DAO dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideChessinDatabase(
        @ApplicationContext context: Context
    ): ChessinDatabase {
        return ChessinDatabase.getInstance(context)
    }

    @Provides
    fun provideGameDao(database: ChessinDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    fun provideMoveDao(database: ChessinDatabase): MoveDao {
        return database.moveDao()
    }

    @Provides
    fun provideRepertoireDao(database: ChessinDatabase): RepertoireDao {
        return database.repertoireDao()
    }

    @Provides
    fun providePuzzleDao(database: ChessinDatabase): PuzzleDao {
        return database.puzzleDao()
    }
}
