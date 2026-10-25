package com.pro.chess_review.domain.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface BillingManager {
    val entitlementState: StateFlow<EntitlementState>
    val productDetails: StateFlow<ProductDetails?>
    
    fun startConnection()
    fun launchBillingFlow(activity: Activity?)
    fun restorePurchases()
    fun destroy()
}
