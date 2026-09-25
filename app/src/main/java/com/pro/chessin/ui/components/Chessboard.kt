package com.pro.chessin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color as ChessColor
import com.pro.chessin.domain.chess.Move
import com.pro.chessin.domain.chess.Piece
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.Square
import com.pro.chessin.domain.chess.MoveGenerator

// ─── Design system constants ───────────────────────────────────────────────

/** Standard chess-board square size per AGENTS.md design system. */
val ChessSquareSize: Dp = 48.dp

/** Side length of the full 8×8 board. */
val ChessBoardSize: Dp = ChessSquareSize * 8

// ─── UI colors ───────────────────────────────────────────────────────────────

private val DarkSquareColor = Color(0xFF191E2B)
private val LightSquareColor = Color(0xFF9F9F9F)

private val SelectedOverlay = Color(0x554A90D9)
private val LastMoveOverlay = Color(0x33FFD700)
private val CheckOverlay = Color(0x44FF4444)
private val HighlightOverlay = Color(0x224A90D9)

private val LegalMoveIndicatorColor = Color(0x884A90D9)

private val PieceWhiteTint = Color(0xFFE8E8E8)
private val PieceBlackTint = Color(0xFF1A1A1A)

// ─── Board orientation ─────────────────────────────────────────────────────

/**
 * Board orientation for display.
 */
enum class BoardOrientation {
    /** White pieces at the bottom of the board (standard orientation). */
    WHITE_AT_BOTTOM,

    /** Black pieces at the bottom of the board (flipped view). */
    BLACK_AT_BOTTOM
}

// ─── Pending promotion ───────────────────────────────────────────────────────

private data class PendingPromotion(
    val from: Square,
    val to: Square
)

// ─── Public API ─────────────────────────────────────────────────────────────

/**
 * A stateless, purely-presentational chess board composable.
 *
 * The board renders from [board] (a [ChessBoardState]) and reports user intent
 * via [onSquareSelected] and [onMoveAttempted]. Selection and the board position
 * are **owned by the caller** — the composable never mutates them internally
 * (except for transient drag / promotion-picker UI state).
 *
 * Supports two input modalities:
 * 1. **Tap-to-move**: tap a piece to select it, tap a destination square to attempt the move.
 * 2. **Drag-to-move**: press and drag a piece to a destination, release to attempt the move.
 *
 * @param board Immutable board position to render.
 * @param selectedSquare Currently selected square (hoisted state).
 * @param onSquareSelected Called when a square is tapped for selection.
 * @param onMoveAttempted Called when the user attempts a move.
 * @param highlightedSquares Extra squares to highlight (e.g. engine hints).
 * @param lastMoveSquares Squares of the last move to highlight.
 * @param orientation Which side is at the bottom of the display.
 * @param modifier Modifier for the board.
 */
