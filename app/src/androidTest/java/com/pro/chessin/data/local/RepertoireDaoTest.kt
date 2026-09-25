package com.pro.chessin.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pro.chessin.data.local.dao.RepertoireDao
import com.pro.chessin.data.local.entity.RepertoireNodeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for RepertoireDao.
 * Verifies tree storage, self-referencing CASCADE deletions, and due queries.
 */
@RunWith(AndroidJUnit4::class)
class RepertoireDaoTest {

    private lateinit var database: ChessinDatabase
    private lateinit var repertoireDao: RepertoireDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChessinDatabase::class.java
        ).build()

        repertoireDao = database.repertoireDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert5MoveDeepTreeWithBranchPoint_persistsAndRetrievesEntireTree() = runTest {
        // 1. e4
        val node1 = RepertoireNodeEntity(
            parentId = null,
            fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            moveSan = "e4",
            fromSquare = "e2",
            toSquare = "e4",
            colorToPlay = "WHITE"
        )
        val id1 = repertoireDao.insertNode(node1)

        // 1... e5 (main line)
        val node2 = RepertoireNodeEntity(
            parentId = id1,
            fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
            moveSan = "e5",
            fromSquare = "e7",
            toSquare = "e5",
            colorToPlay = "WHITE"
        )
        val id2 = repertoireDao.insertNode(node2)

        // 2. Nf3
        val node3 = RepertoireNodeEntity(
            parentId = id2,
            fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            moveSan = "Nf3",
            fromSquare = "g1",
            toSquare = "f3",
            colorToPlay = "WHITE"
        )
        val id3 = repertoireDao.insertNode(node3)

        // 2... Nc6
        val node4 = RepertoireNodeEntity(
            parentId = id3,
            fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            moveSan = "Nc6",
            fromSquare = "b8",
            toSquare = "c6",
            colorToPlay = "WHITE"
        )
        val id4 = repertoireDao.insertNode(node4)

        // 3. Bb5 (Ruy Lopez branch)
        val node5a = RepertoireNodeEntity(
            parentId = id4,
            fen = "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
            moveSan = "Bb5",
            fromSquare = "f1",
            toSquare = "b5",
            colorToPlay = "WHITE"
        )
        val id5a = repertoireDao.insertNode(node5a)

        // 3. Bc4 (Italian Game branch)
        val node5b = RepertoireNodeEntity(
            parentId = id4,
            fen = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
            moveSan = "Bc4",
            fromSquare = "f1",
            toSquare = "c4",
            colorToPlay = "WHITE"
        )
        val id5b = repertoireDao.insertNode(node5b)

        val allNodes = repertoireDao.getNodesByColor("WHITE").first()
        assertEquals(6, allNodes.size)

        // Verify branch point children at id4
        val childrenOfId4 = repertoireDao.getChildrenOf(id4)
        assertEquals(2, childrenOfId4.size)
        val moveSans = childrenOfId4.map { it.moveSan }.toSet()
        assertTrue(moveSans.contains("Bb5"))
        assertTrue(moveSans.contains("Bc4"))
        assertNotNull(repertoireDao.getNodeById(id5a))
        assertNotNull(repertoireDao.getNodeById(id5b))
    }

    @Test
    fun deleteNode_cascadesToSubtree() = runTest {
        val root = repertoireDao.insertNode(
            RepertoireNodeEntity(
                parentId = null,
                fen = "fen1",
                moveSan = "e4",
                fromSquare = "e2",
                toSquare = "e4",
                colorToPlay = "WHITE"
            )
        )
        val child = repertoireDao.insertNode(
            RepertoireNodeEntity(
                parentId = root,
                fen = "fen2",
                moveSan = "e5",
                fromSquare = "e7",
                toSquare = "e5",
                colorToPlay = "WHITE"
            )
        )
        val grandChild = repertoireDao.insertNode(
            RepertoireNodeEntity(
                parentId = child,
                fen = "fen3",
                moveSan = "Nf3",
                fromSquare = "g1",
                toSquare = "f3",
                colorToPlay = "WHITE"
            )
        )

        assertNotNull(repertoireDao.getNodeById(child))
        assertNotNull(repertoireDao.getNodeById(grandChild))

        // Deleting root should CASCADE delete child and grandChild
        repertoireDao.deleteNodeById(root)

        assertNull(repertoireDao.getNodeById(root))
        assertNull(repertoireDao.getNodeById(child))
        assertNull(repertoireDao.getNodeById(grandChild))
    }

    @Test
    fun getDueNodes_returnsOnlyNodesWhereNextReviewAtIsLessThanOrEqualToMaxTimestamp() = runTest {
        val now = 1000000L

        val dueNode = RepertoireNodeEntity(
            parentId = null,
            fen = "fen1",
            moveSan = "e4",
            fromSquare = "e2",
            toSquare = "e4",
            colorToPlay = "WHITE",
            nextReviewAt = now - 500
        )
        val futureNode = RepertoireNodeEntity(
            parentId = null,
            fen = "fen2",
            moveSan = "d4",
            fromSquare = "d2",
            toSquare = "d4",
            colorToPlay = "WHITE",
            nextReviewAt = now + 50000
        )

        repertoireDao.insertNode(dueNode)
        repertoireDao.insertNode(futureNode)

        val dueNodes = repertoireDao.getDueNodes("WHITE", now)
        assertEquals(1, dueNodes.size)
        assertEquals("e4", dueNodes[0].moveSan)
    }
}
