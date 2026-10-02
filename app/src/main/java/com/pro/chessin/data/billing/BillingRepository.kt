package com.pro.chessin.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pro.chessin.domain.billing.SubscriptionState
import com.pro.chessin.domain.billing.SubscriptionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play Billing Library client connection, product details queries,
 * subscription purchase flow, purchase restoration, and subscription status updates.
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingRepository"
        const val PRODUCT_MONTHLY = "subscription_pro_monthly"
        const val PRODUCT_YEARLY = "subscription_pro_yearly"
    }

    private val externalScope = CoroutineScope(Dispatchers.IO)

    private val _subscriptionState = MutableStateFlow(SubscriptionState(isConnecting = true))
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private var productDetailsMap = emptyMap<String, ProductDetails>()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        _subscriptionState.update { it.copy(isConnecting = true) }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully")
                    queryProductsAndPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _subscriptionState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = "Billing setup failed: ${billingResult.debugMessage}"
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected, retrying connection...")
                _subscriptionState.update { it.copy(isConnecting = false) }
            }
        })
    }

    private fun queryProductsAndPurchases() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                productDetailsMap = map

                val monthlyPrice = map[PRODUCT_MONTHLY]?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$4.99/mo"

                val yearlyPrice = map[PRODUCT_YEARLY]?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$39.99/yr"

                _subscriptionState.update {
                    it.copy(
                        isConnecting = false,
                        monthlyPriceString = monthlyPrice,
                        yearlyPriceString = yearlyPrice
                    )
                }

                restorePurchases()
            } else {
                Log.e(TAG, "Query product details failed: ${billingResult.debugMessage}")
                _subscriptionState.update {
                    it.copy(
                        isConnecting = false,
                        monthlyPriceString = "$4.99/mo",
                        yearlyPriceString = "$39.99/yr"
                    )
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            Log.e(TAG, "Product details not found for ID: $productId")
            _subscriptionState.update { it.copy(errorMessage = "Product unavailable") }
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "Offer token not found for ID: $productId")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            } else {
                Log.e(TAG, "Query purchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled purchase flow")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            _subscriptionState.update { it.copy(errorMessage = "Purchase failed: ${billingResult.debugMessage}") }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        var activeSubFound = false
        var gracePeriodFound = false

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                activeSubFound = true
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                gracePeriodFound = true
            }
        }

        val newStatus = when {
            activeSubFound -> SubscriptionStatus.PRO_SUBSCRIBED
            gracePeriodFound -> SubscriptionStatus.GRACE_PERIOD
            else -> SubscriptionStatus.FREE_TIER
        }

        _subscriptionState.update {
            it.copy(
                status = newStatus,
                errorMessage = null
            )
        }
    }
}
