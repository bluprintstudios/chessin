package com.pro.chess_review.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.GameParseStatus
import com.pro.chess_review.ui.state.UiState
import com.pro.chess_review.ui.theme.LocalHudPalette
import com.pro.chess_review.ui.designsystem.HudPanel
import com.pro.chess_review.ui.designsystem.HudScreenHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameLibraryScreen(
    viewModel: GameLibraryViewModel,
    onOpenGame: (String) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val gamesState by viewModel.gamesState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LibraryHeader(
                    gameCount = (gamesState as? UiState.Success)?.data?.size,
                    isImporting = importState is UiState.Loading,
                    onImport = onOpenImport,
                )
            }

            when (val state = gamesState) {
                UiState.Loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                UiState.Empty -> item { EmptyLibraryCard() }

                is UiState.Error -> item { LibraryErrorCard() }

                is UiState.Success -> items(state.data, key = ChessGame::id) { game ->
                    GameCard(game = game, onClick = { onOpenGame(game.id) })
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text("Settings", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = onOpenPaywall, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text("Membership", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = "Independent and unaffiliated with Chess.com.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    gameCount: Int?,
    isImporting: Boolean,
    onImport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HudScreenHeader(title = "GAME LIBRARY", routeLabel = "games")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Your games", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = gameCount?.let { "$it saved locally" } ?: "Local PGN review",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onImport,
                enabled = !isImporting,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Text("IMPORT PGN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard() {
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmptyBoardArt(size = 76.dp)
            Spacer(Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "A fresh board",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Your imported games will appear here. Choose Import PGN to add a completed game.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibraryErrorCard() {
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Your game library couldn't be loaded. Please close and reopen the app.",
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun GameCard(game: ChessGame, onClick: () -> Unit) {
    val canReview = game.parseStatus != GameParseStatus.UNSUPPORTED
    HudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canReview, role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (canReview) "PGN GAME" else "UNSUPPORTED VARIANT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                )
                ResultPill(game.result)
            }

            Text(
                text = "${game.whitePlayer?.takeIf(String::isNotBlank) ?: "White"}  vs  ${game.blackPlayer?.takeIf(String::isNotBlank) ?: "Black"}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = game.playedAtEpochMillis?.let(::formatDate) ?: "Date not provided",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                game.timeControl?.takeIf(String::isNotBlank)?.let { timeControl ->
                    Text(timeControl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = if (canReview) "Open review  →" else "This game variant can't be replayed yet",
                style = MaterialTheme.typography.labelLarge,
                color = if (canReview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ResultPill(result: String?) {
    val label = when (result) {
        "1-0" -> "White won"
        "0-1" -> "Black won"
        "1/2-1/2" -> "Draw"
        "*" -> "Unfinished"
        else -> "Result unknown"
    }
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BoardMark(size: Dp) {
    val palette = LocalHudPalette.current
    Canvas(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
            .background(palette.boardLight),
    ) {
        val cell = this.size.width / 4f
        for (row in 0..3) {
            for (column in 0..3) {
                if ((row + column) % 2 == 0) {
                    drawRect(
                        color = palette.boardDark,
                        topLeft = androidx.compose.ui.geometry.Offset(column * cell, row * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBoardArt(size: Dp) {
    val palette = LocalHudPalette.current
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Canvas(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(17.dp))
            .background(palette.boardLight),
    ) {
        val cell = this.size.width / 4f
        for (row in 0..3) {
            for (column in 0..3) {
                if ((row + column) % 2 == 0) {
                    drawRect(
                        color = palette.boardDark.copy(alpha = 0.82f),
                        topLeft = androidx.compose.ui.geometry.Offset(column * cell, row * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell),
                    )
                }
            }
        }
        drawRoundRect(
            color = outlineColor,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))
