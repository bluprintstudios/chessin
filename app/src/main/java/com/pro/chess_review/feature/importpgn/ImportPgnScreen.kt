package com.pro.chess_review.feature.importpgn

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.chess_review.feature.library.GameLibraryViewModel
import com.pro.chess_review.ui.designsystem.HudPanel
import com.pro.chess_review.ui.designsystem.HudScreenHeader
import com.pro.chess_review.ui.state.UiState

/** The approved HUD handoff for local, source-neutral PGN import. */
@Composable
fun ImportPgnScreen(
    viewModel: GameLibraryViewModel,
    onBack: () -> Unit,
    onImported: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsState()
    val pickPgnFile = rememberPgnFilePicker { uri: Uri ->
        viewModel.importPgnFile(context.applicationContext, uri)
    }

    LaunchedEffect(importState) {
        if (importState is UiState.Success) onImported()
    }

    Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudScreenHeader(title = "IMPORT PGN", routeLabel = "system picker")
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Import a completed game", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            HudPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = importState !is UiState.Loading, onClick = pickPgnFile),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("SYSTEM DOCUMENT PICKER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (importState is UiState.Loading) "Importing your selected PGN…" else "Choose a PGN file you have the right to use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (importState is UiState.Loading) "WORKING" else "CHOOSE FILE  →",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            HudPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("VALIDATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Moves are validated locally before the game is added to your library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HudPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RETURN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "A successful import returns to Your games and preserves the raw moves locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (importState is UiState.Error) {
                Text(
                    "That file couldn't be imported. Choose a completed-game PGN file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
