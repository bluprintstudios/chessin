package com.pro.chessin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pro.chessin.domain.analysis.AnalyzedMove
import com.pro.chessin.domain.analysis.ClassificationTier
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.ui.components.BoardOrientation
import com.pro.chessin.ui.components.Chessboard
import com.pro.chessin.ui.components.EvaluationBar
import com.pro.chessin.ui.screens.viewmodels.AnalysisDashboardViewModel
import com.pro.chessin.ui.theme.ClassificationColors

/**
 * Full-screen analysis dashboard showing:
 * - An interactive chess board + evaluation bar
 * - Top Action Bar with "Paste PGN" and "Sample Game" options
 * - A move list with per-move classifications and deltas
 * - An explanation panel and AI Coach chat section
 */
@Composable
fun AnalysisDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalysisDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val coachChatState by viewModel.coachChatState.collectAsState()
    val selectedSquare by viewModel.selectedSquare.collectAsState()
    val moves = uiState.analyzedMoves
    val selectedIdx = uiState.selectedMoveIndex
    val selectedMove = moves.getOrNull(selectedIdx)

    var showPgnDialog by remember { mutableStateOf(false) }

    // Reconstruct board state from the selected move's FEN
    val boardState = remember(selectedMove?.fenAfter) {
        selectedMove?.fenAfter?.let { fen ->
            try {
                ChessBoardState.fromFEN(fen)
            } catch (_: Exception) {
                null
            }
        } ?: ChessBoardState.startPosition()
    }

    val progressFraction = if (uiState.progressTotal > 0) {
        (uiState.progressCurrent.toFloat() / uiState.progressTotal.toFloat())
            .coerceIn(0f, 1f)
    } else 0f

    val showCancel = uiState.isLoading
    val showRetry = uiState.error != null ||
        uiState.isCancelled ||
        (moves.isNotEmpty() && !uiState.isLoading)

    if (showPgnDialog) {
        var pgnInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPgnDialog = false },
            title = { Text("Paste PGN Game") },
            text = {
                OutlinedTextField(
                    value = pgnInput,
                    onValueChange = { pgnInput = it },
                    label = { Text("Paste PGN string") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pgnInput.isNotBlank()) {
                            viewModel.importPgn(pgnInput)
                        }
                        showPgnDialog = false
                    }
                ) {
                    Text("Analyze")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPgnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F141E))
            .padding(8.dp)
    ) {
        // ── Top Header & Actions ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analysis Dashboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showPgnDialog = true },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Paste PGN", fontSize = 12.sp)
                }
                Button(
                    onClick = { viewModel.retryAnalysis() },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Sample Game", fontSize = 12.sp)
                }
            }
        }

        // ── Progress bar (only when analysis is running) ──────────────
        if (uiState.isLoading) {
            LinearProgressIndicatorWithColor(
                progress = progressFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF4A90D9),
                trackColor = Color(0xFF191E2B)
            )
        }

        // ── Board + Eval bar ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvaluationBar(
                evalCp = selectedMove?.evalAfterCp ?: 0,
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
            )
            Chessboard(
                board = boardState,
                selectedSquare = selectedSquare,
                onSquareSelected = { square -> viewModel.onSquareSelected(square) },
                onMoveAttempted = { from, to, promo -> viewModel.onMoveAttempted(from, to, promo) },
                orientation = BoardOrientation.WHITE_AT_BOTTOM,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            )
        }

        // ── Move Navigation Controls ─────────────────────────────────
        if (moves.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.firstMove() },
                    enabled = selectedIdx > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("|<", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = { viewModel.prevMove() },
                    enabled = selectedIdx > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("<", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${selectedIdx + 1} / ${moves.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = { viewModel.nextMove() },
                    enabled = selectedIdx < moves.lastIndex,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(">", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = { viewModel.lastMove() },
                    enabled = selectedIdx < moves.lastIndex,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(">|", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // ── Move list ─────────────────────────────────────────────────
        if (moves.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(moves, key = { it.moveIndex }) { move ->
                    val isSelected = move.moveIndex == selectedIdx
                    MoveListRow(
                        move = move,
                        isSelected = isSelected,
                        onClick = { viewModel.selectMove(move.moveIndex) }
                    )
                }
            }
        } else if (!uiState.isLoading && uiState.error == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Play moves on the board or tap 'Sample Game' / 'Paste PGN'",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
            }
        }

        // ── Error message ─────────────────────────────────────────────
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color(0xFFF44336),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // ── Explanation panel & Coach Chat ───────────────────────────
        ExplanationPanel(
            move = selectedMove,
            coachChatState = coachChatState,
            isAnalyzing = uiState.isLoading,
            onRequestCoachExplanation = { question ->
                viewModel.requestCoachExplanation(question)
            }
        )

        // ── Cancel / Retry buttons ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showCancel) {
                Button(
                    onClick = { viewModel.cancelAnalysis() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel Analysis")
                }
            }
        }
    }
}

/**
 * Wrapper around LinearProgressIndicator that uses the non-deprecated lambda
 * overload when available, falling back to the Float overload otherwise.
 */
