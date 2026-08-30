package com.claudetest.matchtiles.model

/**
 * Board geometry and chance economy for a single level.
 *
 * A board must always be splittable into pairs, so [tileCount] is guaranteed even.
 */
data class LevelConfig(
    val level: Int,
    val rows: Int,
    val cols: Int,
    /** Chances the level opens with. */
    val chances: Int,
    /** Chances handed back for each matched pair on this level. */
    val chanceReward: Int,
) {
    val tileCount: Int get() = rows * cols
    val pairCount: Int get() = tileCount / 2

    /** Every chance the level can ever offer: the opening budget plus a perfect clear. */
    val chanceCeiling: Int get() = chances + pairCount * chanceReward

    init {
        require(level >= 1) { "Levels start at 1, got $level" }
        require(rows > 0 && cols > 0) { "Board must be positive, got ${rows}x$cols" }
        require(tileCount % 2 == 0) { "Board ${rows}x$cols cannot be split into pairs" }
        require(chances > 0) { "A level needs at least one chance" }
        require(chanceReward >= 0) { "A match cannot cost chances, got $chanceReward" }
    }
}

/**
 * Difficulty curve.
 *
 * Level 1 is [START_DIM] x [START_DIM]; both dimensions grow by one per level until
 * they hit [MAX_DIM], which exists purely so tiles stay tappable on a phone.
 *
 * Four rules shape the result:
 *  - A square board with an odd side has an odd tile count and cannot be paired, so
 *    the column count absorbs the extra tile (7x7 becomes 7x8).
 *  - A level starts with [CHANCE_MULTIPLIER] x columns chances (allowed mismatches),
 *    plus [CHANCE_GROWTH_PER_LEVEL] for every level the board actually grew on. Growth
 *    stops with the board at [CAP_LEVEL], otherwise the budget would keep loosening
 *    against a grid that can no longer get harder.
 *  - Every matched pair hands chances back, so finding pairs buys the time needed to hunt
 *    for the rest and one bad streak cannot end a run.
 *  - That reward is worth [START_CHANCE_REWARD] early and loses one chance every
 *    [REWARD_DECAY_EVERY] levels down to [MIN_CHANCE_REWARD], so difficulty keeps rising
 *    to a steady plateau once the board is capped.
 *
 * The campaign runs from level 1 to [LAST_LEVEL]; clearing that level wins the game.
 */
object LevelPlan {
    const val START_DIM = 4
    const val MAX_DIM = 8
    const val CHANCE_MULTIPLIER = 3
    const val CHANCE_GROWTH_PER_LEVEL = 5
    const val START_CHANCE_REWARD = 5
    const val MIN_CHANCE_REWARD = 1
    const val REWARD_DECAY_EVERY = 2

    /** First level whose board has reached [MAX_DIM]; nothing about the grid grows after it. */
    const val CAP_LEVEL = MAX_DIM - START_DIM + 1

    /**
     * Last level of the campaign; clearing it wins the game.
     *
     * One past [CAP_LEVEL] on purpose, so the finale is a full [MAX_DIM] board played at a
     * tightened reward rate rather than the first board that merely stopped growing.
     */
    const val LAST_LEVEL = CAP_LEVEL + 1

    fun isFinalLevel(level: Int): Boolean = level >= LAST_LEVEL

    fun configFor(level: Int): LevelConfig {
        require(level >= 1) { "Levels start at 1, got $level" }

        val target = START_DIM + (level - 1)
        var rows = target.coerceAtMost(MAX_DIM)
        var cols = target.coerceAtMost(MAX_DIM)
        if ((rows * cols) % 2 != 0) {
            if (cols < MAX_DIM) cols++ else rows--
        }

        val grownLevels = level.coerceAtMost(CAP_LEVEL) - 1
        val chances = cols * CHANCE_MULTIPLIER + grownLevels * CHANCE_GROWTH_PER_LEVEL

        return LevelConfig(
            level = level,
            rows = rows,
            cols = cols,
            chances = chances,
            chanceReward = rewardFor(level),
        )
    }

    /** Chances a matched pair is worth on [level]. Never climbs, never reaches zero. */
    fun rewardFor(level: Int): Int {
        require(level >= 1) { "Levels start at 1, got $level" }
        val decay = (level - 1) / REWARD_DECAY_EVERY
        return (START_CHANCE_REWARD - decay).coerceAtLeast(MIN_CHANCE_REWARD)
    }
}
