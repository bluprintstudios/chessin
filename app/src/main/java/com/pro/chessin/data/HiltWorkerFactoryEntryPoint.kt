package com.pro.chessin.data

import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing HiltWorkerFactory from contexts outside
 * of Hilt's normal injection graph.
 *
 * Note: ChessinApplication already injects HiltWorkerFactory directly via @Inject,
 * so this entry point is available for any future classes that need it.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltWorkerFactoryEntryPoint {

    val workerFactory: HiltWorkerFactory
}
