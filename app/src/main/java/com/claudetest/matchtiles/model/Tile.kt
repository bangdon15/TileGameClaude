package com.claudetest.matchtiles.model

enum class TileState {
    FaceDown,
    FaceUp,
    Matched,
}

/**
 * One tile on the board. [symbol] is an index into `Symbols.faces`; two tiles match
 * when their symbols are equal.
 */
data class Tile(
    val id: Int,
    val symbol: Int,
    val state: TileState = TileState.FaceDown,
)
