package com.pro.chessin.domain.chess

/**
 * Represents a square on the chess board (0-63).
 * Rank 1 = squares 0-7, Rank 2 = squares 8-15, etc.
 * File a = squares 0,8,16,24,32,40,48,56, File b = squares 1,9,17,25,33,41,49,57, etc.
 */
@JvmInline
value class Square(val index: Int) {
    val file: Int get() = index % 8
    val rank: Int get() = index / 8
    
    val fileChar: Char get() = 'a' + file
    val rankChar: Char get() = '1' + rank
    
    val name: String get() = "$fileChar$rankChar"
    
    fun offset(fileOffset: Int, rankOffset: Int): Square? {
        val newFile = file + fileOffset
        val newRank = rank + rankOffset
        return if (newFile in 0..7 && newRank in 0..7) {
            Square(newRank * 8 + newFile)
        } else null
    }
    
    companion object {
        fun fromName(name: String): Square? {
            if (name.length != 2) return null
            val file = name[0].lowercaseChar() - 'a'
            val rank = name[1].digitToIntOrNull()?.minus(1) ?: return null
            return if (file in 0..7 && rank in 0..7) {
                Square(rank * 8 + file)
            } else null
        }
        
        fun fromCoordinates(file: Int, rank: Int): Square? {
            return if (file in 0..7 && rank in 0..7) {
                Square(rank * 8 + file)
            } else null
        }
        
        val A1 = Square(0)
        val B1 = Square(1)
        val C1 = Square(2)
        val D1 = Square(3)
        val E1 = Square(4)
        val F1 = Square(5)
        val G1 = Square(6)
        val H1 = Square(7)
        val A2 = Square(8)
        val B2 = Square(9)
        val C2 = Square(10)
        val D2 = Square(11)
        val E2 = Square(12)
        val F2 = Square(13)
        val G2 = Square(14)
        val H2 = Square(15)
        val A3 = Square(16)
        val B3 = Square(17)
        val C3 = Square(18)
        val D3 = Square(19)
        val E3 = Square(20)
        val F3 = Square(21)
        val G3 = Square(22)
        val H3 = Square(23)
        val A4 = Square(24)
        val B4 = Square(25)
        val C4 = Square(26)
        val D4 = Square(27)
        val E4 = Square(28)
        val F4 = Square(29)
        val G4 = Square(30)
        val H4 = Square(31)
        val A5 = Square(32)
        val B5 = Square(33)
        val C5 = Square(34)
        val D5 = Square(35)
        val E5 = Square(36)
        val F5 = Square(37)
        val G5 = Square(38)
        val H5 = Square(39)
        val A6 = Square(40)
        val B6 = Square(41)
        val C6 = Square(42)
        val D6 = Square(43)
        val E6 = Square(44)
        val F6 = Square(45)
        val G6 = Square(46)
        val H6 = Square(47)
        val A7 = Square(48)
        val B7 = Square(49)
        val C7 = Square(50)
        val D7 = Square(51)
        val E7 = Square(52)
        val F7 = Square(53)
        val G7 = Square(54)
        val H7 = Square(55)
        val A8 = Square(56)
        val B8 = Square(57)
        val C8 = Square(58)
        val D8 = Square(59)
        val E8 = Square(60)
        val F8 = Square(61)
        val G8 = Square(62)
        val H8 = Square(63)
    }
}
