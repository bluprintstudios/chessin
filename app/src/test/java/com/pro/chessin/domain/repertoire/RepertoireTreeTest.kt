package com.pro.chessin.domain.repertoire

import com.pro.chessin.data.model.RepertoireNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RepertoireTreeTest {

    @Test
    fun `reconstructs exact line path from leaf node to root`() {
        val root = RepertoireNode(id = 1, parentId = null, fen = "fen1", moveSan = "e4", fromSquare = "e2", toSquare = "e4", colorToPlay = "WHITE")
        val child = RepertoireNode(id = 2, parentId = 1, fen = "fen2", moveSan = "e5", fromSquare = "e7", toSquare = "e5", colorToPlay = "WHITE")
        val grandChild = RepertoireNode(id = 3, parentId = 2, fen = "fen3", moveSan = "Nf3", fromSquare = "g1", toSquare = "f3", colorToPlay = "WHITE")

        val allNodesMap = mapOf(1L to root, 2L to child, 3L to grandChild)

        val path = mutableListOf<RepertoireNode>()
        var curr: RepertoireNode? = grandChild
        while (curr != null) {
            path.add(0, curr)
            curr = curr.parentId?.let { allNodesMap[it] }
        }

        assertEquals(3, path.size)
        assertEquals("e4", path[0].moveSan)
        assertEquals("e5", path[1].moveSan)
        assertEquals("Nf3", path[2].moveSan)
    }

    @Test
    fun `leitner practice session promotes on correct and demotes to box 1 on incorrect`() {
        val node = RepertoireNode(
            id = 1,
            parentId = null,
            fen = "fen1",
            moveSan = "e4",
            fromSquare = "e2",
            toSquare = "e4",
            colorToPlay = "WHITE",
            boxNumber = 2
        )

        val now = 1000000L

        // Correct review on Box 2 promotes to Box 3
        val resultCorrect = LeitnerSpacedRepetition.promote(node.boxNumber, now)
        assertEquals(3, resultCorrect.boxNumber)
        assertFalse(LeitnerSpacedRepetition.isDue(resultCorrect.nextReviewAt, now))

        // Incorrect review demotes to Box 1
        val resultWrong = LeitnerSpacedRepetition.demote(resultCorrect.boxNumber, now)
        assertEquals(1, resultWrong.boxNumber)
        assertFalse(LeitnerSpacedRepetition.isDue(resultWrong.nextReviewAt, now))
    }
}
