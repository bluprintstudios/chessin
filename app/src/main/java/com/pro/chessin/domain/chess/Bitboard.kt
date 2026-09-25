package com.pro.chessin.domain.chess

/**
 * Bitboard utilities for chess board representation.
 * Uses 64-bit Long to represent piece positions.
 */
object Bitboard {
    // File bitboards (computed to avoid hex literal issues)
    val FILE_A: Long = computeFile(0)
    val FILE_B: Long = computeFile(1)
    val FILE_C: Long = computeFile(2)
    val FILE_D: Long = computeFile(3)
    val FILE_E: Long = computeFile(4)
    val FILE_F: Long = computeFile(5)
    val FILE_G: Long = computeFile(6)
    val FILE_H: Long = computeFile(7)
    
    // Rank bitboards
    val RANK_1: Long = computeRank(0)
    val RANK_2: Long = computeRank(1)
    val RANK_3: Long = computeRank(2)
    val RANK_4: Long = computeRank(3)
    val RANK_5: Long = computeRank(4)
    val RANK_6: Long = computeRank(5)
    val RANK_7: Long = computeRank(6)
    val RANK_8: Long = computeRank(7)
    
    private fun computeFile(file: Int): Long {
        var result = 0L
        for (rank in 0..7) {
            result = result or (1L shl (rank * 8 + file))
        }
        return result
    }
    
    private fun computeRank(rank: Int): Long {
        var result = 0L
        for (file in 0..7) {
            result = result or (1L shl (rank * 8 + file))
        }
        return result
    }
    
    fun squareBit(square: Square): Long = 1L shl square.index
    
    fun countBits(bitboard: Long): Int = java.lang.Long.bitCount(bitboard)
    
    fun lsb(bitboard: Long): Int = java.lang.Long.numberOfTrailingZeros(bitboard)
    
    fun popLsb(bitboard: Long): Long = bitboard and (bitboard - 1)
    
    fun msb(bitboard: Long): Int = 63 - java.lang.Long.numberOfLeadingZeros(bitboard)
    
    fun shiftNorth(bitboard: Long): Long = (bitboard shl 8) and computeNorthMask()
    fun shiftSouth(bitboard: Long): Long = (bitboard ushr 8) and computeSouthMask()
    fun shiftEast(bitboard: Long): Long = (bitboard and FILE_H.inv()) shl 1
    fun shiftWest(bitboard: Long): Long = (bitboard and FILE_A.inv()) ushr 1
    fun shiftNorthEast(bitboard: Long): Long = (bitboard and FILE_H.inv()) shl 9
    fun shiftNorthWest(bitboard: Long): Long = (bitboard and FILE_A.inv()) shl 7
    fun shiftSouthEast(bitboard: Long): Long = (bitboard and FILE_H.inv()) ushr 7
    fun shiftSouthWest(bitboard: Long): Long = (bitboard and FILE_A.inv()) ushr 9
    
    internal fun computeNorthMask(): Long {
        var mask = 0L
        for (rank in 1..7) {
            for (file in 0..7) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeSouthMask(): Long {
        var mask = 0L
        for (rank in 0..6) {
            for (file in 0..7) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeEastMask(): Long {
        var mask = 0L
        for (rank in 0..7) {
            for (file in 0..6) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeWestMask(): Long {
        var mask = 0L
        for (rank in 0..7) {
            for (file in 1..7) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeNorthEastMask(): Long {
        var mask = 0L
        for (rank in 1..7) {
            for (file in 0..6) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeNorthWestMask(): Long {
        var mask = 0L
        for (rank in 1..7) {
            for (file in 1..7) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeSouthEastMask(): Long {
        var mask = 0L
        for (rank in 0..6) {
            for (file in 0..6) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
    
    internal fun computeSouthWestMask(): Long {
        var mask = 0L
        for (rank in 0..6) {
            for (file in 1..7) {
                mask = mask or (1L shl (rank * 8 + file))
            }
        }
        return mask
    }
}
