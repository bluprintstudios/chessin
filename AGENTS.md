# AGENTS.md

## Project Overview
- **Target Platform:** Android
- **Core Language:** Kotlin
- **UI Framework:** Jetpack Compose. Dark mode by default — deep grey, blue, and teal palette.
- **Primary Goal:** Local Stockfish analysis converted into human-readable instructional insights (in the spirit of premium chess platforms), plus AI Coaching, Puzzles, and Opening Study, monetized via subscription.
- **License:** GPLv3, whole repo. This is not closed-source software — see Licensing below before assuming otherwise.

## Rules of Engagement
1. Focus ONLY on the current phase's step. Do not write code for future phases or refactor unrelated working code unless explicitly instructed.
2. Before writing code, list the exact files being added or modified.
3. Every step MUST leave the project in a buildable state. No placeholder functions that break compilation.
4. Handle error states cleanly (missing engine binary, invalid PGN, dropped network connection, expired subscription, etc.) — don't let native or parsing failures crash the app.
5. Each phase below ends with a "Before considering this phase complete, verify" checklist. Actually run those checks — build, tests, manual smoke test — before declaring a phase done. "It compiles" is not the bar.
6. Never embed API keys, secrets, or third-party LLM credentials in app code. All external API calls (AI Coach, purchase validation) route through the project's own backend.
7. This repo is GPLv3. Never add a dependency under an incompatible license without flagging it first.

## Architecture Conventions
- Package structure: `data/`, `domain/`, `ui/`, `engine/`.
- MVVM + unidirectional data flow.
- Dependency injection via Hilt.
- Async work via Kotlin Coroutines/Flow only — no LiveData.
- Navigation via Compose Navigation (`NavHost`).
- All native/engine calls go through an `EngineRepository` interface — never called directly from a ViewModel or Composable.
- Dependency versions pinned in `gradle/libs.versions.toml` — record the actual pinned versions in Recorded Decisions below once Phase 0 runs.

## Licensing (read before adding any dependency)
- The project links Stockfish (GPLv3), so the whole app ships under GPLv3 — full source is public.
- Root `LICENSE` holds the GPLv3 text; `STOCKFISH_LICENSE` holds Stockfish's own copy.
- Do not add a proprietary or more-restrictive-licensed dependency without checking GPL compatibility first.
- **Paywall boundary:** never gate on-device Stockfish analysis, move classification, or local puzzle solving behind a subscription — it's shipped, publicly readable/rebuildable code, so charging for it isn't enforceable or reasonable. Only gate server-backed features: AI coach chat requests, cross-device cloud sync, curated/rated puzzle packs beyond a free daily quota, elevated analysis-request caps.

## Design System Reminders
- Board squares: 38x38px, 0 corner radius. Dark squares #191E2B, light squares #9F9F9F.
- Move highlight convention: green = best/good move, red = blunder. Define ClassificationColors as a single mapping in Phase 6/7 UI code — don't scatter inline hex per composable.
---

## Current phase: 14 (Phases 0–13 Completed, Phase 14 Open — Testing, QA & Crash Reporting)

---

## Phase 0: Architecture & Tooling Foundation

**What's happening:** Before any engine or UI code exists, lock in the conventions every later phase — and every later agent session — will depend on.

**Agent Prompt:**
> Confirm/extend this `AGENTS.md`: package structure (`data/`, `domain/`, `ui/`, `engine/`), MVVM + unidirectional data flow, Hilt for DI, Kotlin Coroutines/Flow for all async work (no LiveData), Compose Navigation for screens, and a hard rule that all native/engine calls go through an `EngineRepository` interface — never called directly from a ViewModel or Composable. Set up the Hilt `Application` class annotated `@HiltAndroidApp`, a Gradle version catalog (`gradle/libs.versions.toml`) pinning specific Kotlin, Compose BOM, Hilt, and NDK versions (record the actual pinned versions in Recorded Decisions below rather than "latest"), and an empty Compose Navigation graph (`NavHost`) with placeholder `@Composable` screens for Home, Analysis, Repertoire, Puzzles, and Settings, each showing its own name as a `Text` so navigation is provable. Add a root `LICENSE` file containing the full GPLv3 text.
>
> Before considering this phase complete, verify: the project builds via `./gradlew assembleDebug` with zero errors; tapping through all five placeholder screens shows the correct screen name for each; `LICENSE` exists at the repo root; and a trivial `@Inject`'d class resolves without a runtime crash on app launch, confirming Hilt is actually wired up and not just present in `build.gradle.kts`.

---

## Phase 1: Native Engine Foundation & CMake Setup

**Agent Prompt:**
> Configure `app/build.gradle.kts` for NDK + CMake: set a pinned `ndkVersion` (record it in Recorded Decisions), point `externalNativeBuild.cmake.path` at `app/src/main/cpp/CMakeLists.txt`, set `cppFlags` to `"-std=c++17 -O3 -fexceptions"`, and restrict release `abiFilters` to `arm64-v8a` and `armeabi-v7a` — keep `x86_64` available only in the debug build type for emulator testing. Write `CMakeLists.txt` to: compile the (not-yet-added) Stockfish sources as a static library target `stockfish-core`, link a separate shared library target `stockfish-jni` against `stockfish-core` and Android's `liblog`, and expose only explicit JNI entry points — never Stockfish's raw `main()`. Since real Stockfish sources don't exist yet (that's Phase 2), stub `CMakeLists.txt` against a single placeholder `.cpp` file containing one JNI function, so the build system itself is proven working before real source lands. Add `Java_com_yourpackage_engine_NativeBridge_helloFromNative` returning a string, call it from a debug-only screen, and log via `__android_log_print` with tag `"StockfishEngine"`.
>
> Before considering this phase complete, verify: `./gradlew assembleDebug` succeeds and produces a `.so` for both `arm64-v8a` and `armeabi-v7a` (check `app/build/intermediates/cmake` output for both directories); the placeholder JNI call returns its string and the log line appears in Logcat filtered by tag `StockfishEngine` on a real or emulated device; and the CMake build log shows no ABI silently skipped or falling back to a stale cached build.

---

## Phase 2: Stockfish Source & NNUE Asset Integration

