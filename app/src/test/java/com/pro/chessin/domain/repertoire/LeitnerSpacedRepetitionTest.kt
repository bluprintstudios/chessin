package com.pro.chessin.domain.repertoire

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class LeitnerSpacedRepetitionTest {

    @Test
    fun `promote increases box number up to max box 5`() {
        val now = 1000000L
        
        var result = LeitnerSpacedRepetition.promote(1, now)
        assertEquals(2, result.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(3), result.nextReviewAt)

        result = LeitnerSpacedRepetition.promote(2, now)
        assertEquals(3, result.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(7), result.nextReviewAt)

        result = LeitnerSpacedRepetition.promote(3, now)
        assertEquals(4, result.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(14), result.nextReviewAt)

        result = LeitnerSpacedRepetition.promote(4, now)
        assertEquals(5, result.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(30), result.nextReviewAt)

        // Cannot promote beyond Box 5
        result = LeitnerSpacedRepetition.promote(5, now)
        assertEquals(5, result.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(30), result.nextReviewAt)
    }

    @Test
    fun `demote resets box number to 1 with 1 day interval`() {
        val now = 1000000L

        val resultFromBox5 = LeitnerSpacedRepetition.demote(5, now)
        assertEquals(1, resultFromBox5.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(1), resultFromBox5.nextReviewAt)

        val resultFromBox2 = LeitnerSpacedRepetition.demote(2, now)
        assertEquals(1, resultFromBox2.boxNumber)
        assertEquals(now + TimeUnit.DAYS.toMillis(1), resultFromBox2.nextReviewAt)
    }

    @Test
    fun `isDue correctly identifies due timestamp`() {
        val now = 1000000L

        assertTrue(LeitnerSpacedRepetition.isDue(now - 1000, now))
        assertTrue(LeitnerSpacedRepetition.isDue(now, now))
        assertFalse(LeitnerSpacedRepetition.isDue(now + 1000, now))
    }
}
