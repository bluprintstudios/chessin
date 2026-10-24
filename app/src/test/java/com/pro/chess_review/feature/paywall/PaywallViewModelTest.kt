package com.pro.chess_review.feature.paywall

import com.pro.chess_review.data.billing.FakeBillingManager
import com.pro.chess_review.domain.billing.EntitlementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_startsBillingConnection() = runTest {
        val fakeManager = FakeBillingManager()
        val viewModel = PaywallViewModel(fakeManager)

        val productDetails = viewModel.productDetails.value
        assertEquals("$9.99", productDetails?.formattedPrice)
        assertEquals(EntitlementState.NotPurchased, viewModel.entitlementState.value)
    }

    @Test
    fun launchPurchaseFlow_triggersBillingManagerAndUpdatesState() = runTest {
        val fakeManager = FakeBillingManager()
        val viewModel = PaywallViewModel(fakeManager)

        // Mock Activity doesn't matter for the fake
        viewModel.launchPurchaseFlow(null as android.app.Activity?)

        val state = viewModel.entitlementState.value
        assertTrue(state is EntitlementState.Purchased)
        assertEquals("mocked_token_12345", (state as EntitlementState.Purchased).token)
    }

    @Test
    fun restorePurchases_triggersRestoreAndUpdatesState() = runTest {
        val fakeManager = FakeBillingManager()
        val viewModel = PaywallViewModel(fakeManager)

        viewModel.restorePurchases()

        val state = viewModel.entitlementState.value
        assertTrue(state is EntitlementState.Error)
    }
}
