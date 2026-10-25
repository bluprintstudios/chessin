package com.pro.chess_review.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.pro.chess_review.domain.billing.BillingManager
import com.pro.chess_review.domain.billing.EntitlementState
import com.pro.chess_review.domain.billing.ProductDetails as DomainProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayBillingManager(
    private val context: Context,
) : BillingManager, PurchasesUpdatedListener, BillingClientStateListener {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _entitlementState = MutableStateFlow<EntitlementState>(EntitlementState.Loading)
    override val entitlementState: StateFlow<EntitlementState> = _entitlementState.asStateFlow()

    private val _productDetails = MutableStateFlow<DomainProductDetails?>(null)
    override val productDetails: StateFlow<DomainProductDetails?> = _productDetails.asStateFlow()

    private var billingClient: BillingClient? = null
    private var playProductDetails: ProductDetails? = null

    override fun startConnection() {
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()
        }

        billingClient?.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            applicationScope.launch {
                queryProductDetails()
                queryPurchases()
            }
        } else {
            _entitlementState.value = EntitlementState.Error("Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        // We could implement retry logic here, but for now we'll just wait for the user to try again
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                val pd = productDetailsList[0]
                playProductDetails = pd
                val oneTimeInfo = pd.oneTimePurchaseOfferDetails
                if (oneTimeInfo != null) {
                    _productDetails.value = DomainProductDetails(
                        formattedPrice = oneTimeInfo.formattedPrice,
                        description = pd.description,
                        name = pd.name
                    )
                }
            }
        }
    }

    private suspend fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
            
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    override fun launchBillingFlow(activity: Activity?) {
        if (activity == null) return
        val pd = playProductDetails
        if (pd == null) {
            _entitlementState.value = EntitlementState.Error("Product not loaded yet")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(pd)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // User backed out; ignore or set state back to NotPurchased if we were loading
            if (_entitlementState.value is EntitlementState.Loading) {
                _entitlementState.value = EntitlementState.NotPurchased
            }
        } else {
            _entitlementState.value = EntitlementState.Error(billingResult.debugMessage)
        }
    }

    override fun restorePurchases() {
        _entitlementState.value = EntitlementState.Loading
        applicationScope.launch {
            queryPurchases()
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchase = purchases.find { purchase -> 
            purchase.products.contains("premium_lifetime") 
        }

        if (premiumPurchase == null) {
            _entitlementState.value = EntitlementState.NotPurchased
            return
        }

        if (premiumPurchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge if not already acknowledged
            if (!premiumPurchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(premiumPurchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _entitlementState.value = EntitlementState.Purchased(premiumPurchase.purchaseToken)
                    }
                }
            } else {
                _entitlementState.value = EntitlementState.Purchased(premiumPurchase.purchaseToken)
            }
        } else if (premiumPurchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _entitlementState.value = EntitlementState.Loading // Alternatively add a Pending state
        }
    }

    override fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
