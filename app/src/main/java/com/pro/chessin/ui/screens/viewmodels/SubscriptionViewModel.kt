package com.pro.chessin.ui.screens.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.pro.chessin.data.billing.BillingRepository
import com.pro.chessin.domain.billing.SubscriptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel managing Paywall UI state and billing actions.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = billingRepository.subscriptionState

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        billingRepository.launchPurchaseFlow(activity, productId)
    }

    fun restorePurchases() {
        billingRepository.restorePurchases()
    }
}