@Composable
fun Chessboard(
    board: ChessBoardState,
    selectedSquare: Square?,
    onSquareSelected: (Square) -> Unit,
    onMoveAttempted: (from: Square, to: Square, promotion: PieceType?) -> Unit,
    highlightedSquares: Set<Square> = emptySet(),
    lastMoveSquares: Set<Square> = emptySet(),
    orientation: BoardOrientation = BoardOrientation.WHITE_AT_BOTTOM,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var boardWidthPx by remember { mutableStateOf(0f) }
    val defaultSquareSizePx = with(density) { ChessBoardSize.toPx() / 8f }
    val squareSizePx = if (boardWidthPx > 0f) boardWidthPx / 8f else defaultSquareSizePx

    // ── Computed state (must come before rememberUpdatedState) ────────────
    val legalMoves: List<Move> = if (selectedSquare == null) {
        emptyList()
    } else {
        MoveGenerator.generateLegalMoves(board).filter { it.from == selectedSquare }
    }

    val legalDestinations: Map<Square, Boolean> = legalMoves.associate {
        it.to to it.isCapture
    }

    val kingInCheckSquare: Square? = if (board.isInCheck()) {
        board.findKing(board.sideToMove)
    } else null

    // ── Transient drag state ─────────────────────────────────────────────
    var isDragging by remember { mutableStateOf(false) }
    var dragStartSquare by remember { mutableStateOf<Square?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var draggedPiece by remember { mutableStateOf<Piece?>(null) }

    // ── Promotion picker state ──────────────────────────────────────────
    var pendingPromotion by remember { mutableStateOf<PendingPromotion?>(null) }

    // ── Latest values for use inside pointerInput coroutines ─────────────
    val updatedBoard by rememberUpdatedState(board)
    val updatedLegalMoves by rememberUpdatedState(legalMoves)
    val updatedOnMoveAttempted by rememberUpdatedState(onMoveAttempted)
    val updatedOrientation by rememberUpdatedState(orientation)

    // ── Tap handler for square selection / move attempts ────────────────
    val handleTap: (Square) -> Unit = { square: Square ->
        val sel = selectedSquare
        if (sel != null) {
            val move = legalMoves.find { it.to == square }
            if (move != null) {
                if (move.isPromotion) {
                    pendingPromotion = PendingPromotion(sel, square)
                } else {
                    onMoveAttempted(sel, square, null)
                }
            } else {
                onSquareSelected(square)
            }
        } else {
            onSquareSelected(square)
        }
    }

    // ── Helper function ──────────────────────────────────────────────────

    fun offsetToSquare(offset: Offset): Square? {
        val sqPx = squareSizePx
        if (offset.x < 0f || offset.y < 0f ||
            offset.x > sqPx * 8f || offset.y > sqPx * 8f
        ) return null

        val displayFile = (offset.x / sqPx).toInt().coerceIn(0, 7)
        val displayRank = (offset.y / sqPx).toInt().coerceIn(0, 7)

        val isWhiteAtBottom = updatedOrientation == BoardOrientation.WHITE_AT_BOTTOM
        val boardFile = if (isWhiteAtBottom) displayFile else 7 - displayFile
        val boardRank = if (isWhiteAtBottom) 7 - displayRank else displayRank

        return Square.fromCoordinates(boardFile, boardRank)
    }

    // ── Board rendering ──────────────────────────────────────────────────
    val displayRanks = if (orientation == BoardOrientation.WHITE_AT_BOTTOM)
        (7 downTo 0) else (0..7)
    val displayFiles = if (orientation == BoardOrientation.WHITE_AT_BOTTOM)
        (0..7) else (7 downTo 0)

    val boardContentDesc = buildString {
        append("Chess board, ")
        append(if (board.sideToMove == ChessColor.WHITE) "white" else "black")
        append(" to move")
        if (kingInCheckSquare != null) append(", check")
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { boardWidthPx = it.width.toFloat() }
            .semantics {
                contentDescription = boardContentDesc
                testTag = "chess_board"
            }
            .pointerInput(Unit) {
                // ── Drag detection only ─────────────────────────────────
                // Tap handling is done by each square's clickable modifier,
                // avoiding double-tap handling between board-level and
                // square-level gesture detectors.
                detectDragGestures(
                    onDragStart = { startPosition ->
                        val square = offsetToSquare(startPosition)
                        val piece = square?.let { updatedBoard.getPiece(it) }
                        if (square != null && piece != null &&
                            piece.color == updatedBoard.sideToMove
                        ) {
                            isDragging = true
                            dragStartSquare = square
                            draggedPiece = piece
                            dragPosition = startPosition
                        }
                    },
                    onDrag = { change, _ ->
                        if (isDragging) {
                            dragPosition = change.position
                        }
                    },
                    onDragEnd = {
                        val from = dragStartSquare
                        if (from != null) {
                            val target = offsetToSquare(dragPosition)
                            if (target != null && target != from) {
                                // Compute legal moves for the drag start square on-demand
                                val dragLegalMoves = MoveGenerator.generateLegalMoves(updatedBoard)
                                    .filter { it.from == from }
                                val move = dragLegalMoves.find {
                                    it.from == from && it.to == target
                                }
                                if (move != null) {
                                    if (move.isPromotion) {
                                        pendingPromotion = PendingPromotion(from, target)
                                    } else {
                                        updatedOnMoveAttempted(from, target, null)
                                    }
                                }
                            }
                        }
                        isDragging = false
                        dragStartSquare = null
                        draggedPiece = null
                        dragPosition = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragStartSquare = null
                        draggedPiece = null
                        dragPosition = Offset.Zero
                    }
                )
            }
    ) {
        // ── Render squares ───────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            displayRanks.forEach { boardRank ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    displayFiles.forEach { boardFile ->
                        val square = Square.fromCoordinates(boardFile, boardRank)!!
                        val piece = board.getPiece(square)
                        val isLight = (square.file + square.rank) % 2 == 1
                        val isChecked = square == kingInCheckSquare
                        val isLastMove = lastMoveSquares.contains(square)
                        val isSelected = selectedSquare == square
                        val isHighlighted = highlightedSquares.contains(square)
                        val isLegalDest = legalDestinations.containsKey(square)
                        val isCapture = legalDestinations[square] == true

                        ChessSquare(
                            square = square,
                            piece = piece,
                            isLight = isLight,
                            isSelected = isSelected,
                            isLastMove = isLastMove,
                            isInCheck = isChecked,
                            isHighlighted = isHighlighted,
                            hasLegalMove = isLegalDest,
                            isLegalCapture = isCapture,
                            onClick = { handleTap(square) },
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        )
                    }
                }
            }
        }

        // ── Drag overlay (piece following finger) ────────────────────────
        if (isDragging && draggedPiece != null) {
            val piece = draggedPiece!!
            val tint = if (piece.color == ChessColor.WHITE) PieceWhiteTint else PieceBlackTint
            val dragSizeDp = with(density) { (squareSizePx * 0.75f).toDp() }

            Box(
                modifier = Modifier
                    .size(dragSizeDp)
                    .offset {
                        IntOffset(
                            (dragPosition.x - squareSizePx * 0.375f).roundToInt(),
                            (dragPosition.y - squareSizePx * 0.375f).roundToInt()
                        )
                    }
            ) {
                Image(
                    painter = piecePainter(piece.type),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(tint),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Promotion picker ─────────────────────────────────────────────
        val pending = pendingPromotion
        if (pending != null) {
            PromotionPicker(
                pieceColor = board.getPiece(pending.from)?.color ?: board.sideToMove,
                onPieceSelected = { pieceType ->
                    onMoveAttempted(pending.from, pending.to, pieceType)
                    pendingPromotion = null
                },
                onDismiss = { pendingPromotion = null },
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { testTag = "promotion_picker" }
            )
        }
    }
}

// ─── ChessSquare (internal) ──────────────────────────────────────────────────

/**
 * A single chess square. Pure function of its parameters so Compose can
 * skip recomposition when nothing on a given square changes.
 */
@Composable
private fun ChessSquare(
    square: Square,
    piece: Piece?,
    isLight: Boolean,
    isSelected: Boolean,
    isLastMove: Boolean,
    isInCheck: Boolean,
    isHighlighted: Boolean,
    hasLegalMove: Boolean,
    isLegalCapture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = if (isLight) LightSquareColor else DarkSquareColor

    val overlayColor: Color? = when {
        isInCheck -> CheckOverlay
        isLastMove -> LastMoveOverlay
        isSelected -> SelectedOverlay
        isHighlighted -> HighlightOverlay
        else -> null
    }

    val pieceTint = if (piece?.color == ChessColor.WHITE) PieceWhiteTint else PieceBlackTint

    val contentDescriptionText = buildString {
        append(square.name)
        if (piece != null) {
            val colorName = if (piece.color == ChessColor.WHITE) "White" else "Black"
            append(", $colorName ${piece.type.name.lowercase()}")
        } else {
            append(", empty")
        }
    }

    Box(
        modifier = modifier
            .background(baseColor)
            .drawSquareHighlight(overlayColor)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = contentDescriptionText
                testTag = "square_${square.name}"
            }
    ) {
        // ── Legal move indicator ───────────────────────────────────────
        if (hasLegalMove) {
            if (isLegalCapture) {
                // Hollow ring (capture)
                Canvas(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.55f)
                ) {
                    drawCircle(
                        color = LegalMoveIndicatorColor,
                        radius = size.minDimension / 2f * 0.7f,
                        style = Stroke(width = size.minDimension / 2f * 0.12f)
                    )
                }
            } else {
                // Filled dot (non-capture)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.25f)
                        .background(LegalMoveIndicatorColor, shape = CircleShape)
                )
            }
        }

        // ── Piece ───────────────────────────────────────────────────────
        if (piece != null) {
            Image(
                painter = piecePainter(piece.type),
                contentDescription = null,
                colorFilter = ColorFilter.tint(pieceTint),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.75f)
            )
        }
    }
}

// ─── Helper: draw highlight overlay ──────────────────────────────────────────

private fun Modifier.drawSquareHighlight(overlayColor: Color?): Modifier =
    if (overlayColor != null) {
        this.then(Modifier.background(overlayColor))
    } else {
        this
    }
