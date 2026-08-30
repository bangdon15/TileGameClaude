package com.claudetest.matchtiles.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelPlanTest {

    @Test
    fun `level one is a four by four board with twelve chances`() {
        val config = LevelPlan.configFor(1)
        assertEquals(4, config.rows)
        assertEquals(4, config.cols)
        assertEquals(16, config.tileCount)
        assertEquals(8, config.pairCount)
        assertEquals(12, config.chances)
        assertEquals(LevelPlan.START_CHANCE_REWARD, config.chanceReward)
    }

    @Test
    fun `early levels follow the documented curve`() {
        val expected = listOf(
            LevelConfig(level = 1, rows = 4, cols = 4, chances = 12, chanceReward = 5),
            LevelConfig(level = 2, rows = 5, cols = 6, chances = 23, chanceReward = 5),
            LevelConfig(level = 3, rows = 6, cols = 6, chances = 28, chanceReward = 4),
            LevelConfig(level = 4, rows = 7, cols = 8, chances = 39, chanceReward = 4),
            LevelConfig(level = 5, rows = 8, cols = 8, chances = 44, chanceReward = 3),
            // The board is capped from here on, so the opening budget holds at 44 and only
            // the match reward keeps tapering.
            LevelConfig(level = 6, rows = 8, cols = 8, chances = 44, chanceReward = 3),
            LevelConfig(level = 7, rows = 8, cols = 8, chances = 44, chanceReward = 2),
        )
        expected.forEach { assertEquals("level ${it.level}", it, LevelPlan.configFor(it.level)) }
    }

    @Test
    fun `the match reward tapers but never disappears`() {
        for (level in 2..40) {
            val previous = LevelPlan.rewardFor(level - 1)
            val reward = LevelPlan.rewardFor(level)
            assertTrue("reward must not climb at level $level", reward <= previous)
            assertTrue(
                "reward must stay at or above the floor at level $level",
                reward >= LevelPlan.MIN_CHANCE_REWARD,
            )
        }
        assertEquals(LevelPlan.START_CHANCE_REWARD, LevelPlan.rewardFor(1))
        assertEquals(LevelPlan.MIN_CHANCE_REWARD, LevelPlan.rewardFor(40))
    }

    @Test
    fun `a perfect clear can never exceed the level's chance ceiling`() {
        for (level in 1..40) {
            val config = LevelPlan.configFor(level)
            assertEquals(
                "ceiling at level $level",
                config.chances + config.pairCount * config.chanceReward,
                config.chanceCeiling,
            )
            assertTrue("ceiling must beat the opening budget", config.chanceCeiling > config.chances)
        }
    }

    @Test
    fun `odd square boards gain a column so they stay pairable`() {
        // 5x5 and 7x7 both have an odd tile count.
        assertEquals(6, LevelPlan.configFor(2).cols)
        assertEquals(8, LevelPlan.configFor(4).cols)
    }

    @Test
    fun `every board can be split into pairs and stays within the cap`() {
        for (level in 1..40) {
            val config = LevelPlan.configFor(level)
            assertEquals("level $level must be pairable", 0, config.tileCount % 2)
            assertTrue("level $level rows within cap", config.rows <= LevelPlan.MAX_DIM)
            assertTrue("level $level cols within cap", config.cols <= LevelPlan.MAX_DIM)
            assertTrue("level $level keeps a chance budget", config.chances >= config.cols)
        }
    }

    @Test
    fun `boards never shrink as levels advance`() {
        var previous = LevelPlan.configFor(1)
        for (level in 2..40) {
            val config = LevelPlan.configFor(level)
            assertTrue(
                "level $level should not be smaller than level ${level - 1}",
                config.tileCount >= previous.tileCount,
            )
            previous = config
        }
    }

    @Test
    fun `the opening chance budget grows by five per level while the board grows`() {
        for (level in 2..LevelPlan.CAP_LEVEL) {
            val previous = LevelPlan.configFor(level - 1)
            val config = LevelPlan.configFor(level)
            val fromWidth = (config.cols - previous.cols) * LevelPlan.CHANCE_MULTIPLIER
            assertEquals(
                "chance growth into level $level",
                LevelPlan.CHANCE_GROWTH_PER_LEVEL + fromWidth,
                config.chances - previous.chances,
            )
            assertTrue(
                "level $level must never be tighter than level ${level - 1}",
                config.chances > previous.chances,
            )
        }
    }

    @Test
    fun `the opening budget plateaus with the board`() {
        val capped = LevelPlan.configFor(LevelPlan.CAP_LEVEL)
        for (level in LevelPlan.CAP_LEVEL..40) {
            val config = LevelPlan.configFor(level)
            assertEquals("level $level board", capped.tileCount, config.tileCount)
            assertEquals("level $level opening budget", capped.chances, config.chances)
        }
    }

    @Test
    fun `difficulty never loosens once the board is capped`() {
        // The point of the plateau: with the grid frozen, only the tapering reward moves,
        // so every level past the cap offers at most as much slack as the one before it.
        var previous = LevelPlan.configFor(LevelPlan.CAP_LEVEL)
        for (level in (LevelPlan.CAP_LEVEL + 1)..40) {
            val config = LevelPlan.configFor(level)
            assertTrue(
                "level $level must not be more forgiving than level ${level - 1}",
                config.chanceCeiling <= previous.chanceCeiling,
            )
            previous = config
        }
        val plateau = LevelPlan.configFor(40)
        assertTrue("the plateau must stay winnable", plateau.chanceCeiling > plateau.pairCount)
    }

    @Test
    fun `the campaign ends on a full sized board past the cap`() {
        val finale = LevelPlan.configFor(LevelPlan.LAST_LEVEL)
        assertEquals(LevelPlan.MAX_DIM, finale.rows)
        assertEquals(LevelPlan.MAX_DIM, finale.cols)
        assertTrue("the finale is past the growth cap", LevelPlan.LAST_LEVEL > LevelPlan.CAP_LEVEL)
        assertTrue(
            "the finale rewards less than the opening level",
            finale.chanceReward < LevelPlan.rewardFor(1),
        )
        assertTrue("the last level is still winnable", finale.chanceCeiling > finale.pairCount)

        assertTrue(LevelPlan.isFinalLevel(LevelPlan.LAST_LEVEL))
        assertTrue("only the finale ends the run", !LevelPlan.isFinalLevel(LevelPlan.LAST_LEVEL - 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `level zero is rejected`() {
        LevelPlan.configFor(0)
    }
}
