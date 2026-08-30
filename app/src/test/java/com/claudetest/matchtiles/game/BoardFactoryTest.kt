package com.claudetest.matchtiles.game

import com.claudetest.matchtiles.model.LevelPlan
import com.claudetest.matchtiles.model.TileState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardFactoryTest {

    @Test
    fun `board holds exactly two of every symbol it uses`() {
        for (level in 1..10) {
            val config = LevelPlan.configFor(level)
            val tiles = BoardFactory.build(config, Random(level))

            assertEquals("tile count at level $level", config.tileCount, tiles.size)
            tiles.groupingBy { it.symbol }.eachCount().forEach { (symbol, count) ->
                assertEquals(
                    "symbol $symbol at level $level should appear an even number of times",
                    0,
                    count % 2,
                )
            }
        }
    }

    @Test
    fun `symbols are unique per pair while the glyph pool is large enough`() {
        for (level in 1..10) {
            val config = LevelPlan.configFor(level)
            if (config.pairCount > Symbols.count) continue
            val tiles = BoardFactory.build(config, Random(level))
            assertEquals(
                "level $level should use one distinct glyph per pair",
                config.pairCount,
                tiles.map { it.symbol }.distinct().size,
            )
        }
    }

    @Test
    fun `tiles start face down with sequential ids`() {
        val config = LevelPlan.configFor(3)
        val tiles = BoardFactory.build(config, Random(99))
        assertEquals(tiles.indices.toList(), tiles.map { it.id })
        assertTrue(tiles.all { it.state == TileState.FaceDown })
    }

    @Test
    fun `the same seed deals the same board`() {
        val config = LevelPlan.configFor(4)
        assertEquals(
            BoardFactory.build(config, Random(42)),
            BoardFactory.build(config, Random(42)),
        )
    }

    @Test
    fun `the glyph pool covers the largest board`() {
        val largest = LevelPlan.configFor(40)
        assertTrue(
            "need at least ${largest.pairCount} glyphs, have ${Symbols.count}",
            Symbols.count >= largest.pairCount,
        )
    }
}
