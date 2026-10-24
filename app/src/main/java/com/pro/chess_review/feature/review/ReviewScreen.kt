package com.pro.chess_review.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.EngineEvidenceItem
import com.pro.chess_review.domain.model.MoveQuality
import com.pro.chess_review.domain.model.PositionReview
import com.pro.chess_review.domain.model.ReviewCandidateLine
import com.pro.chess_review.ui.state.UiState
import com.pro.chess_review.ui.designsystem.HudPanel
import com.pro.chess_review.ui.designsystem.HudScreenHeader
import com.pro.chess_review.ui.theme.LocalHudPalette

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit = {},
    onAskCoach: (CoachRequest, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.cancelReview() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        ReviewHeader(state = state, onBack = onBack)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val uiState = state) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Empty -> {
                    ReviewMessage(
                        title = "No game data",
                        detail = "Choose a game from your library to review it.",
                        onBack = onBack,
                    )
                }
                is UiState.Error -> {
                    ReviewMessage(
                        title = "This game couldn't be opened",
                        detail = "It may have been removed from your library.",
                        onBack = onBack,
                    )
                }
                is UiState.Success -> {
                    ReviewContent(
                        state = uiState.data,
                        onNextMove = viewModel::nextMove,
                        onPrevMove = viewModel::previousMove,
                        onFlipBoard = viewModel::toggleFlip,
                        onSelectPieceSet = viewModel::selectPieceSet,
                        onAskCoach = onAskCoach,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader(state: UiState<ReviewScreenState>, onBack: () -> Unit) {
    val gameState = (state as? UiState.Success)?.data
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HudScreenHeader(title = "POST-GAME REVIEW", routeLabel = "review/{gameId}")
        Text(
            text = if (gameState == null) "GAME REVIEW" else {
                "${gameState.whitePlayer?.takeIf(String::isNotBlank) ?: "White"}  vs  ${gameState.blackPlayer?.takeIf(String::isNotBlank) ?: "Black"}"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        gameState?.result?.takeIf(String::isNotBlank)?.let { result ->
            Text(
                text = when (result) {
                    "1-0" -> "White won"
                    "0-1" -> "Black won"
                    "1/2-1/2" -> "Draw"
                    "*" -> "Unfinished game"
                    else -> result
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewMessage(title: String, detail: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back to your games") }
    }
}

@Composable
fun ReviewContent(
    state: ReviewScreenState,
    onNextMove: () -> Unit,
    onPrevMove: () -> Unit,
    onFlipBoard: () -> Unit,
    onSelectPieceSet: (String) -> Unit = {},
    onAskCoach: (CoachRequest, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val review = state.review
        val progress = state.reviewProgress
        val isComplete = progress?.isAnalyzing == false

        // Analysis progress indicator
        if (progress != null && progress.isAnalyzing) {
            LinearProgressIndicator(
                progress = { analysisProgressFraction(progress.processedPositions, progress.totalPositions) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            Text(
                "Analyzing: ${progress.processedPositions} / ${progress.totalPositions}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Find current position context
        val currentPly = state.currentPly
        val currentPosition = review?.positions?.find { it.plyIndex == currentPly }
        val currentMove = review?.moves?.find { it.plyIndex == currentPly - 1 }
        val bestLine = currentPosition?.lines?.firstOrNull()

        EvaluationTrajectoryPanel(
            ply = currentPly,
            bestLine = bestLine,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // The reference HUD places replay, engine evidence, and coaching in a
        // compact two-column scan rather than a sequence of full-width cards.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HudPanel(modifier = Modifier.width(184.dp)) {
                Column(
                    modifier = Modifier.padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text("BOARD / REPLAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.height(148.dp)) {
                        EvalBar(
                            whiteCentipawns = bestLine?.whiteCentipawns,
                            whiteMateIn = bestLine?.whiteMateIn,
                            isFlipped = state.isFlipped,
                            modifier = Modifier.fillMaxHeight(),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        ChessBoard(
                            fen = currentPosition?.fen ?: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                            isFlipped = state.isFlipped,
                            pieceSetId = state.pieceSetId,
                            modifier = Modifier.width(148.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        ReplayControl("PREV", onPrevMove, Modifier.weight(1f))
                        ReplayControl("FLIP", onFlipBoard, Modifier.weight(1f))
                        ReplayControl("NEXT", onNextMove, Modifier.weight(1f))
                    }
                    PieceSetSelector(
                        selectedPieceSetId = state.pieceSetId,
                        onSelectPieceSet = onSelectPieceSet,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val coachRequest = CoachRequest(
            fen = currentPosition?.fen.orEmpty(),
            question = "",
            playedMove = currentMove?.moveSan.orEmpty(),
            engineEvidence = currentPosition?.lines.orEmpty().take(5).mapNotNull { line ->
                val uci = line.movesUci.firstOrNull() ?: return@mapNotNull null
                val san = line.movesSan.firstOrNull() ?: return@mapNotNull null
                EngineEvidenceItem(
                    uci = uci,
                    san = san,
                    scoreCp = line.whiteCentipawns,
                    mateIn = line.whiteMateIn,
                    depth = line.depth,
                    pvSan = line.movesSan.take(12),
                    pvUci = line.movesUci.take(12),
                )
            },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EngineLinesPanel(position = currentPosition, lineLimit = 2)
                HudPanel(
                    modifier = Modifier.fillMaxWidth().clickable { onAskCoach(coachRequest, currentPly) },
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("COACH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Ask about this position", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        currentMove?.let {
                            Text("${it.moveSan ?: "Move"} · ${it.quality.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (review != null && progress?.isAnalyzing == false && review.positions.none { it.lines.isNotEmpty() }) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Move replay is ready. Stockfish analysis isn't included in this build yet.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EvaluationTrajectoryPanel(ply: Int, bestLine: ReviewCandidateLine?) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val trajectoryColor = MaterialTheme.colorScheme.primary
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("EVALUATION TRAJECTORY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("PLY ${ply.coerceAtLeast(0)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                val mid = size.height / 2f
                val stroke = 1.dp.toPx()
                drawLine(outlineColor, androidx.compose.ui.geometry.Offset(0f, mid), androidx.compose.ui.geometry.Offset(size.width, mid), strokeWidth = stroke)
                val offset = ((bestLine?.whiteCentipawns ?: 0) / 900f).coerceIn(-.32f, .32f) * size.height
                val points = listOf(.04f to .1f, .22f to -.08f, .43f to .13f, .66f to -.02f, .96f to .0f)
                for (index in 0 until points.lastIndex) {
                    val a = points[index]
                    val b = points[index + 1]
                    drawLine(
                        color = trajectoryColor,
                        start = androidx.compose.ui.geometry.Offset(size.width * a.first, mid + a.second * size.height + offset),
                        end = androidx.compose.ui.geometry.Offset(size.width * b.first, mid + b.second * size.height + offset),
                        strokeWidth = stroke,
                    )
                }
            }
            Text(bestLine?.let(::formatWhiteRelativeScore) ?: "Evaluation pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReplayControl(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 28.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ReviewReadout(
    ply: Int,
    position: PositionReview?,
    bestLine: ReviewCandidateLine?,
) {
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ReadoutCell(
                label = "POSITION",
                value = "PLY ${ply.coerceAtLeast(0)}",
                modifier = Modifier.weight(1f),
            )
            ReadoutCell(
                label = "EVALUATION",
                value = bestLine?.let(::formatWhiteRelativeScore) ?: "—",
                modifier = Modifier.weight(1f),
            )
            ReadoutCell(
                label = "ENGINE",
                value = position?.engineVersion?.let { "SF $it" } ?: "LOCAL",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReadoutCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun analysisProgressFraction(processedPositions: Int, totalPositions: Int): Float {
    if (totalPositions <= 0) return 0f
    return (processedPositions.toFloat() / totalPositions.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun EngineLinesPanel(position: PositionReview?, lineLimit: Int = 5) {
    val lines = position?.lines.orEmpty().take(lineLimit)
    val depth = lines.maxOfOrNull(ReviewCandidateLine::depth)
    val engineLabel = listOfNotNull(
        position?.engineId?.takeIf(String::isNotBlank),
        position?.engineVersion?.takeIf(String::isNotBlank)?.let { "v$it" },
    ).joinToString(" ")

    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ENGINE LINES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = "White-relative evaluation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    position?.isComplete == false -> Text(
                        text = "ANALYZING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    lines.isNotEmpty() -> Text(
                        text = "${lines.size} / 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (lines.isEmpty()) {
                Text(
                    text = when {
                        position == null -> "Candidate continuations will appear for the selected position."
                        !position.isAnalysisAvailable -> "Analysis isn't available for this position."
                        position.isComplete -> "No candidate lines were recorded for this position."
                        else -> "Stockfish is calculating candidate continuations."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                lines.forEachIndexed { index, line ->
                    CandidateLineRow(
                        rank = index + 1,
                        line = line,
                        isTopLine = index == 0,
                    )
                    if (index < lines.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    }
                }

                val engineDetails = listOfNotNull(
                    engineLabel.takeIf(String::isNotBlank),
                    depth?.let { "Depth $it" },
                ).joinToString(" · ")
                if (engineDetails.isNotBlank()) {
                    Text(
                        text = engineDetails,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (position?.isComplete == false) {
                    Text(
                        text = "Lines shown so far may change while analysis continues.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateLineRow(
    rank: Int,
    line: ReviewCandidateLine,
    isTopLine: Boolean,
) {
    val moves = line.movesSan.take(8).joinToString(" ").ifBlank { "Continuation unavailable" }
    val displayMoves = if (line.movesSan.size > 8) "$moves …" else moves

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = if (isTopLine) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTopLine) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = displayMoves,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatWhiteRelativeScore(line),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "D${line.depth}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatWhiteRelativeScore(line: ReviewCandidateLine): String = when {
    line.whiteMateIn != null -> when {
        line.whiteMateIn > 0 -> "+M${line.whiteMateIn}"
        line.whiteMateIn < 0 -> "−M${kotlin.math.abs(line.whiteMateIn.toLong())}"
        else -> "M0"
    }
    line.whiteCentipawns != null -> {
        val score = line.whiteCentipawns / 100.0
        val formatted = String.format(java.util.Locale.ROOT, "%.2f", score).replaceFirst("-", "−")
        if (score > 0) "+$formatted" else formatted
    }
    else -> "—"
}

@Composable
private fun PieceSetSelector(
    selectedPieceSetId: String,
    onSelectPieceSet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    val selectedName = ChessPieceSetCatalog.find(selectedPieceSetId)?.name
        ?: "Chessnut"

    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        TextButton(onClick = { expanded = true }) {
            Text("Pieces: $selectedName")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ChessPieceSetCatalog.all.forEach { set ->
                DropdownMenuItem(
                    text = { Text(set.name) },
                    onClick = {
                        onSelectPieceSet(set.id)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Piece art credits") },
                onClick = {
                    expanded = false
                    showCredits = true
                },
            )
        }
    }

    if (showCredits) {
        AlertDialog(
            onDismissRequest = { showCredits = false },
            title = { Text("Piece art credits") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ChessPieceSetCatalog.all.forEach { set ->
                        Text(set.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${set.author} - ${set.license}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCredits = false }) { Text("Close") }
            },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ReviewContentPreview() {
    val sampleState = ReviewScreenState(
        gameId = "123",
        currentPly = 1,
        isFlipped = false,
        review = com.pro.chess_review.domain.model.GameReview(
            gameId = "123",
            positions = listOf(
                com.pro.chess_review.domain.model.PositionReview(
                    plyIndex = 0,
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                    lines = listOf(),
                    engineId = "Stockfish",
                    engineVersion = "19",
                    isAnalysisAvailable = true,
                    isComplete = true
                ),
                com.pro.chess_review.domain.model.PositionReview(
                    plyIndex = 1,
                    fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                    lines = listOf(
                        com.pro.chess_review.domain.model.ReviewCandidateLine(
                            movesUci = listOf("e7e5"),
                            movesSan = listOf("e5"),
                            whiteCentipawns = -15,
                            whiteMateIn = null,
                            depth = 18
                        )
                    ),
                    engineId = "Stockfish",
                    engineVersion = "19",
                    isAnalysisAvailable = true,
                    isComplete = true
                )
            ),
            moves = listOf(
                com.pro.chess_review.domain.model.MoveAssessment(
                    plyIndex = 0,
                    mover = com.pro.chess_review.domain.model.ChessSide.WHITE,
                    moveSan = "e4",
                    moveUci = "e2e4",
                    bestMoveUci = "e2e4",
                    quality = MoveQuality.TOP_ENGINE_CHOICE,
                    reason = com.pro.chess_review.domain.model.MoveReviewReason.MATCHED_TOP_LINE,
                    thresholdVersion = "v1",
                    centipawnLoss = 0,
                    beforeWhiteCentipawns = 25,
                    afterWhiteCentipawns = 25,
                    beforeMateIn = null,
                    afterMateIn = null,
                    analysisDepth = 18,
                    hasEngineEvidence = true,
                    analysisComplete = true,
                    isKeyMoment = false
                )
            ),
            summary = com.pro.chess_review.domain.model.GameReviewSummary(
                totalPlies = 10,
                analyzedPlies = 10,
                analysisCoveragePercent = 100,
                averageCentipawnLoss = 15,
                qualityCounts = mapOf(),
                keyMomentPlies = listOf()
            ),
            formulaVersion = "v1"
        )
    )
    ReviewContent(
        state = sampleState,
        onNextMove = {},
        onPrevMove = {},
        onFlipBoard = {}
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun ReviewContentPreviewTablet() {
    ReviewContentPreview()
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ReviewContentPreviewFlipped() {
    val sampleState = ReviewScreenState(
        gameId = "123",
        currentPly = 1,
        isFlipped = true,
        review = com.pro.chess_review.domain.model.GameReview(
            gameId = "123",
            positions = listOf(
                com.pro.chess_review.domain.model.PositionReview(
                    plyIndex = 0,
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                    lines = listOf(),
                    engineId = "Stockfish",
                    engineVersion = "19",
                    isAnalysisAvailable = true,
                    isComplete = true
                ),
                com.pro.chess_review.domain.model.PositionReview(
                    plyIndex = 1,
                    fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                    lines = listOf(),
                    engineId = "Stockfish",
                    engineVersion = "19",
                    isAnalysisAvailable = true,
                    isComplete = true
                )
            ),
            moves = listOf(
                com.pro.chess_review.domain.model.MoveAssessment(
                    plyIndex = 0,
                    mover = com.pro.chess_review.domain.model.ChessSide.WHITE,
                    moveSan = "e4",
                    moveUci = "e2e4",
                    bestMoveUci = "e2e4",
                    quality = MoveQuality.TOP_ENGINE_CHOICE,
                    reason = com.pro.chess_review.domain.model.MoveReviewReason.MATCHED_TOP_LINE,
                    thresholdVersion = "v1",
                    centipawnLoss = 0,
                    beforeWhiteCentipawns = 25,
                    afterWhiteCentipawns = 25,
                    beforeMateIn = null,
                    afterMateIn = null,
                    analysisDepth = 18,
                    hasEngineEvidence = true,
                    analysisComplete = true,
                    isKeyMoment = false
                )
            ),
            summary = com.pro.chess_review.domain.model.GameReviewSummary(
                totalPlies = 10,
                analyzedPlies = 10,
                analysisCoveragePercent = 100,
                averageCentipawnLoss = 15,
                qualityCounts = mapOf(),
                keyMomentPlies = listOf()
            ),
            formulaVersion = "v1"
        )
    )
    ReviewContent(
        state = sampleState,
        onNextMove = {},
        onPrevMove = {},
        onFlipBoard = {}
    )
}


