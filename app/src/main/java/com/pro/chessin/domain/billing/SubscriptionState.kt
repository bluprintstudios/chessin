package com.pro.chessin.domain.billing

/**
 * Subscription states for the app paywall boundary.
 *
 * Per AGENTS.md Licensing & Phase 13 Rules:
 * - On-device Stockfish engine analysis, local move classification,
 *   local puzzle solving, and repertoire practice remain 100% free and ungated.
 * - Server-backed AI Coach chat requests (via Cloudflare Worker) and elevated
 *   daily request caps are gated behind PRO_SUBSCRIBED / GRACE_PERIOD.
 */
enum class SubscriptionStatus {
    FREE_TIER,
    PRO_SUBSCRIBED,
    GRACE_PERIOD,
    ON_HOLD,
    EXPIRED
}

/**
 * Immutable domain state representing current user subscription and product info.
 */
data class SubscriptionState(
    val status: SubscriptionStatus = SubscriptionStatus.FREE_TIER,
    val monthlyPriceString: String? = null,
    val yearlyPriceString: String? = null,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Whether server-backed AI Coach chat features are currently accessible.
     */
    val isAiCoachUnlocked: Boolean
        get() = status == SubscriptionStatus.PRO_SUBSCRIBED || status == SubscriptionStatus.GRACE_PERIOD

    /**
     * Whether a payment issue banner should be displayed (soft warning).
     */
    val showPaymentWarningBanner: Boolean
        get() = status == SubscriptionStatus.GRACE_PERIOD || status == SubscriptionStatus.ON_HOLD
}
