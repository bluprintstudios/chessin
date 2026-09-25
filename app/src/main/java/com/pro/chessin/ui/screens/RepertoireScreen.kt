package com.pro.chessin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pro.chessin.data.model.RepertoireNode
import com.pro.chessin.ui.components.Chessboard
import com.pro.chessin.ui.screens.viewmodels.PracticeFeedbackType
import com.pro.chessin.ui.screens.viewmodels.RepertoireColor
import com.pro.chessin.ui.screens.viewmodels.RepertoireMode
import com.pro.chessin.ui.screens.viewmodels.RepertoireViewModel

@Composable
fun RepertoireScreen(
    viewModel: RepertoireViewModel = hiltViewModel()
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
        Text(
            text = "Opening Repertoire Builder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // White / Black Repertoire Selector
        TabRow(
            selectedTabIndex = if (state.selectedColor == RepertoireColor.WHITE) 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = state.selectedColor == RepertoireColor.WHITE,
                onClick = { viewModel.selectColor(RepertoireColor.WHITE) },
                text = { Text("White Repertoire") }
            )
            Tab(
                selected = state.selectedColor == RepertoireColor.BLACK,
                onClick = { viewModel.selectColor(RepertoireColor.BLACK) },
                text = { Text("Black Repertoire") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode Selector: Browse & Edit vs Practice Mode
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = state.mode == RepertoireMode.BROWSE,
                onClick = { viewModel.setMode(RepertoireMode.BROWSE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Browse & Edit")
            }
            SegmentedButton(
                selected = state.mode == RepertoireMode.PRACTICE,
                onClick = { viewModel.setMode(RepertoireMode.PRACTICE) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Practice")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.mode == RepertoireMode.BROWSE) {
            BrowseModeView(state = state, viewModel = viewModel)
        } else {
            PracticeModeView(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun BrowseModeView(
    state: com.pro.chessin.ui.screens.viewmodels.RepertoireUiState,
    viewModel: RepertoireViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
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

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Breadcrumbs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Line Path",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { viewModel.resetToStart() },
                            label = { Text("Start") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    itemsIndexed(state.currentPath) { index, node ->
                        AssistChip(
                            onClick = { viewModel.selectBreadcrumb(index) },
                            label = { Text(node.moveSan) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.stepBack() }, enabled = state.currentPath.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Step Back")
                    }
                    TextButton(onClick = { viewModel.resetToStart() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Board")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Variations Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Variations at this position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.availableVariations.isEmpty()) {
                    Text(
                        text = "No variations recorded yet. Play a move on the board above to add a new line!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow {
                        items(state.availableVariations) { variation ->
                            AssistChip(
                                onClick = { viewModel.selectVariation(variation) },
                                label = { Text(variation.moveSan) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Position Comment / Notes Section
        if (state.currentNode != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Notes for ${state.currentNode.moveSan}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = { viewModel.updateComment(it) },
                        label = { Text("Comment / Strategy note") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.deleteCurrentNode() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Move")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Move")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeModeView(
    state: com.pro.chessin.ui.screens.viewmodels.RepertoireUiState,
    viewModel: RepertoireViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.isPracticeComplete) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All Caught Up!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No moves are due for review today in your ${state.selectedColor.name.lowercase()} repertoire.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.setMode(RepertoireMode.BROWSE) }) {
                        Text("Return to Browse")
                    }
                }
            }
        } else {
            // Status Header
            Text(
                text = "Practice ${state.selectedColor.name} Repertoire (Line ${state.currentPracticeIndex + 1} of ${state.dueNodes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Feedback Banner
            state.practiceFeedback?.let { feedback ->
                val containerColor = if (feedback.type == PracticeFeedbackType.CORRECT) {
                    Color(0xFF2E7D32) // Green
                } else {
                    Color(0xFFC62828) // Red
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Text(
                        text = feedback.message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Chessboard for Practice
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
        }
    }
}
