package com.pro.chessin.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import com.pro.chessin.data.DiTest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diTest: DiTest
) : ViewModel() {
    fun getDiTestMessage(): String = diTest.getMessage()
}
