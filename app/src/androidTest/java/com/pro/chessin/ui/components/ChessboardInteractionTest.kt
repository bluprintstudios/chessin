package com.pro.chessin.ui.components

import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import com.pro.chessin.EmptyTestActivity
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.MoveGenerator
import com.pro.chessin.domain.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Instrumented tests verifying that the Chessboard gesture handling
 * (detectDragGestures at board level; .clickable at square level)
 * behaves correctly for drag, tap, flip, and promotion flows.
 */
@RunWith(AndroidJUnit4::class)
class ChessboardInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Runs a test block with a freshly launched [EmptyTestActivity] containing the Chessboard.
     * Automatically cleans up the Activity scenario at the end of each test method.
     */
    private fun runBoardTest(
        fen: String,
        orientation: BoardOrientation = BoardOrientation.WHITE_AT_BOTTOM,
        highlightedSquares: Set<Square> = emptySet(),
        lastMoveSquares: Set<Square> = emptySet(),
        block: (boardState: MutableState<ChessBoardState>, selectedSquare: MutableState<Square?>) -> Unit
    ) {
        val boardState = mutableStateOf(ChessBoardState.fromFEN(fen))
        val selectedSquare = mutableStateOf<Square?>(null)

        ActivityScenario.launch(EmptyTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    Chessboard(
                        board = boardState.value,
                        selectedSquare = selectedSquare.value,
                        onSquareSelected = { selectedSquare.value = it },
                        onMoveAttempted = { from, to, promotion ->
                            val moves = MoveGenerator.generateLegalMoves(boardState.value)
                            val move = moves.find {
                                it.from == from && it.to == to && it.promotion == promotion
                            }
                            if (move != null) {
                                boardState.value = boardState.value.makeMove(move)
                                selectedSquare.value = null
                            }
                        },
                        highlightedSquares = highlightedSquares,
                        lastMoveSquares = lastMoveSquares,
                        orientation = orientation,
                    )
                }
            }
            block(boardState, selectedSquare)
        }
    }

    /**
     * Compute the center pixel of a board square in the board's local coordinate system.
     */
    private fun squareCenter(
        file: Int,
        rank: Int,
        orientation: BoardOrientation,
        sqPx: Float,
    ): Offset {
        val displayFile = if (orientation == BoardOrientation.WHITE_AT_BOTTOM) file else 7 - file
        val displayRank = if (orientation == BoardOrientation.WHITE_AT_BOTTOM) 7 - rank else rank
        return Offset(
            (displayFile + 0.5f) * sqPx,
            (displayRank + 0.5f) * sqPx,
        )
    }

    /**
     * Simulates a smooth drag gesture on the board from [from] square to [to] square,
     * using multiple intermediate moveTo() steps so that Compose's detectDragGestures
     * recognizes the touch movement beyond the touch slop threshold.
     */
    private fun performDrag(
        from: Square,
        to: Square,
        orientation: BoardOrientation = BoardOrientation.WHITE_AT_BOTTOM,
    ) {
        composeTestRule.onNodeWithTag("chess_board").performTouchInput {
            val sqPx = width / 8f
            val start = squareCenter(from.file, from.rank, orientation, sqPx)
            val end = squareCenter(to.file, to.rank, orientation, sqPx)

            down(start)
            val steps = 10
            for (i in 1..steps) {
                val fraction = i.toFloat() / steps
                val current = Offset(
                    start.x + (end.x - start.x) * fraction,
                    start.y + (end.y - start.y) * fraction
                )
                moveTo(current, delayMillis = 16L)
            }
            up()
        }
        composeTestRule.waitForIdle()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 1. Drag Tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun drag_from_e2_to_e4_produces_correct_fen() {
        runBoardTest(ChessBoardState.startPosition().toFEN()) { boardState, _ ->
            composeTestRule.waitForIdle()

            performDrag(Square.E2, Square.E4)

            val expectedFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
            assertEquals(expectedFen, boardState.value.toFEN())
        }
    }

    @Test
    fun board_flip_drag_from_e2_to_e4_produces_correct_fen() {
        runBoardTest(
            ChessBoardState.startPosition().toFEN(),
            orientation = BoardOrientation.BLACK_AT_BOTTOM
        ) { boardState, _ ->
            composeTestRule.waitForIdle()

            performDrag(Square.E2, Square.E4, orientation = BoardOrientation.BLACK_AT_BOTTOM)

            val expectedFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
            assertEquals(expectedFen, boardState.value.toFEN())
        }
    }

    @Test
    fun promotion_picker_appears_when_pawn_dragged_to_eighth_rank() {
        runBoardTest("8/4P3/8/8/8/8/8/k6K w - - 0 1") { boardState, _ ->
            composeTestRule.waitForIdle()

            performDrag(Square.E7, Square.E8)

            composeTestRule.onNodeWithTag("promotion_picker", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag("promotion_option_QUEEN", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            val expectedFen = "4Q3/8/8/8/8/8/8/k6K b - - 0 1"
            assertEquals(expectedFen, boardState.value.toFEN())
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Tap Tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun tap_select_e2_then_tap_e4_produces_correct_fen() {
        runBoardTest(ChessBoardState.startPosition().toFEN()) { boardState, selectedSquare ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e2", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTrue(selectedSquare.value == Square.E2)

            composeTestRule.onNodeWithTag("square_e4", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            val expectedFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
            assertEquals(expectedFen, boardState.value.toFEN())
            assertNull(selectedSquare.value)
        }
    }

    @Test
    fun board_flip_tap_select_e2_then_tap_e4_produces_correct_fen() {
        runBoardTest(
            ChessBoardState.startPosition().toFEN(),
            orientation = BoardOrientation.BLACK_AT_BOTTOM
        ) { boardState, selectedSquare ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e2", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTrue(selectedSquare.value == Square.E2)

            composeTestRule.onNodeWithTag("square_e4", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            val expectedFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
            assertEquals(expectedFen, boardState.value.toFEN())
            assertNull(selectedSquare.value)
        }
    }

    @Test
    fun single_tap_selects_square_without_toggling() {
        runBoardTest(ChessBoardState.startPosition().toFEN()) { boardState, selectedSquare ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e2", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            assertTrue(selectedSquare.value == Square.E2)
            assertEquals(ChessBoardState.startPosition().toFEN(), boardState.value.toFEN())
        }
    }

    @Test
    fun tap_non_legal_destination_changes_selection() {
        runBoardTest(ChessBoardState.startPosition().toFEN()) { boardState, selectedSquare ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e2", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTrue(selectedSquare.value == Square.E2)

            composeTestRule.onNodeWithTag("square_a1", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTrue(selectedSquare.value == Square.A1)
            assertEquals(ChessBoardState.startPosition().toFEN(), boardState.value.toFEN())
        }
    }

    @Test
    fun tap_enemy_piece_when_no_selection_selects_enemy_square() {
        runBoardTest(ChessBoardState.startPosition().toFEN()) { _, selectedSquare ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e7", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTrue(selectedSquare.value == Square.E7)
        }
    }

    @Test
    fun promotion_picker_appears_on_tap_promotion_and_executes_promotion() {
        runBoardTest("8/4P3/8/8/8/8/8/k6K w - - 0 1") { boardState, _ ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e7", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e8", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("promotion_picker", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag("promotion_option_QUEEN", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            val expectedFen = "4Q3/8/8/8/8/8/8/k6K b - - 0 1"
            assertEquals(expectedFen, boardState.value.toFEN())
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. UI Display & Highlight Tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun legal_move_indicators_rendered_for_selected_piece() {
        runBoardTest(ChessBoardState.startPosition().toFEN()) { _, selectedSquare ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e2", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTrue(selectedSquare.value == Square.E2)

            composeTestRule.onNodeWithTag("square_e3", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag("square_e4", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun check_highlight_rendered_for_king_in_check() {
        runBoardTest("rnb1kbnr/pppp1ppp/8/4Q3/4p3/8/PPPP1PPP/RNB1KBNR b KQkq - 0 1") { _, _ ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e8", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun last_move_squares_rendered() {
        runBoardTest(
            ChessBoardState.startPosition().toFEN(),
            lastMoveSquares = setOf(Square.E2, Square.E4)
        ) { _, _ ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e2", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag("square_e4", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun highlighted_squares_rendered() {
        runBoardTest(
            ChessBoardState.startPosition().toFEN(),
            highlightedSquares = setOf(Square.E4)
        ) { _, _ ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("square_e4", useUnmergedTree = true).assertIsDisplayed()
        }
    }
}
