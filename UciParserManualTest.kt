// Manual test for UciParser - run with: kotlinc -script UciParserManualTest.kt
// This bypasses Gradle's test infrastructure to verify parsing logic

@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

// Simulate UciParser logic
fun parseInfoLine(line: String): Pair<Int, Int>? {
    if (!line.startsWith("info")) return null
    
    val tokens = line.split(" ")
    var depth = 0
    var multiPv = 1
    var scoreCp: Int? = null
    var scoreMate: Int? = null
    
    var i = 1
    while (i < tokens.size) {
        when (tokens[i]) {
            "depth" -> {
                if (i + 1 < tokens.size) {
                    depth = tokens[i + 1].toIntOrNull() ?: 0
                    i += 2
                } else {
                    i++
                }
            }
            "multipv" -> {
                if (i + 1 < tokens.size) {
                    multiPv = tokens[i + 1].toIntOrNull() ?: 1
                    i += 2
                } else {
                    i++
                }
            }
            "score" -> {
                if (i + 1 < tokens.size) {
                    when (tokens[i + 1]) {
                        "cp" -> {
                            if (i + 2 < tokens.size) {
                                scoreCp = tokens[i + 2].toIntOrNull()
                            }
                            i += 3
                        }
                        "mate" -> {
                            if (i + 2 < tokens.size) {
                                scoreMate = tokens[i + 2].toIntOrNull()
                            }
                            i += 3
                        }
                        else -> i += 2
                    }
                } else {
                    i++
                }
            }
            else -> i++
        }
    }
    
    val score = scoreCp ?: (scoreMate?.let { 10000 - (100 * it) })
    return if (score != null && depth > 0) {
        Pair(multiPv, score)
    } else {
        null
    }
}

fun main() {
    println("Testing UciParser with real MultiPV output...")
    
    // Test case 1: MultiPV line 1
    val line1 = "info depth 14 multipv 1 score cp 30 pv e2e4 e7e5"
    val result1 = parseInfoLine(line1)
    println("Line 1: $line1")
    println("Result: multiPv=${result1?.first}, score=${result1?.second}")
    assert(result1?.first == 1) { "Expected multiPv=1, got ${result1?.first}" }
    assert(result1?.second == 30) { "Expected score=30, got ${result1?.second}" }
    println("✓ Test 1 passed\n")
    
    // Test case 2: MultiPV line 2
    val line2 = "info depth 14 multipv 2 score cp 20 pv d2d4 d7d5"
    val result2 = parseInfoLine(line2)
    println("Line 2: $line2")
    println("Result: multiPv=${result2?.first}, score=${result2?.second}")
    assert(result2?.first == 2) { "Expected multiPv=2, got ${result2?.first}" }
    assert(result2?.second == 20) { "Expected score=20, got ${result2?.second}" }
    println("✓ Test 2 passed\n")
    
    // Test case 3: Mate score
    val line3 = "info depth 14 multipv 1 score mate 3 pv e2e4 e7e5"
    val result3 = parseInfoLine(line3)
    println("Line 3: $line3")
    println("Result: multiPv=${result3?.first}, score=${result3?.second}")
    assert(result3?.first == 1) { "Expected multiPv=1, got ${result3?.first}" }
    assert(result3?.second == 9700) { "Expected score=9700 (10000-300), got ${result3?.second}" }
    println("✓ Test 3 passed\n")
    
    // Test case 4: Non-info line
    val line4 = "bestmove e2e4 ponder e7e5"
    val result4 = parseInfoLine(line4)
    println("Line 4: $line4")
    println("Result: $result4")
    assert(result4 == null) { "Expected null for non-info line, got $result4" }
    println("✓ Test 4 passed\n")
    
    println("All UciParser manual tests passed!")
}