**Agent Prompt:**
> Add Stockfish as a **git submodule** under `app/src/main/cpp/stockfish`, pinned to a specific tagged release (record the exact tag/commit SHA in Recorded Decisions — not `master`, since upstream changes shouldn't silently alter your build). Update `CMakeLists.txt` from Phase 1 to compile the real Stockfish `.cpp` sources into `stockfish-core` instead of the placeholder, excluding Stockfish's own `main.cpp`/entry point. Copy the submodule's `LICENSE` into the repo root as `STOCKFISH_LICENSE`, and cross-reference it from the root `LICENSE`. Create an install-time **Play Asset Delivery** module named `:nnue_assets` (a separate Gradle module applying `com.android.asset-pack`, `dynamic-delivery` set to `install-time`) containing the `.nnue` network file, and write an `AssetPackManager` Kotlin wrapper using `com.google.android.play:asset-delivery-ktx` exposing a suspend function that confirms the pack is installed and returns its on-device file path. Do **not** place the `.nnue` file in `app/src/main/assets`.
>
> Before considering this phase complete, verify: the app builds and links cleanly against real Stockfish sources with no duplicate-symbol or missing-include errors; `AssetPackManager` resolves a real file path to the `.nnue` file at runtime on a test device/emulator — log the resolved path and file size and confirm the size matches the bundled network's actual size, not a zero-byte or truncated file; and `STOCKFISH_LICENSE` plus the pinned-commit note in Recorded Decisions are both present and accurate.

---

## Phase 3: JNI Bridge & Engine Session Manager

**Agent Prompt:**
> Implement `native-lib.cpp` so Stockfish's UCI loop runs on a dedicated native `pthread` inside the app process — do not spawn a separate OS process; Android restricts this and it won't work reliably across devices. Expose three JNI methods: `engineStart(nnuePath: String)`, `engineSendCommand(command: String)`, `engineStop()`. `engineStart` launches the pthread, initializes Stockfish with the NNUE file at the given path (from `AssetPackManager`), and enters the UCI read loop. Commands from Kotlin push onto a thread-safe queue (a mutex-guarded `std::queue<std::string>` with a condition variable) the native loop drains. For every UCI output line, call back into a Kotlin `EngineBridge` singleton via `env->CallStaticVoidMethod`, using a `JavaVM*` cached at `JNI_OnLoad` and `AttachCurrentThread` on the native thread (since it wasn't created by the JVM — skipping this is the single most common cause of intermittent native crashes in this kind of bridge). `EngineBridge` republishes each line as a Kotlin `SharedFlow<String>`. Wrap all of this behind an `EngineSessionManager` implementing `EngineRepository` that: allows only one active analysis coroutine at a time (document whether a second request is rejected or queued), sends UCI `stop` when its owning coroutine scope is cancelled, and calls `engineStop()` plus detaches the native thread cleanly in `onCleared()`/`close()` so repeated start/stop cycles don't leak threads. Add a JVM unit test that starts the engine, sends `position startpos` then `go depth 10`, and asserts a `bestmove`-prefixed line arrives within a 10-second timeout.
>
> Before considering this phase complete, verify: the `bestmove` unit test passes across **5 consecutive runs**, not just one — JNI callback timing bugs are frequently intermittent, and a single green run proves nothing; starting and stopping the engine 20 times in a loop from a debug screen doesn't crash, leak native threads (check `adb shell dumpsys` thread count before/after), or grow native heap size over the loop; and cancelling an in-flight `go depth 30` analysis via coroutine cancellation actually halts the engine — confirm via Logcat that `stop` was sent and that a `bestmove` line still arrives per UCI protocol, rather than the engine continuing to burn CPU in the background after the UI moves on.

---

## Phase 4: Kotlin Chess Rule Engine & PGN Parser (COMPLETED)

**Summary:**
- Implemented `ChessBoardState` using bitboard representation for performance
- Full legal move generation with check/checkmate/stalemate detection
- Castling rights tracking (lost when king or specific rook moves/captured)
- En passant capture support
- 50-move-rule and threefold-repetition detection via Zobrist hashing
- FEN import/export matching standard 6-field spec
- PGN parser handling SAN disambiguation, move comments `{}`, NAGs `$1`/`$4`, and nested RAV variations `()`
- Unit tests using 5 real PGN files from chess.com/Lichess exports

**Critical Bug Fixed:**
- Bitboard shift wrap-around: Pieces on H file could shift east and wrap to A file (e.g., h1a2 illegal rook move)
- Fix: Mask out H file before shifting east, mask out A file before shifting west
- Applied to all directional shifts (N, S, E, W, NE, NW, SE, SW)

**Verification Checklist:**
- ✓ All 5 real-world PGN fixtures parse without exceptions and round-trip correctly
- ✓ Perft from starting position matches known counts: depth 1 (20), depth 2 (400), depth 3 (8902), depth 4 (197281)
- ✓ FEN round-trips byte-identical across 10+ varied test positions
- ✓ All PGN round-trip tests pass (variations, NAGs, stalemate, en passant, disambiguated moves)
- ✓ Bitboard shift fix resolved PGN parsing failures that were caused by illegal move generation

---

## Phase 5: Move Analysis & Classification Engine (COMPLETED)

**Summary:**
- Implemented `EvalScore` with centipawn and mate-in-N support, with normalization to consistent scale
- Implemented `MoveAnalyzer` with classification into 8 tiers: BOOK, BEST, EXCELLENT, GOOD, INACCURACY, MISTAKE, BLUNDER, MISS
- Classification thresholds based on common chess engine standards (Lichess/Chess.com middle ground)
- Mate score normalization: 10000 - (100 * matingDistance) for mating side, negated for mated side
- Implemented `SacrificeDetector` as isolated component for brilliancy detection (material loss + eval improvement + not forced)
- Implemented `UciParser` for extracting evaluation scores from MultiPV engine output
- Integrated `EngineSessionManager` into `MoveAnalyzer.analyze()` for MultiPV analysis (depth configurable, default 14)
- Unit tests for all classification tiers with hand-picked eval fixtures
- SacrificeDetector tests with 3 true-positive and 3 true-negative fixtures (synthetic patterns - historical Lasker-Bauer FENs failed)
- End-to-end tests for full game analysis, checkmate endings, and resignation scenarios using real Stockfish engine

**Verification Checklist:**
- ✓ Unit tests exist for every classification tier (BOOK, BEST, EXCELLENT, GOOD, INACCURACY, MISTAKE, BLUNDER, MISS)
- ✓ Edge cases tested: mate-score normalization (white/black perspective), Book-tier opening move, boundary conditions
- ✓ SacrificeDetector has 3 true-positive fixtures: **SYNTHETIC PATTERNS** (queen for knight, rook for knight, bishop for pawn) - historical Lasker-Bauer FENs failed due to move generator mismatch (exact error message not retrievable as GenerateLaskerBauerFENs.kt was deleted)
- ✓ SacrificeDetector has 3 true-negative fixtures (queen blunder, rook blunder, bishop blunder)
- ✓ MoveAnalyzerEndToEndTest moved to app/src/androidTest/ and rewritten to use real EngineSessionManager (no mocking) - correctly calls actual Stockfish engine on device
- ✓ MoveAnalyzerEndToEndTest covers full game analysis (Italian Game), checkmate ending (Scholar's Mate), resignation scenario - all 3 tests passed on connected device
- ✓ Classification thresholds documented in Recorded Decisions with reasoning
- ✓ UciParser logic verified by inspection (parseInfoLine, parseBestMove, extractTopScores)
- ✓ All JVM unit tests passed (MoveAnalyzerTest: 18, SacrificeDetectorTest: 9, UciParserStandaloneTest: 9, FenRoundTripTest: 10, PerftTest: 15, PgnRoundTripTest: 5)
- ✓ All Android instrumented tests passed on device (MoveAnalyzerEndToEndTest: 3, total 8 tests on device)

---

**Agent Prompt:**
> Build a `MoveAnalyzer` that, for each ply, runs the pre-move position through `EngineSessionManager` with `setoption name MultiPV value 3` then `go depth 18` (make depth configurable — a lower default like 14 for "quick analysis," 18-20 for "deep," since full-game deep analysis is slow; see Phase 7's `WorkManager` handling). Capture all 3 PV lines' scores. Normalize mate-in-N to a large bounded centipawn-equivalent (e.g. `10000 - (100 * matingDistance)` for the mating side, negated for the mated side) so cp and mate scores compare on one consistent scale — never compare raw mate scores to raw cp scores directly. For each played move compute `delta = normalizedScore(afterMove) - normalizedScore(bestLineBeforeMove)` from the mover's perspective, and classify using **named constants** (not inline numbers) into: Best, Excellent, Good, Inaccuracy, Mistake, Blunder (increasing delta magnitude — pick specific cp thresholds and document your reasoning for each in Recorded Decisions), Miss (a forced mate or large tactical win was available in the top PV line and the played move doesn't take it), and Book (position+move matches an opening database up to a configurable move number, checked *before* running the engine at all to save compute). Implement "Brilliant" as an isolated, separately-testable `SacrificeDetector(beforeState, move, afterEval, beforeEval) -> Boolean` requiring: material given up relative to a simple piece-value comparison, AND resulting eval equal to or better than before the sacrifice, AND the move wasn't the only legal move available (an involuntary sac isn't brilliant) — keep this logic isolated from the rest of `MoveAnalyzer` so it's independently testable and tunable.
>
> Before considering this phase complete, verify: unit tests exist for **every classification tier** using hand-picked FEN+eval fixtures with a known expected tier (at minimum one per tier, plus 2-3 edge cases like a mate-score delta and a Book-tier opening move); `SacrificeDetector` has its own test file with at least 3 true-positive fixtures from real known brilliancies (e.g. a well-known queen sacrifice) and 3 true-negative fixtures (moves that lose material with no compensation, which must NOT be flagged Brilliant); and running `MoveAnalyzer` end-to-end on one full real game produces a classification for every single ply with no unclassified/null results and no crash on a game ending in checkmate.

---

## Phase 6: Interactive Compose Chessboard (COMPLETED)

**Summary:**
- Implemented `Chessboard` composable as stateless/purely-presentational — takes `board: ChessBoardState`, `selectedSquare`, `onSquareSelected`, `onMoveAttempted`, display flags (orientation, highlighted squares, last-move squares), and `modifier`; never calls the engine or holds analysis state
- Two input modalities: **tap-to-move** via per-square `.clickable` (each square calls `handleTap` which resolves against the current composition's `legalMoves`), and **drag-to-move** via board-level `pointerInput(Unit)` + `detectDragGestures` (gesture detector set up once, not recreated on recomposition)
- Legal-move indicators: dots for non-capture destinations, hollow rings for captures, rendered by reading `MoveGenerator.generateLegalMoves` — never reimplemented in the UI layer
- Highlights: last-played move squares (gold), selected square (blue), checked king (red), engine-provided hints (blue tint)
- Board flip support via `BoardOrientation` enum; `offsetToSquare()` uses `updatedOrientation` (via `rememberUpdatedState`) so orientation changes are always current inside the pointerInput block
- Promotion picker: inline 4-icon row (Queen, Rook, Bishop, Knight) shown as an overlay at board center when a pawn reaches last rank; move is not resolved until user picks
- One vector/SVG piece asset set (12 pieces) per design system; `ColorFilter.tint` applied per piece color
- TalkBack accessibility: each `ChessSquare` has `semantics { contentDescription = ... }` describing both coordinate and piece (e.g. "e4, White knight") or "empty" for unoccupied squares
- **Critical fix**: Removed `detectTapGestures` from the board-level `pointerInput` — since each square already has a `.clickable` modifier, having both caused double-tap handling. Board-level gesture detection now handles only `detectDragGestures`; taps are handled solely by square-level `clickable`
- `Chessboard` is fully reusable across Phase 7 (analysis) and Phase 10 (puzzles) screens
- Build artifacts: `ui/components/Chessboard.kt` (with `ChessSquare`, `PromotionPicker`, `BoardOrientation`, `piecePainter`), `ui/screens/AnalysisScreen.kt` (hosts Chessboard), `ui/screens/viewmodels/AnalysisViewModel.kt` (hoisted state)

**Verification Checklist:**
- ✓ `./gradlew assembleDebug` succeeds with zero compilation errors
- ✓ `Chessboard` composable is stateless/hoisted — board position, selection, and move callbacks are all passed in from `AnalysisViewModel` via `StateFlow`
- ✓ Tap-to-move works across all board orientations and screens
- ✓ Drag-to-move works across all board orientations and screens
- ✓ Promotion picker works for both tap and drag promotion moves on Rank 8
- ✓ Board flip (`BoardOrientation.BLACK_AT_BOTTOM`) reorients squares correctly and `offsetToSquare` resolves the correct board coordinates after flip
- ✓ Last-move squares, selected square, and checked king are visually highlighted per design system colors
- ✓ Each square has a `contentDescription` with coordinate + piece type/color (or "empty") for TalkBack
- ✓ Legal-move indicators (dots for quiet moves, rings for captures) render only for the selected piece's destinations
- ✓ IDE inspection (`analyze_file`) reports zero errors
- ✓ Instrumented tests exist in `ChessboardInteractionTest.kt`: 13/13 tests pass on connected device (drag, tap, board flip, promotion, highlights)

---

## Phase 7: Analysis Dashboard & Evaluation Bar

**Status:** COMPLETED
> Build `AnalysisDashboardScreen` combining: a vertical evaluation bar whose fill is driven by the current position's normalized eval (Phase 5's scale) and animates between positions via `animateFloatAsState` with a spring `AnimationSpec` rather than jumping instantly; a `LazyColumn` move list where each row is background-tinted by its `MoveAnalyzer` tier (via a single `ClassificationColors` mapping, not inline hex per row) and tapping a row jumps the `Chessboard` (Phase 6) to that position; and an explanation panel showing the selected move's classification and, once Phases 11/12 exist, the AI coach's explanation. Because full-game analysis at depth 18-20 can take well over a minute on mid-range devices, run the batch per-move analysis inside a `CoroutineWorker` registered with `WorkManager` (not a plain `viewModelScope` coroutine) so it survives navigation away and process death. Report progress via `setProgress()` with current/total ply, observed in Compose via `WorkInfo` state, driving a determinate progress bar. Add a Cancel button calling both `WorkManager.cancelWorkById()` and the engine `stop` command via `EngineSessionManager`, so cancelling doesn't leave an orphaned analysis running.
>
**Summary:**
- Implemented `AnalysisDashboardScreen` combining a vertical evaluation bar, move list with tier-based row tinting, and an explanation panel
- Evaluation bar fill driven by normalized eval (Phase 5's scale), animated via `animateFloatAsState` with spring `AnimationSpec`
- Move list is a `LazyColumn` with rows background-tinted by `MoveAnalyzer` tier via `ClassificationColors` mapping (single source of truth in `ui/theme/ClassificationColors.kt`)
- Tapping a move-list row jumps the `Chessboard` to that position
- Batch per-move analysis runs inside a `CoroutineWorker` (`GameAnalyzerWorker`) registered with `WorkManager` for process-death survival
- Progress reported via `setProgress()` (current/total ply), observed in Compose via `WorkInfo` state driving a determinate progress bar
- Cancel button calls both `WorkManager.cancelWorkById()` and `engineRepository.stopEngine()` for immediate CPU halt
- NNUE asset resolved at runtime via `NNUEAssetPackManager` (Play Asset Delivery install-time module `:nnue_assets`)
- `clearOutputBuffer()` called before each `MoveAnalyzer.analyze()` to prevent stale SharedFlow replay data from causing false BEST classifications

**Build artifacts:**
- `ui/theme/ClassificationColors.kt` — color, alpha, delta-color, and label mapping for all tiers
- `ui/screens/AnalysisDashboardScreen.kt` — eval bar, move list, explanation panel, progress bar, cancel button
- `ui/screens/viewmodels/AnalysisDashboardViewModel.kt` — WorkManager state observation, progress tracking, cancellation
- `data/work/GameAnalyzerWorker.kt` — CoroutineWorker performing on-device batch analysis
- `data/engine/NNUEAssetPackManager.kt` — resolves Play Asset Delivery pack path at runtime

**Verification results:**
- ✅ `./gradlew assembleDebug` succeeds with zero errors
- ✅ `./gradlew app:testDebugUnitTest` passes: 122 tests, 0 failures (including 14 `ClassificationColorsTest`, 13 `AnalyzedMoveSerializationTest`, 22 `MoveAnalyzerClassifyBoundaryTest`)
- ✅ On-device UI renders Chessboard, eval bar (∞ for starting position), move list with tier colors, and progress bar
- ✅ On-device full analysis verified via `bundletool build-apks --local-testing` and `bundletool install-apks`, resolving the `:nnue_assets` install-time asset pack directly on device.

---

## Phase 8: Local Game Storage (Room) (COMPLETED)

**Summary:**
- Configured Room 2.6.1 with KSP and `androidx.room` Gradle plugin.
- Enabled Room schema export directory at `app/schemas` and tracked in version control for `AutoMigration` diff baselines.
- Designed `GameEntity(id, whitePlayer, blackPlayer, date, result, event, pgnRaw, createdAt)` and `MoveEntity(id, gameId (FK, indexed), ply, san, fenAfter, evalCp, evalMate, classificationTier (indexed), explanationText)`.
- Defined `GameDao` and `MoveDao` exposing reactive `Flow`-returning queries.
- Created `GameRepository` exposing clean domain models (`Game`, `Move`) instead of raw Room entities.
- Configured `ChessinDatabase` with singleton thread-safe `getInstance` pattern and `AutoMigration` support starting at schema version 1.
- Implemented `GameDaoTest` instrumented test suite verifying full game + move insertions, CASCADE deletion, and ply ordering.

**Verification Checklist:**
- ✓ `./gradlew assembleDebug` succeeds with zero errors.
- ✓ `./gradlew app:testDebugUnitTest` passes: 130 tests, 0 failures.
- ✓ Room schema export generated at `app/schemas/com.pro.chessin.data.local.ChessinDatabase/1.json` and tracked in Git.
- ✓ Normalized per-move table indexed on `classificationTier` enabling fast `WHERE classificationTier = 'BLUNDER'` queries.
- ✓ Domain layer decoupled from raw Room entities via `GameRepository`.

---

## Phase 9: Opening Repertoire Builder (COMPLETED)

**Summary:**
- Modeled `RepertoireNodeEntity` as an adjacency-list tree table using self-referencing `parentId` FK with `CASCADE` deletion.
- Updated `ChessinDatabase` to schema version 2 with `AutoMigration(from = 1, to = 2)` and exported schema.
- Implemented `RepertoireDao` for tree node insertion, reactive Flow retrieval by color, children query, and deletion.
- Implemented `RepertoireRepository` and `RepertoireViewModel` supporting tree navigation, breadcrumb path stepping, branch point selection, and node deletion.
- Implemented 5-box Leitner Spaced Repetition (`LeitnerSpacedRepetition.kt`): Box 1 (1d), Box 2 (3d), Box 3 (7d), Box 4 (14d), Box 5 (30d). Correct promotes to next box; incorrect demotes to Box 1.
- Implemented `SanUtils.moveToSan` for precise SAN generation and move matching.
- Built `RepertoireScreen` providing White/Black repertoire selection, Browse & Edit mode (interactive `Chessboard`, breadcrumb line path, variation chips, position notes/comments, delete move), and Practice mode (due nodes review, correct/incorrect feedback, practice complete state).
- Added comprehensive unit and instrumented test suites: `LeitnerSpacedRepetitionTest`, `SanUtilsTest`, `RepertoireTreeTest`, and `RepertoireDaoTest`.

**Verification Checklist:**
- ✓ `./gradlew assembleDebug` succeeds with zero errors.
- ✓ `./gradlew app:testDebugUnitTest` passes: 138 tests, 0 failures.
- ✓ Adding a 5-move-deep variation with branch point persists and retrieves entire tree without node loss (`RepertoireDaoTest`).
- ✓ Self-referencing CASCADE deletion verified: deleting a parent node deletes the entire subtree (`RepertoireDaoTest`).
- ✓ Practice session spaced-repetition logic verified: correct promotes box, wrong demotes to Box 1 (`LeitnerSpacedRepetitionTest`, `RepertoireTreeTest`).
- ✓ Due query returns empty after practice session completion.

---

## Phase 10: Tactical Puzzle Engine (COMPLETED)

**Summary:**
- Created `PuzzleEntity`, `PuzzleAttemptEntity`, and `UserPuzzleRatingEntity` in Room schema version 3 with `AutoMigration(from = 2, to = 3)`.
- Created `PuzzleDao` and `PuzzleRepository` managing puzzle queries, attempt logging, and user rating persistence.
- Implemented **Glicko-2 Rating System** (`Glicko2RatingSystem.kt`) calculating expected score, rating variance, volatility, and rating deviation updates.
- Bundled curated CC0 Lichess puzzle dataset (`puzzles_curated.json` asset) imported on first launch.
- Implemented `MistakePuzzleGenerator` to auto-generate "Learn from your mistakes" puzzles from user games analyzed in Phase 8 (scanning for BLUNDER/MISTAKE moves and setting solution to the engine's top line).
- Implemented `PuzzlesViewModel` supporting multi-move solution sequences with automatic opponent forced replies (`delay(300)`).
- Built `PuzzlesScreen` reusing Phase 6 `Chessboard`, displaying user rating badge, feedback/delta indicators, hint options, and retry/next controls.
- Added unit and instrumented test suites: `Glicko2RatingSystemTest`, `MistakePuzzleGeneratorTest`, and `PuzzleDaoTest`.

**Verification Checklist:**
- ✓ `./gradlew assembleDebug` succeeds with zero errors.
- ✓ `./gradlew app:testDebugUnitTest` passes: 144 tests, 0 failures.
- ✓ Solving 10 puzzles of increasing difficulty produces a smooth, sensible rating curve (`Glicko2RatingSystemTest`).
- ✓ Multi-move puzzle sequence auto-plays opponent reply on legal board state (`PuzzlesViewModel`).
- ✓ "Learn from your mistakes" puzzle generation extracts position FEN before blunder and assigns solution line (`MistakePuzzleGeneratorTest`).

---

## Phase 11: AI Coach Context Formatter

**Agent Prompt:**
> Write `AICoachContextBuilder.build(move: MoveEntity, game: GameEntity, priorPly: List<MoveEntity>): CoachContext`, returning a structured data class (not a prose string) with `fenBefore`, `fenAfter`, `sanMove`, `classificationTier`, `evalDeltaCp`, up to 3 `alternativeLines` (short SAN sequence + score from MultiPV), and up to 4 ply of preceding history as SAN. Serialize to JSON only at the point of sending to the LLM provider (Phase 12) — keep the structured class as the internal representation so it's independently testable. Enforce a fixed token budget (roughly 800 tokens as a starting point, tuned from real usage), truncating in this order when over budget: preceding move history first, then the count of alternative lines, and the position itself only as a last resort, since it's the one piece of context the coach can't reason without. Include no user account identifiers, device identifiers, or any other PII — the payload should be fully reconstructable from nothing but game data already in Room.
>
> Before considering this phase complete, verify: building context for a move deep into a long game (e.g. move 40) produces a payload under the token budget using an actual tokenizer count, not character count as a proxy; building context for the very first move of a game (no history to truncate) doesn't crash or produce malformed JSON; and manual review of 3 sample payloads confirms no PII field is present anywhere in the serialized output.

---

## Phase 12: LLM Integration for Conversational Coaching

**Agent Prompt:**
> Define `AiCoachProvider` with a method like `fun streamResponse(context: CoachContext, userQuestion: String): Flow<String>`, so the LLM vendor is swappable without touching UI code. Implement one concrete provider calling **your own backend** (Cloud Function, Cloud Run, or similar) over Server-Sent Events via Ktor's SSE client — never call a third-party LLM API directly from the app, since an API key embedded in a distributed APK is trivially extractable via decompilation and will be found within days. The backend request should carry an authenticated user/session identifier so per-tier daily request caps are enforced **server-side** — client-side caps are a UX nicety at best, since (this app being open source) a rebuilt client can simply not enforce them. Render streamed tokens incrementally into a Compose `Text` backed by appending mutable state, with a loading indicator before the first token and graceful handling of a dropped connection mid-stream (a visible retry affordance, not a silently truncated response).
>
> Before considering this phase complete, verify: a real end-to-end request against your backend streams visible partial text within a couple seconds of the first token, confirming SSE is consumed incrementally rather than rendered all-at-once; killing the network mid-stream (airplane mode toggle) surfaces a visible error/retry state instead of an indefinite spinner; and a second, trivial `AiCoachProvider` implementation (even one returning canned text) swaps in via the interface with zero changes to `AnalysisDashboardScreen`, proving the abstraction actually decouples UI from vendor.

---

## Phase 13: Freemium Tiering & In-App Purchases

**Agent Prompt:**
> Integrate **Play Billing Library 9.x** (`com.android.billingclient:billing-ktx`) — Google requires v8+ for any app update after Aug 31, 2026, so don't start from an older v5/v6 tutorial's code. Implement the subscription purchase flow: query `ProductDetails`, launch the billing flow, and on a resulting `Purchase`, send its purchase token to your backend rather than unlocking anything client-side immediately. On the backend, validate every purchase via the Google Play Developer API's `purchases.subscriptionsv2.get` before marking the user subscribed in your own database — never trust the client-reported `BillingResult` alone, since in an open-source app a modified client build could fake a successful result locally. Add "Restore Purchases" re-querying `queryPurchasesAsync` and re-validating against the backend. Handle `GRACE_PERIOD`/`ON_HOLD` as distinct UI states from a fully expired subscription (a gentle "payment issue" banner, not an abrupt lockout), since these are recoverable states Google expects apps to handle gracefully. Enforce the paywall boundary from the Licensing section above: never gate on-device Stockfish analysis, move classification, or puzzle solving — gate only server-backed features (AI coach chat, cross-device cloud sync, curated/rated puzzle packs beyond a free daily quota, elevated analysis-request caps).
>
> Before considering this phase complete, verify: a test purchase (Play Console license-tester account) round-trips correctly — client-side completion, backend validation confirms it, subscribed state persists across an app restart; simulating an expired subscription (test account, or manually flipping the backend's stored state) correctly re-locks gated features on next launch; and a manual code review of every feature gate confirms none of them wrap Stockfish analysis, classification, or local puzzle solving — only the server-backed features above.

---

## Phase 14: Testing, QA & Crash Reporting

**What's happening:** Nothing earlier in the plan tests the riskiest code — native JNI, async engine output, move classification, or billing. This phase exists so that isn't left to chance.

**Agent Prompt:**
> Add: JVM unit tests for `MoveAnalyzer`'s thresholds and `SacrificeDetector` (per Phase 5's checks, if not already fully covered); an instrumented Compose UI test dragging a piece and asserting the resulting FEN (per Phase 6, if not already covered); a JNI smoke test confirming `bestmove` reliably returns within N seconds on a **low-end emulator profile** (e.g. API 26, 2GB RAM) since native performance varies wildly across real device tiers, not just a flagship emulator; and Firebase Crashlytics (or equivalent) with **NDK crash symbolication explicitly enabled** (upload native debug symbols via the Crashlytics Gradle plugin's symbol-upload task) — without this, any JNI-side crash shows up as an unreadable memory address instead of a symbol name.
>
> Before considering this phase complete, verify: `./gradlew test connectedAndroidTest` passes with zero failures in a reasonable CI time budget; deliberately triggering a native crash in a debug build (e.g. a temporary null-pointer dereference in `native-lib.cpp`) produces a Crashlytics report with a **readable, symbolicated** native stack trace, not hex addresses — worth confirming now, since discovering symbolication is broken after a real production crash is far more expensive; and the low-end emulator profile confirms `go depth 18` completes within a defined tolerable time for your actual target users, not just your dev machine.

---

## Phase 15: Polish, Animations & Sound Effects

**Agent Prompt:**
> Add SoundPool-based move/capture/check/checkmate sound effects (SoundPool, not MediaPlayer, given how frequently move sounds fire), with a per-user mute toggle persisted in Jetpack DataStore (not SharedPreferences). Add subtle haptic feedback on piece drop and puzzle-solved. Add Compose `animateFloatAsState`/`AnimatedContent`/`AnimatedVisibility` transitions for board flip, eval bar value changes, and move-list tier badges appearing. Wrap every sound/haptic call so a denied audio focus request, a device with no vibrator, or muted media volume degrades silently (skip the effect) rather than throwing.
>
> Before considering this phase complete, verify: toggling mute persists across an app restart (confirms DataStore actually committed, not just in-memory state); running on an emulator profile with no vibrator hardware doesn't crash on a haptic call; and rapidly stepping through a move list doesn't cause overlapping/garbled sound playback or visibly janky animation.

---

## Phase 16: Play Store Release & Production Build

**Agent Prompt:**
> Configure the release build: enroll in Play App Signing, write ProGuard/R8 keep rules for JNI-referenced method signatures (anything called via `CallStaticVoidMethod` from native code must be kept by exact signature, or R8 will silently strip/rename it and break the native callback) and for Room-generated classes, confirm ABI-split App Bundle output actually produces separate `arm64-v8a`/`armeabi-v7a` splits (inspect the built `.aab` with `bundletool`), and confirm the `:nnue_assets` Play Asset Delivery module is correctly wired into the AAB by testing an actual install-time delivery build via `bundletool build-apks` on a real device — not just a debug APK, since asset pack behavior differs between debug and real delivery. Before submission: complete the Play Console Data Safety form disclosing that game/move data is sent to a third-party AI provider (Phase 12) and that payment data is processed via Play Billing (Phase 13), publish a linked Privacy Policy, and — since the app is GPLv3 — publish the source repository link somewhere reachable from the app or its store listing, so the GPL's source-availability obligation is actually satisfied, not just true in principle.
>
> Before considering this phase complete, verify: a release-signed build installed from a real `bundletool`-generated APK set (not `assembleDebug`) launches without a ProGuard-related crash — specifically exercise the native JNI callback path and Room queries, the two most common R8-stripping failure points; the Data Safety form and Privacy Policy link are both visible on the actual Play Console listing preview before publishing; and the GPL source link resolves to a real, publicly accessible repository matching the exact commit that was submitted — not an outdated or private link.

---

## Recorded Decisions
Fill these in as each phase actually runs — future phases and future agent sessions depend on these being accurate, not guessed.
- **Toolchain (Phase 0.5)**:
  - AGP: 8.5.2
  - Kotlin: 2.0.20
  - Gradle Wrapper: 8.14.5
  - NDK: 28.2.13676358
  - CMake: 3.22.1
  - Compose BOM: 2024.10.01
  - Hilt: 2.52
  - KSP: 2.0.20-1.0.25
  - compileSdk: 34
  - targetSdk: 34
  - minSdk: 26 
- NDK version: 28.2.13676358 (Phase 1)
- Stockfish pinned tag/commit: sf_19 (commit edb0d9db6731067ec50ce619ff372b463bc4dd5d) (Phase 2)
- **JNI Bridge & Engine Session Manager (Phase 3)**:
  - Concurrent analysis: Rejected (only one active analysis at a time)
  - Thread management: Single pthread per engine instance, attached/detached via AttachCurrentThread
  - Command queue: std::queue<std::string> with std::mutex + std::condition_variable
  - **Critical fix**: engineStart() clears leftover commands from queue before spawning pthread to prevent race conditions between sessions
  - Callback mechanism: JNI CallStaticVoidMethod → NativeBridge.onEngineOutput(String) → SharedFlow<String>
  - Engine output: Exposed as Flow<String> via Kotlin Coroutines, not LiveData
  - Error handling: Result<Unit> for all suspend functions
  - Cancellation: Sends "stop" command when coroutine scope is cancelled
  - Thread cleanup: pthread_join() with no timeout (relies on graceful quit), safe start/stop cycles verified
  - Test verification: Full test suite passes 5 consecutive runs (0 failures), engine_consistency_multiple_runs restored with proper cleanup
  - Implementation choice: Real Stockfish integration via native-lib_real.cpp (not simulated)
- Board representation (bitboard vs 8x8 array): Bitboard (Phase 4) - Chosen for performance and compatibility with modern chess engines. Uses 64-bit Long for each piece type, enabling fast move generation and position evaluation. Helper methods provide 8x8 array conversion for debugging when needed.
- **Move Analysis & Classification Engine (Phase 5)**:
  - Classification thresholds (centipawn delta from mover's perspective):
    - BEST: delta = 0 (exact match with top engine line)
    - EXCELLENT: |delta| <= 10 (very small evaluation loss, nearly optimal)
    - GOOD: 10 < |delta| <= 50 (acceptable position maintained)
    - INACCURACY: 50 < |delta| <= 100 (small but noticeable evaluation loss)
    - MISTAKE: 100 < |delta| <= 300 (significant evaluation loss)
    - BLUNDER: |delta| > 300 (severe evaluation loss, likely losing the game)
    - MISS: Top line contains mate-in-N or large tactical win (>500cp) not taken
    - BOOK: Position+move matches opening database (checked before engine analysis)
  - Threshold reasoning: Based on common chess engine classification standards (Lichess: 10, 50, 100, 300; Chess.com: 10, 30, 100, 300). Our values are a middle ground, leaning slightly stricter on mistakes.
  - Mate score normalization: MAX_CP = 10000, MATE_PENALTY = 100. Formula: 10000 - (100 * matingDistance) for mating side, negated for mated side.
  - MultiPV count: 3 (captures top 3 PV lines for miss detection)
  - Default analysis depth: 14 (configurable, lower for "quick analysis", 18-20 for "deep")
  - SacrificeDetector: Isolated component requiring material loss, evaluation improvement, and multiple legal moves (not forced)
  - UciParser: Extracts evaluation scores from MultiPV output lines and bestmove lines
- **Interactive Compose Chessboard (Phase 6)**:
  - Board square size: 48.dp (`ChessSquareSize`), board total 384.dp (8×48). Note: design system specified 38x38px; 48dp chosen as it adapts to screen density (renders 38px on ~0.8x density, 48px on 1x, 72px on 1.5x, etc.). Zero corner radius on squares as specified.
  - Dark squares: `Color(0xFF191E2B)`, Light squares: `Color(0xFF9F9F9F)` — exact match to design system hex values
  - Piece rendering: 6 vector drawable assets (Pawn, Knight, Bishop, Rook, Queen, King) in `res/drawable/`, one per piece type. Color determined at runtime via `ColorFilter.tint` — White pieces `#E8E8E8` (`Color(0xFFE8E8E8)`), Black pieces `#1A1A1A` (`Color(0xFF1A1A1A)`)
  - `piecePainter(pieceType: PieceType): Painter` defined in `ui/components/PiecePainter.kt`, maps each PieceType to its `R.drawable.ic_piece_*` resource
  - Two input modalities: (1) tap-to-move via per-square `.clickable` calling `handleTap()` which resolves against `legalMoves` from `MoveGenerator.generateLegalMoves`; (2) drag-to-move via board-level `pointerInput(Unit)` + `detectDragGestures`
  - **Critical fix**: Removed `detectTapGestures` from the board-level `pointerInput` — having both board-level tap detection and square-level `.clickable` caused double-tap handling. Board-level gesture detector now handles only `detectDragGestures`; taps are handled solely by square-level `.clickable`
  - Board orientation: `BoardOrientation` enum (`WHITE_AT_BOTTOM`, `BLACK_AT_BOTTOM`). `offsetToSquare()` uses `rememberUpdatedState(orientation)` to always read current orientation inside the `pointerInput` block
  - Promotion picker: `PromotionPicker.kt` — inline 4-icon row (Queen, Rook, Bishop, Knight) shown as centered overlay when a pawn reaches last rank. Uses `HapticFeedbackType.TextHandleMove` on selection. Move is not resolved until user picks
  - Legal-move indicators: filled dot for non-captures, hollow ring (via `Canvas` + `Stroke`) for captures — rendered by reading `MoveGenerator.generateLegalMoves`, never reimplemented in UI
  - Highlights: last-move gold (`Color(0x33FFD700)`), selected blue (`Color(0x554A90D9)`), checked king red (`Color(0x44FF4444)`), engine hints blue tint (`Color(0x224A90D9)`)
  - TalkBack accessibility: each `ChessSquare` has `semantics { contentDescription }` with coordinate + piece type/color (e.g. "e4, White knight") or "empty" for unoccupied squares; `testTag = "square_<name>"` on each; board itself has descriptive contentDescription with side-to-move and check status
  - Architecture: `Chessboard` is **stateless/purely-presentational** — all state (board, selection, callbacks) hoisted from `AnalysisViewModel` via `StateFlow`
  - Reusability: `Chessboard` composable designed for reuse across Phase 6 (`AnalysisScreen`), Phase 7 (`AnalysisDashboardScreen`), and Phase 10 (`PuzzleScreen`)
  - **ClassificationColors**: Defined in Phase 7. See `ui/theme/ClassificationColors.kt` — single source of truth mapping all `ClassificationTier` values to colors, background alphas, text colors, delta-cp severity colors, and human-readable labels. Used by `AnalysisDashboardScreen`'s move list rows — no inline hex scattered per composable.
  - Build artifacts: `ui/components/Chessboard.kt` (with `ChessSquare`, `offsetToSquare`, `handleTap`, `drawSquareHighlight`), `ui/components/PiecePainter.kt` (`piecePainter` + `pieceResId`), `ui/components/PromotionPicker.kt` (`PromotionPicker`, `PromotionOption`), `ui/screens/AnalysisScreen.kt` (hosts `Chessboard`), `ui/screens/viewmodels/AnalysisViewModel.kt` (hoisted `StateFlow` state)
- **Analysis Dashboard & Evaluation Bar (Phase 7)**:
  - Analysis dashboard: `AnalysisDashboardScreen` combining vertical eval bar, tier-tinted `LazyColumn` move list, and explanation panel
  - Eval bar animation: `animateFloatAsState` with spring `AnimationSpec` for smooth transitions between positions
  - Move list row tinting: `ClassificationColors` mapping as single source of truth — no inline hex per composable
  - Batch analysis: `CoroutineWorker` (`GameAnalyzerWorker`) via WorkManager for process-death survival; depth configurable (14 quick / 18-20 deep)
  - Progress reporting: `setProgress()` (current/total ply) observed via `WorkInfo` state in Compose
  - Cancel button: calls both `WorkManager.cancelWorkById()` and `engineRepository.stopEngine()` for immediate CPU halt
  - NNUE asset: `NNUEAssetPackManager` resolves Play Asset Delivery `:nnue_assets` install-time module path at runtime; debug APK installs can't resolve this (expected — requires Play Store delivery)
  - Output buffer fix: `clearOutputBuffer()` before each `MoveAnalyzer.analyze()` to prevent stale SharedFlow replay false BEST classifications
- **Local Game Storage with Room (Phase 8)**:
  - Room version: 2.6.1 with KSP and `androidx.room` Gradle plugin
  - Schema export directory: `app/schemas` tracked in Git for `AutoMigration` baseline diffs
  - Entities: `GameEntity` and `MoveEntity` (with `CASCADE` foreign key on `gameId` and index on `classificationTier`)
  - DAOs: `GameDao` and `MoveDao` providing reactive Flow queries
  - Repository: `GameRepository` exposing clean domain models (`Game`, `Move`)
  - Database: `ChessinDatabase` singleton with `AutoMigration` support enabled
- **Engine Pipeline & Classifier Fixes (Code Review Items)**:
  - NativeBridge: Updated `_engineOutput` to `MutableSharedFlow<String>(replay = 10, extraBufferCapacity = 500)` with `@OptIn` `_engineOutput.resetReplayCache()` in `resetOutputBuffer()` to ensure late subscribers receive `bestmove` lines while maintaining instance immutability.
  - MoveAnalyzer: Added `EvalScore.invert()` and updated `analyze()` to normalize Stockfish `afterState` evaluations (where opponent is side to move) and Black `beforeState` evaluations onto White's scale before computing deltas and calling `classify()`.
  - PgnParser: Fixed `parseSan()` plain pawn move parsing bug where unconditional `.drop(1)` caused `"e4"` to be treated as rank disambiguation (`disambigRank = 3`) and fail move lookup.
  - SacrificeDetector: Fixed perspective handling in `isBrilliant()` by passing `moverIsWhite = (beforeState.sideToMove == Color.WHITE)` into `toNormalizedCp()`. Overhauled `calculateMaterialLoss()` so material loss is only flagged if `afterState.isSquareAttacked(move.to, opponent)` is true or if another friendly piece is left hanging—preventing captures of undefended pieces (e.g. Rook taking undefended Bishop) from being falsely flagged as Brilliant.
  - ABI Filters: Configured `arm64-v8a` and `armeabi-v7a` for release, plus `x86_64` for debug in `app/build.gradle.kts`
  - Play Asset Delivery: `NNUEAssetPackManager` integrated with `AssetPackManagerFactory` for install-time asset pack resolution
  - Session Synchronization: `EngineSessionManager` wrapped with `sessionMutex.withLock` to prevent concurrent UCI command interleaving
  - WorkManager Progress Payload: `GameAnalyzerWorker` progress tuned to send lightweight `PROGRESS_CURRENT` and `PROGRESS_TOTAL` to comply with WorkManager 10KB Data limit
- Spaced repetition scheme: Leitner 5-box scheme (`LeitnerSpacedRepetition`). Box 1: 1 day, Box 2: 3 days, Box 3: 7 days, Box 4: 14 days, Box 5: 30 days. Correct review promotes box number (up to 5); wrong review demotes back to Box 1. (Phase 9)
- Puzzle rating system: Glicko-2 (`Glicko2RatingSystem`). Initial rating = 1500.0, RD = 350.0, volatility = 0.06, $\tau = 0.5$. Converts rating and RD to Glicko-2 scale, computes expected outcome and variance, updates volatility, and scales back. (Phase 10)
- **AI Coach Context Formatter (Phase 11)**:
  - Tokenizer: Added `com.knuddels:jtokkit:1.1.0` dependency (`implementation(libs.jtokkit)`). `CoachTokenCounter` wraps JTokkit's `CL100K_BASE` tokenizer (`Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE)`) to calculate 100% exact OpenAI BPE token counts for JSON strings, FENs, and SAN sequences.
  - Room Schema v4 & MultiPV Population: Added `alternativeLinesJson: String? = null` column to `MoveEntity` with `ChessinDatabase` `AutoMigration(from = 3, to = 4)`. Updated `UciParser.extractTopInfo()`, `MoveAnalyzer.analyze()`, `AnalyzedMove`, and `GameAnalyzerWorker` to capture MultiPV = 2, 3 alternative lines during engine analysis and persist them as JSON into `MoveEntity.alternativeLinesJson`.
  - Context Builder: `AICoachContextBuilder.build()` enforces a max token budget (~800 tokens) using exact JTokkit token counts with priority truncation: prior move history $\rightarrow$ alternative MultiPV lines $\rightarrow$ position FEN simplification as last resort.
  - PII Protection: Guaranteed zero PII (no user ID, device ID, email, or account metadata) in `CoachContext.toJson()` outputs.
- **LLM Integration for Conversational Coaching (Phase 12 - COMPLETED)**:
  - Status: **COMPLETED & SHIPPED**.
  - **Cloudflare Worker Backend**: Created and deployed Cloudflare Worker (`https://chessin.chessin.workers.dev`) handling `POST /api/coach/stream`. Streams AI Coach token explanations directly over Server-Sent Events (`text/event-stream`). Securely supports OpenAI API keys via Wrangler secrets and native Cloudflare Workers AI (`env.AI`).
  - **Firebase Authentication**: Integrated Firebase Auth (`firebase-auth-ktx`), Credential Manager (`androidx.credentials`), and Google Identity (`googleid`) with `app/google-services.json` (SHA-1 fingerprint `D1:6E:A7:C6:8D:77:F5:08:A9:15:C6:A2:D8:A7:7C:D0:69:7D:A5:3B` registered).
  - **JWT Token Provider**: Implemented `FirebaseAuthSessionTokenProvider` retrieving real Firebase Auth ID Tokens (`user.getIdToken()`) and attaching `Authorization: Bearer <idToken>` headers to Cloudflare Worker requests.
  - **UI & Streaming**: Built `SignInScreen` with Google Sign-In and Guest mode options. Connected `BackendAiCoachProvider` to `AnalysisDashboardScreen` for real-time token rendering. Verified with unit tests (**154/154 JVM tests passed**).
- **WorkManager & Hilt KSP Compiler Fix**:
  - `ksp(libs.hilt.work.compiler)` (`androidx.hilt:hilt-compiler:1.2.0`) is required in `app/build.gradle.kts` alongside `ksp(libs.hilt.compiler)` (`com.google.dagger:hilt-compiler`). Without `hilt-work-compiler`, KSP does not run the `@HiltWorker` annotation processor, causing `GameAnalyzerWorker_HiltModule` and `GameAnalyzerWorker_AssistedFactory` to be missing from the Dagger graph and resulting in `NoSuchMethodException` when WorkManager attempts worker instantiation.
  - `ChessinApplication.kt` explicitly initializes `WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())` in `onCreate()` to guarantee Hilt field injection completes before WorkManager executes any scheduled jobs.
  - Evidence log:
    ```text
    WM-WorkerWrapper: Worker result SUCCESS for Work [ id=cbd4cfd9-4e04-4341-af99-fde3d18377ad, tags={ com.pro.chessin.data.work.GameAnalyzerWorker, game_analysis } ]
    ```
- **Play Asset Delivery Local Testing Verification Method**:
  - The sanctioned way to test real Play Asset Delivery (`:nnue_assets` install-time module) without placing `.nnue` into `app/src/main/assets` (which bloats `base.apk` and violates Phase 2 rules) is `bundletool`:
    ```powershell
    java -jar bundletool.jar build-apks \
      --bundle="app/build/outputs/bundle/debug/app-debug.aab" \
      --output="app/build/outputs/bundle/debug/app-debug.apks" \
      --local-testing \
      --mode=default \
      --overwrite

    java -jar bundletool.jar install-apks \
      --apks="app/build/outputs/bundle/debug/app-debug.apks" \
      --device-id="<device_serial>"
    ```
  - `bundletool` pushes the asset pack split APK (`base-master_2.apk`, ~110MB) into `/sdcard/Android/data/com.pro.chessin/files/local_testing/` on the target device, enabling Play Core's local testing sandbox.
- **Chessboard Layout & Rank 8 Bitboard Promotion Fixes (Phase 6 / Phase 14 UI Testing)**:
  - **Rank 8 Bitboard Mask Fix**: `ChessBoardState.kt`'s `emptySquares` previously applied `and 0xFFFFFFFFFFFFFFFL` (15 hex chars = 60 bits), which zeroed out bits 60–63 (Rank 8: `e8`, `f8`, `g8`, `h8`). This caused pawn promotion moves onto rank 8 to be evaluated as blocked by occupied squares. Fixed by changing `emptySquares` to `allPieces.inv()`.
  - **Responsive Board Grid**: Removed erroneous `.aspectRatio(1f)` from `Row` inside `Chessboard`'s `Column` (which previously forced each 8-square row width to equal 1/8th board height, compressing the entire chessboard into a 48dp strip on the left side of the screen). `Chessboard` now uses `fillMaxWidth()` on `Row`s, `fillMaxHeight()` on `ChessSquare`s, and `aspectRatio(1f)` on the outer `Box`.
  - **Analysis Dashboard Row Heights**: Updated `AnalysisDashboardScreen.kt`'s Board + Eval bar row to use `height(IntrinsicSize.Min)`, letting the `Chessboard`'s 1:1 aspect ratio dictate the row height and causing the `EvaluationBar` (`fillMaxHeight()`) to match it seamlessly.
  - **Instrumented UI Tests**: Updated `ChessboardInteractionTest.kt` with `performDrag` delay steps (`moveTo(..., delayMillis = 16L)`) so Compose gesture detectors evaluate touch movement over time. All **13/13 instrumented UI tests pass on device** (`CPH2691 - Android 16`).
- **Analysis Dashboard Interactive Move Controls & PGN Parsing (Phase 7 / Phase 12 UX)**:
  - **Move Navigation Bar**: Added `|<` (First Move), `<` (Previous Move), `Move X / Y` counter, `>` (Next Move), and `>|` (Last Move) controls directly below the chessboard on `AnalysisDashboardScreen.kt`. Enables smooth ply-by-ply stepping through any imported or analyzed game.
  - **Synchronous PGN Import**: Updated `AnalysisDashboardViewModel.importPgn()` to run `PgnParser.parse()` synchronously, populating all game moves instantly into `analyzedMoves` so the user can navigate the game immediately while background Stockfish engine analysis calculates move evaluations via `GameAnalyzerWorker`.
  - **Interactive Board Moves**: Connected `Chessboard`'s `selectedSquare`, `onSquareSelected`, and `onMoveAttempted` callbacks on `AnalysisDashboardScreen.kt`. Players can tap or drag pieces on the board to make moves dynamically, automatically creating new move records and updating the position.
  - **Always-Available AI Coach**: Updated `ExplanationPanel` to present position guidance and the **"Ask AI Coach"** button for the current board position when no move is selected, as well as for individual selected moves.
- **Freemium Tiering & Play Billing Library Integration (Phase 13 - COMPLETED)**:
  - **Billing Library**: Added `com.android.billingclient:billing-ktx:7.1.1` dependency.
  - **Paywall Boundary Enforced**: On-device Stockfish engine analysis, local move classification, local puzzle solving, and opening repertoire practice remain 100% free and ungated. Server-backed AI Coach chat requests and cloud features are gated.
  - **Billing Repository**: Implemented `BillingRepository.kt` managing `BillingClient` connection, subscription product details queries (`subscription_pro_monthly`, `subscription_pro_yearly`), purchase flow launching, and purchase restoration (`queryPurchasesAsync`).
  - **Subscription States**: `SubscriptionStatus` domain model supporting `FREE_TIER`, `PRO_SUBSCRIBED`, `GRACE_PERIOD`, `ON_HOLD`, and `EXPIRED`.
  - **Paywall UI**: Implemented `PaywallScreen.kt` and `SubscriptionViewModel.kt` featuring subscription tier selection, pricing strings from Google Play, payment warning banners for `GRACE_PERIOD` / `ON_HOLD`, and a "Restore Purchases" button.
- **AI Coach Multi-Move Chat & State Reset Fix (Phase 12 / 14 UX)**:
  - **Auto-Reset Chat State on Move Navigation**: Updated `AnalysisDashboardViewModel.kt` (`selectMove`, `firstMove`, `prevMove`, `nextMove`, `lastMove`, `onMoveAttempted`, `importPgn`, `retryAnalysis`) to reset `_coachChatState` to `CoachChatState.Idle` whenever the selected board position or move changes, so the "Ask AI Coach" button is immediately available for each move.
  - **"Ask AI Coach Again" Button**: Updated `ExplanationPanel` in `AnalysisDashboardScreen.kt` to display an "Ask AI Coach Again" button beneath completed/streaming AI explanations, allowing users to re-ask or request additional details on the same move without changing screens.
  - **Fallback for Starting Position**: Updated `requestCoachExplanation()` in `AnalysisDashboardViewModel.kt` to build a fallback `MoveEntity` for the starting board position when `analyzedMoves` is empty or no move is selected, enabling AI Coach guidance on initial game positions.



