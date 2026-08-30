package com.claudetest.matchtiles.model

enum class Phase {
    /** Every tile is shown briefly so the player can memorise the board. */
    Preview,
    Playing,
    LevelClear,
    GameOver,
    /** The last level of the campaign is cleared: the run is finished for good. */
    GameWon,
}

data class GameUiState(
    val config: LevelConfig,
    val tiles: List<Tile>,
    val chancesLeft: Int,
    /**
     * Highest chance count held on this level. Matches hand chances back, so [chancesLeft]
     * can climb past the opening budget; the meter drains against this instead.
     */
    val chancePeak: Int,
    val matchedPairs: Int,
    val moves: Int,
    val streak: Int,
    val bestStreak: Int,
    /** Points earned on the current level, including its clear bonus. */
    val levelScore: Int,
    /** Points banked from levels already completed. */
    val bankedScore: Int,
    val phase: Phase,
    val inputLocked: Boolean,
    /** Tiles that just failed to match, used to drive the shake animation. */
    val missIds: Set<Int> = emptySet(),
    /**
     * Identifies the arrangement currently on screen. Bumped by every deal and every
     * shuffle, so the UI can tell a genuinely new board from a recomposition and replay
     * the entrance animation for it.
     */
    val dealId: Int = 1,
    /** How long the current look-at-the-board window lasts. A shuffle buys a longer one. */
    val lookMillis: Long = 0L,
) {
    val totalScore: Int get() = bankedScore + levelScore

    /** Chances handed back by matches on this level. */
    val chancesEarned: Int get() = matchedPairs * config.chanceReward

    val progress: Float
        get() = if (config.pairCount == 0) 0f else matchedPairs.toFloat() / config.pairCount

    val chanceFraction: Float
        get() = if (chancePeak <= 0) 0f else (chancesLeft.toFloat() / chancePeak).coerceIn(0f, 1f)

    /** A reshuffle is only on offer while a level is actually in play. */
    val canShuffle: Boolean
        get() = phase == Phase.Playing && !inputLocked && matchedPairs < config.pairCount

    /** During the preview every tile is face up regardless of its own state. */
    fun isRevealed(tile: Tile): Boolean =
        phase == Phase.Preview || tile.state != TileState.FaceDown

    fun isTappable(tile: Tile): Boolean =
        phase == Phase.Playing && !inputLocked && tile.state == TileState.FaceDown
}
