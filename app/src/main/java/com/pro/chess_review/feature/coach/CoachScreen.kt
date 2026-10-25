package com.pro.chess_review.feature.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.chess_review.domain.billing.EntitlementState
import com.pro.chess_review.ui.designsystem.HudPanel
import com.pro.chess_review.ui.designsystem.HudScreenHeader
import com.pro.chess_review.domain.model.CoachRequest

// Using MaterialTheme.colorScheme based on Figma palette

@Composable
fun CoachScreen(
    request: CoachRequest,
    viewModel: CoachViewModel,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val entitlement by viewModel.entitlementState.collectAsState()
    var question by rememberSaveable(request.fen, request.playedMove) { mutableStateOf("") }
    val hasEvidence = request.engineEvidence.isNotEmpty()
    val consented = state.consent?.hasConsented == true
    val purchased = viewModel.isLocalDebugPreview || entitlement is EntitlementState.Purchased
    val canAsk = viewModel.serviceConfigured && hasEvidence && consented && purchased &&
        !state.isLoading && question.isNotBlank() && state.consent?.isLimitReached != true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudScreenHeader(title = "AI COACH", routeLabel = "coach/{gameId}/{ply}")
                Text(
                    "Ask about one position",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Ask about a completed game. Explanations use the selected position and its Stockfish lines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            PositionCard(request = request)

            if (state.response == null) {
                NoticeCard(
                    title = "COACH EXPLANATION",
                    body = if (hasEvidence) "Ask a focused question to receive a grounded explanation of the selected engine lines." else "A grounded explanation is available once Stockfish lines finish for this position.",
                )
            }

            state.response?.let { response ->
                // Coach Chat Bubble
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Surface(
                        modifier = Modifier.weight(0.9f, fill = false),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✨ COACH EXPLANATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(response.explanation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp)
                            
                            if (response.groundedCandidates.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Engine lines referenced:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                        response.groundedCandidates.forEach { candidate ->
                                            Text("• $candidate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                            if (response.disclaimer.isNotBlank()) {
                                Text(response.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // User Input Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬 YOUR QUESTION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.serviceConfigured,
                        minLines = 1,
                        maxLines = 2,
                        placeholder = { Text("e.g. Why was this move important?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.background,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Checkbox(
                                checked = consented,
                                onCheckedChange = viewModel::setConsent,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allow sending for AI explanation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.size(2.dp))
                                Text(
                                    "Only the selected FEN, question, played move, and Stockfish lines are sent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    val usage = state.consent
                    if (usage != null) {
                        Text(
                            "Preview counter: ${usage.usageCount} / ${usage.fairUseLimit}. Final production allowance and provider terms are pending.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.errorMessage?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }

                    if (!viewModel.isLocalDebugPreview) {
                        when (entitlement) {
                            is EntitlementState.NotPurchased -> OutlinedButton(
                                onClick = onOpenPaywall,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) { Text("View one-time purchase", fontWeight = FontWeight.Bold) }
                            is EntitlementState.Loading -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            }
                            is EntitlementState.Error -> TextButton(onClick = onOpenPaywall, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                                Text("Check purchase options", fontWeight = FontWeight.Bold)
                            }
                            is EntitlementState.Purchased -> Unit
                        }
                    }

                    Button(
                        onClick = { viewModel.ask(question) },
                        enabled = canAsk,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (viewModel.serviceConfigured) "Ask Chessin'" else "AI Coach unavailable in preview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                "Chessin' is independent and unaffiliated with Chess.com. AI explanations are optional; local replay remains available.",
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun PositionCard(request: CoachRequest) {
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Text(
                    "SELECTED POSITION", 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (request.playedMove.isBlank()) "Starting position" else "After ${request.playedMove}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (request.engineEvidence.isEmpty()) "No completed Stockfish lines" else "${request.engineEvidence.size} Stockfish line${if (request.engineEvidence.size == 1) "" else "s"} available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            
            request.engineEvidence.firstOrNull()?.let { evidence ->
                Text(
                    "Top line: ${evidence.san} · ${formatCoachScore(evidence.scoreCp, evidence.mateIn)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatCoachScore(scoreCp: Int?, mateIn: Int?): String = when {
    mateIn != null && mateIn > 0 -> "White mates in $mateIn"
    mateIn != null && mateIn < 0 -> "Black mates in ${kotlin.math.abs(mateIn.toLong())}"
    mateIn == 0 -> "Checkmate"
    scoreCp != null -> String.format(java.util.Locale.ROOT, "%+.2f", scoreCp / 100.0)
    else -> "Score unavailable"
}

@Composable
private fun NoticeCard(title: String, body: String) {
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}
