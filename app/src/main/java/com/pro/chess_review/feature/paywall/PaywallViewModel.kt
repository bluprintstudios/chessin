package com.pro.chess_review.feature.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pro.chess_review.domain.billing.BillingManager
import com.pro.chess_review.domain.billing.EntitlementState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PaywallViewModel(
    private val billingManager: BillingManager,
) : ViewModel() {

    val productDetails = billingManager.productDetails

    val entitlementState = billingManager.entitlementState

    init {
        billingManager.startConnection()
    }

    fun launchPurchaseFlow(activity: Activity?) {
        billingManager.launchBillingFlow(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    override fun onCleared() {
        billingManager.destroy()
    }
}

class PaywallViewModelFactory(
    private val billingManager: BillingManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(PaywallViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return PaywallViewModel(billingManager) as T
    }
}
