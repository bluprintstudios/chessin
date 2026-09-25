package com.pro.chessin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pro.chessin.ui.components.Chessboard
import com.pro.chessin.ui.screens.viewmodels.PuzzleStatus
import com.pro.chessin.ui.screens.viewmodels.PuzzlesViewModel

@Composable
fun PuzzlesScreen(
    viewModel: PuzzlesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title & Rating Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tactical Puzzles",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "Rating: ${state.userRating}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.status == PuzzleStatus.LOADING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Feedback Card
            val feedbackBgColor = when (state.status) {
                PuzzleStatus.CORRECT -> Color(0xFF2E7D32) // Green
                PuzzleStatus.FAILED -> Color(0xFFC62828)  // Red
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            val feedbackTextColor = when (state.status) {
                PuzzleStatus.CORRECT, PuzzleStatus.FAILED -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = feedbackBgColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = state.feedbackMessage ?: "Find the best move!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = feedbackTextColor
                    )

                    state.currentPuzzle?.let { puzzle ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Puzzle Rating: ${puzzle.rating} • Source: ${puzzle.source}",
                            style = MaterialTheme.typography.labelMedium,
                            color = feedbackTextColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Chessboard
            Chessboard(
                board = state.boardState,
                selectedSquare = state.selectedSquare,
                onSquareSelected = { viewModel.onSquareSelected(it) },
                onMoveAttempted = { from, to, promo -> viewModel.onMoveAttempted(from, to, promo) },
                orientation = state.boardOrientation,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hint Text
            state.hintText?.let { hint ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.showHint() },
                    enabled = state.status == PuzzleStatus.IN_PROGRESS
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Hint")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hint")
                }

                Button(
                    onClick = { viewModel.loadNextPuzzle() }
                ) {
                    Icon(
                        if (state.status == PuzzleStatus.FAILED) Icons.Default.Refresh else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.status == PuzzleStatus.FAILED) "Retry / Next" else "Next Puzzle")
                }
            }
        }
    }
}
