package com.pro.chessin.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pro.chessin.data.billing.BillingRepository
import com.pro.chessin.domain.billing.SubscriptionStatus
import com.pro.chessin.ui.screens.viewmodels.SubscriptionViewModel

/**
 * PaywallScreen presenting subscription tiers, pricing from Google Play,
 * "Restore Purchases", and payment issue banners for GRACE_PERIOD / ON_HOLD.
 *
 * Per AGENTS.md Licensing & Phase 13 Rules:
 * - On-device Stockfish analysis, local move classification, and puzzle solving
 *   are 100% FREE for all users and explicitly highlighted in the UI.
 */
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.subscriptionState.collectAsState()

    var selectedOption by remember { mutableStateOf(BillingRepository.PRODUCT_YEARLY) }

    val monthlyPrice = state.monthlyPriceString ?: "$4.99/mo"
    val yearlyPrice = state.yearlyPriceString ?: "$39.99/yr"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F141E))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top Bar / Close ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "✕",
                color = Color(0xFF9F9F9F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Headline ──────────────────────────────────────────────────
        Text(
            text = "Chessin PRO",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Unlock Grandmaster AI Coaching & Unlimited Cloud Sync",
            fontSize = 14.sp,
            color = Color(0xFF4A90D9),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Payment Warning Banner (GRACE_PERIOD / ON_HOLD) ───────────
        if (state.showPaymentWarningBanner) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE74C3C).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (state.status == SubscriptionStatus.GRACE_PERIOD) {
                        "⚠️ Payment Issue: Your subscription is in a grace period. Please update your payment method in Google Play."
                    } else {
                        "⚠️ Subscription On Hold: Please update your payment method in Google Play to restore AI Coaching."
                    },
                    color = Color(0xFFFFA07A),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Free Tier Banner (Reassurance) ────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191E2B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "✓ ALWAYS 100% FREE:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2ECC71)
                )
                Text(
                    text = "On-device Stockfish engine analysis, move classification, local puzzle solving, and opening repertoire practice are completely free for all users.",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Subscription Tier Options ─────────────────────────────────
        if (state.isConnecting) {
            CircularProgressIndicator(color = Color(0xFF4A90D9))
        } else {
            // Yearly Plan (Best Value)
            val isYearlySelected = selectedOption == BillingRepository.PRODUCT_YEARLY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isYearlySelected) 2.dp else 1.dp,
                        color = if (isYearlySelected) Color(0xFF4A90D9) else Color(0xFF2C3E50),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedOption = BillingRepository.PRODUCT_YEARLY },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191E2B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Yearly Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2ECC71)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SAVE 33%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Full access to AI Coach & Cloud Sync",
                            fontSize = 12.sp,
                            color = Color(0xFF9F9F9F),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Text(
                        text = yearlyPrice,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90D9)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Monthly Plan
            val isMonthlySelected = selectedOption == BillingRepository.PRODUCT_MONTHLY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isMonthlySelected) 2.dp else 1.dp,
                        color = if (isMonthlySelected) Color(0xFF4A90D9) else Color(0xFF2C3E50),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedOption = BillingRepository.PRODUCT_MONTHLY },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191E2B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Pro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Flexible month-to-month access",
                            fontSize = 12.sp,
                            color = Color(0xFF9F9F9F),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Text(
                        text = monthlyPrice,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Subscribe Button ──────────────────────────────────────────
        Button(
            onClick = {
                activity?.let {
                    viewModel.launchPurchaseFlow(it, selectedOption)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9))
        ) {
            Text(
                text = "Subscribe Now",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Restore Purchases Button ──────────────────────────────────
        OutlinedButton(
            onClick = { viewModel.restorePurchases() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = "Restore Purchases",
                fontSize = 14.sp,
                color = Color(0xFF9F9F9F)
            )
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.errorMessage!!,
                color = Color(0xFFE74C3C),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
