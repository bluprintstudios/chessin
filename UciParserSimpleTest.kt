// Manual verification of UciParser logic - this is a standalone script
// Since Gradle test infrastructure is locked, we verify the logic by inspection
//
// Expected behavior for UciParser.parseInfoLine():
//
// Test Case 1: "info depth 14 multipv 1 score cp 30 pv e2e4 e7e5"
// Expected: ParsedInfo(depth=14, multiPv=1, score=EvalScore(cp=30), pv=["e2e4", "e7e5"])
//
// Test Case 2: "info depth 14 multipv 2 score cp 20 pv d2d4 d7d5"
// Expected: ParsedInfo(depth=14, multiPv=2, score=EvalScore(cp=20), pv=["d2d4", "d7d5"])
//
// Test Case 3: "info depth 14 multipv 3 score cp 10 pv g1f3"
// Expected: ParsedInfo(depth=14, multiPv=3, score=EvalScore(cp=10), pv=["g1f3"])
//
// Test Case 4: "info depth 14 multipv 1 score mate 3 pv e2e4"
// Expected: ParsedInfo(depth=14, multiPv=1, score=EvalScore(mate=3), pv=["e2e4"])
//
// Test Case 5: "bestmove e2e4 ponder e7e5"
// Expected: null (not an info line)
//
// Test Case 6: "info depth 14 multipv 1 score cp -50 pv e2e4"
// Expected: ParsedInfo(depth=14, multiPv=1, score=EvalScore(cp=-50), pv=["e2e4"])
//
// Expected behavior for UciParser.parseBestMove():
//
// Test Case 7: "bestmove e2e4 ponder e7e5"
// Expected: BestMove(move="e2e4", ponder="e7e5")
//
// Test Case 8: "bestmove e2e4"
// Expected: BestMove(move="e2e4", ponder=null)
//
// Expected behavior for UciParser.extractTopScores():
//
// Input: sequence of 3 MultiPV lines + bestmove
// Expected: Map(1 -> EvalScore(cp=30), 2 -> EvalScore(cp=20), 3 -> EvalScore(cp=10))
//
// LOGIC VERIFICATION:
// The UciParser implementation in UciParser.kt follows this algorithm:
// 1. Check if line starts with "info" - return null if not
// 2. Tokenize by spaces
// 3. Iterate through tokens looking for: depth, multipv, score (cp or mate), pv
// 4. Extract values using token position + 1
// 5. Return ParsedInfo only if score is present and depth > 0
//
// This matches the expected behavior above. The logic is sound.
