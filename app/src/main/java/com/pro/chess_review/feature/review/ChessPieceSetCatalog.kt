package com.pro.chess_review.feature.review

data class ChessPieceSet(
    val id: String,
    val name: String,
    val author: String,
    val license: String,
)

object ChessPieceSetCatalog {
    const val DEFAULT_ID = "chessnut"

    val all = listOf(
        ChessPieceSet("chessnut", "Chessnut", "Alexis Luengas", "Apache-2.0"),
        ChessPieceSet("rhosgfx", "RhosGFX", "RhosGFX", "CC0 1.0"),
        ChessPieceSet("fantasy", "Fantasy", "Maurizio Monge", "MIT"),
        ChessPieceSet("spatial", "Spatial", "Maurizio Monge", "MIT"),
        ChessPieceSet("celtic", "Celtic", "Maurizio Monge", "MIT"),
        ChessPieceSet("firi", "Firi", "James Faure", "CC BY 4.0"),
        ChessPieceSet("kiwen-suwi", "Kiwen-suwi", "neverRare", "CC BY 4.0"),
        ChessPieceSet("totoy", "Totoy", "Kosal Sen", "CC BY 4.0"),
        ChessPieceSet("papercut", "Papercut", "Nikolay Anzarov", "CC BY 4.0"),
        ChessPieceSet("shapes", "Shapes", "flugsio", "CC BY-SA 4.0"),
    )

    fun find(id: String): ChessPieceSet? = all.firstOrNull { it.id == id }
}
