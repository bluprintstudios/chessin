package com.pro.chessin.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiTest @Inject constructor() {
    fun getMessage(): String = "Hilt is working!"
}
