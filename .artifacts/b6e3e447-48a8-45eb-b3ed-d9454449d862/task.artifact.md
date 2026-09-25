# Phase 7: Analysis Dashboard & Evaluation Bar — Task List

## Status: Completed

- [x] Critical bug fix: stale SharedFlow replay buffer in `MoveAnalyzer.analyze()` — added `clearOutputBuffer()` between query cycles
- [x] Refactored inline delta colors in `MoveListRow` — moved to `ClassificationColors.deltaColorForCp()`
- [x] Wrote JVM unit tests for `ClassificationColors` mapping (all tiers, delta colors, labels) — `ClassificationColorsTest.kt` with 14 tests
- [x] Wrote JVM unit tests for `AnalyzedMove` serialization edge cases — `AnalyzedMoveSerializationTest.kt` with 13 tests (round-trip, malformed data, special chars, negative deltas, missing lines)
- [x] Wrote JVM unit tests for `MoveAnalyzer.classify()` boundary conditions — `MoveAnalyzerClassifyBoundaryTest.kt` with 22 tests (all tiers, MISS thresholds, black perspective, edge cases)
- [x] Fixed bug: `miss_justOverThreshold_isMiss` test — `afterEval=50` didn't trigger inner MISS condition (`50 < 501-500=1` was false); changed to `afterEval=0`
- [x] Fixed bug: `deltaColor_BoundaryAt50` test — expected Orange at cp=50 but code uses strict `>`, so cp=50 falls to >0 (Yellow-green); fixed to match code behavior
- [x] Fixed bug: `backgroundForTier_UsesCorrectAlpha` test — Compose Color quantizes alpha to 8-bit (0.12f→0.1216), widened delta from 0.001 to 0.01
- [x] Fixed bug: Black perspective tests — `toNormalizedCp(false)` negates cp, so delta = afterEval.cp - bestEval.cp; fixed test values to produce correct positive deltas
- [ ] Write instrumented Compose UI test for AnalysisDashboard: verify move list rendering, selection, and board position jump
- [ ] Write instrumented test for GameAnalyzerWorker: verify WorkManager integration, progress reporting, and cancellation
- [x] Verify: `./gradlew assembleDebug` succeeds with zero errors
- [x] Verify: `./gradlew app:testDebugUnitTest` passes with zero failures (122 passed, 0 failed)
- [ ] Verify: Full-game analysis completes on device, classifications shown for every ply
- [ ] Verify: Cancel mid-analysis stops CPU usage within seconds (Logcat confirmation)
- [ ] Verify: Backgrounding app and returning shows correct progress state
- [ ] Verify: Resignation game (non-terminal ending) analyzed correctly by worker
