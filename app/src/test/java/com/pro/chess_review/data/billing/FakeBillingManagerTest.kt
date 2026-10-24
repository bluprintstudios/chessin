package com.pro.chess_review.data.billing

import android.app.Activity
import com.pro.chess_review.domain.billing.BillingManager
import com.pro.chess_review.domain.billing.EntitlementState
import com.pro.chess_review.domain.billing.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBillingManager : BillingManager {
    private val _entitlementState = MutableStateFlow<EntitlementState>(EntitlementState.Loading)
    override val entitlementState: StateFlow<EntitlementState> = _entitlementState.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    override val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    override fun startConnection() {
        _productDetails.value = ProductDetails("$9.99", "Lifetime AI Access", "Premium Lifetime")
        _entitlementState.value = EntitlementState.NotPurchased
    }

    override fun launchBillingFlow(activity: Activity?) {
        // Simulate a successful purchase flow
        _entitlementState.value = EntitlementState.Purchased("mocked_token_12345")
    }

    override fun restorePurchases() {
        // Simulate restore (for testing, we'll just leave it or set an error if needed)
        _entitlementState.value = EntitlementState.Error("Restore not available in mock")
    }

    override fun destroy() {}
}

class FakeBillingManagerTest {

    @Test
    fun startConnection_emitsProductDetailsAndNotPurchasedState() {
        val manager = FakeBillingManager()
        manager.startConnection()

        manager.startConnection() // Ensure product details state is set first
        val details = manager.productDetails.value
        assertEquals("$9.99", details?.formattedPrice)
        assertEquals(EntitlementState.NotPurchased, manager.entitlementState.value)
    }

    @Test
    fun launchBillingFlow_emitsPurchasedStateWithToken() {
        val manager = FakeBillingManager()
        manager.startConnection()
        
        // Since we aren't using mockito, just pass null for testing the Fake behavior
        manager.launchBillingFlow(null as Activity?)

        val state = manager.entitlementState.value
        assertTrue(state is EntitlementState.Purchased)
        assertEquals("mocked_token_12345", (state as EntitlementState.Purchased).token)
    }

    @Test
    fun restorePurchases_emitsErrorState() {
        val manager = FakeBillingManager()
        manager.restorePurchases()

        val state = manager.entitlementState.value
        assertTrue(state is EntitlementState.Error)
    }
}