@Composable
private fun LinearProgressIndicatorWithColor(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4A90D9),
    trackColor: Color = Color(0xFF191E2B),
) {
    @Suppress("DEPRECATION")
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

// ─── Move list row ───────────────────────────────────────────────────────────

@Composable
fun MoveListRow(
    move: AnalyzedMove,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) {
        ClassificationColors.backgroundForTier(move.tier).copy(alpha = 0.6f)
    } else {
        ClassificationColors.backgroundForTier(move.tier)
    }

    val moveLabel = remember(move) {
        val moveNumber = move.moveIndex / 2 + 1
        val isWhiteMove = move.moveIndex % 2 == 0
        val suffix = if (isWhiteMove) ". " else "... "
        "$moveNumber$suffix${move.san}"
    }

    val deltaText = remember(move.deltaCp) {
        if (move.deltaCp > 0) "+${move.deltaCp}" else move.deltaCp.toString()
    }

    val deltaColor = ClassificationColors.deltaColorForCp(move.deltaCp)

    val isBook = move.isBookMove

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = moveLabel,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBook) {
                Text(
                    text = "BOOK",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = ClassificationColors.labelForTier(move.tier),
                    color = ClassificationColors.forTier(move.tier),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = deltaText,
                    color = deltaColor,
                    fontSize = 12.sp
                )
            }
            if (!isBook) {
                Spacer(Modifier.width(8.dp))
                val evalText = remember(move.evalAfterCp) {
                    val sign = if (move.evalAfterCp > 0) "+" else ""
                    "$sign${move.evalAfterCp}"
                }
                Text(
                    text = evalText,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ─── Explanation panel ───────────────────────────────────────────────────────

@Composable
fun ExplanationPanel(
    move: AnalyzedMove?,
    coachChatState: com.pro.chessin.ui.screens.viewmodels.CoachChatState,
    isAnalyzing: Boolean,
    onRequestCoachExplanation: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF191E2B),
            disabledContainerColor = Color(0xFF191E2B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val mv = move
            if (mv != null) {
                val moveLabel = remember(mv) {
                    val moveNumber = mv.moveIndex / 2 + 1
                    val isWhiteMove = mv.moveIndex % 2 == 0
                    val suffix = if (isWhiteMove) ". " else "... "
                    "$moveNumber$suffix${mv.san}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = moveLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = ClassificationColors.labelForTier(mv.tier),
                        style = MaterialTheme.typography.titleSmall,
                        color = ClassificationColors.forTier(mv.tier),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = tierDescription(mv.tier),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (!mv.isBookMove && mv.deltaCp != 0) {
                    Text(
                        text = buildString {
                            append("Δ: ")
                            if (mv.deltaCp > 0) append("+")
                            append("${mv.deltaCp} cp")
                            append(" · Eval: ")
                            if (mv.evalAfterCp > 0) append("+")
                            append("${mv.evalAfterCp}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (mv.isBookMove) {
                    Text(
                        text = "Matched opening book — engine analysis skipped to save compute",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "Current Position Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Play moves on the board or tap 'Ask AI Coach' for positional guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // AI Coach Chat Section (Always Available)
            Spacer(Modifier.height(12.dp))
            if (isAnalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        color = Color(0xFF4A90D9),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Engine analyzing position...",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp
                    )
                }
            } else {
                when (coachChatState) {
                    is com.pro.chessin.ui.screens.viewmodels.CoachChatState.Idle -> {
                        Button(
                            onClick = { onRequestCoachExplanation("Explain this move and position") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ask AI Coach")
                        }
                    }
                    is com.pro.chessin.ui.screens.viewmodels.CoachChatState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                color = Color(0xFF4A90D9),
                                strokeWidth = 2.dp
                            )
                            Text("Connecting to AI Coach...", color = Color(0xFFB0B0B0), fontSize = 13.sp)
                        }
                    }
                    is com.pro.chessin.ui.screens.viewmodels.CoachChatState.Streaming -> {
                        Column {
                            Text(
                                text = "AI Coach Explanation:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A90D9),
                                fontSize = 12.sp
                            )
                            Text(
                                text = coachChatState.accumulatedText,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRequestCoachExplanation("Explain this move and position in more detail") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ask AI Coach Again")
                            }
                        }
                    }
                    is com.pro.chessin.ui.screens.viewmodels.CoachChatState.Error -> {
                        Column {
                            Text(
                                text = "AI Coach Error: ${coachChatState.message}",
                                color = Color(0xFFF44336),
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { onRequestCoachExplanation(coachChatState.lastQuestion) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a human-readable description for the given classification tier.
 */
@Composable
fun tierDescription(tier: ClassificationTier): String = when (tier) {
    ClassificationTier.BOOK ->
        "Opening book move — matched before engine analysis"
    ClassificationTier.BEST ->
        "Best move — exactly matches the engine's top suggestion"
    ClassificationTier.EXCELLENT ->
        "Excellent — virtually no evaluation loss"
    ClassificationTier.GOOD ->
        "Good — a perfectly playable move"
    ClassificationTier.INACCURACY ->
        "Inaccuracy — a slightly better move was available"
    ClassificationTier.MISTAKE ->
        "Mistake — the evaluation dropped noticeably"
    ClassificationTier.BLUNDER ->
        "Blunder — severe evaluation loss, likely losing material or position"
    ClassificationTier.MISS ->
        "Miss — a forced mate or decisive tactical win was available"
}
