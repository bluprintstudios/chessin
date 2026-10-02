package com.pro.chessin.data.billing

import com.pro.chessin.domain.billing.SubscriptionState
import com.pro.chessin.domain.billing.SubscriptionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SubscriptionState and paywall feature boundaries.
 */
class SubscriptionStateTest {

    @Test
    fun testAiCoachUnlocking_OnlyUnlockedForProAndGracePeriod() {
        val freeState = SubscriptionState(status = SubscriptionStatus.FREE_TIER)
        assertFalse("AI Coach should be locked on free tier", freeState.isAiCoachUnlocked)

        val proState = SubscriptionState(status = SubscriptionStatus.PRO_SUBSCRIBED)
        assertTrue("AI Coach should be unlocked for pro subscriber", proState.isAiCoachUnlocked)

        val graceState = SubscriptionState(status = SubscriptionStatus.GRACE_PERIOD)
        assertTrue("AI Coach should be unlocked during grace period", graceState.isAiCoachUnlocked)

        val holdState = SubscriptionState(status = SubscriptionStatus.ON_HOLD)
        assertFalse("AI Coach should be locked when subscription is on hold", holdState.isAiCoachUnlocked)

        val expiredState = SubscriptionState(status = SubscriptionStatus.EXPIRED)
        assertFalse("AI Coach should be locked when subscription is expired", expiredState.isAiCoachUnlocked)
    }

    @Test
    fun testShowPaymentWarningBanner_ShowsOnlyForGracePeriodAndOnHold() {
        val freeState = SubscriptionState(status = SubscriptionStatus.FREE_TIER)
        assertFalse(freeState.showPaymentWarningBanner)

        val proState = SubscriptionState(status = SubscriptionStatus.PRO_SUBSCRIBED)
        assertFalse(proState.showPaymentWarningBanner)

        val graceState = SubscriptionState(status = SubscriptionStatus.GRACE_PERIOD)
        assertTrue(graceState.showPaymentWarningBanner)

        val holdState = SubscriptionState(status = SubscriptionStatus.ON_HOLD)
        assertTrue(holdState.showPaymentWarningBanner)

        val expiredState = SubscriptionState(status = SubscriptionStatus.EXPIRED)
        assertFalse(expiredState.showPaymentWarningBanner)
    }
}
