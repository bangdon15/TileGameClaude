package com.claudetest.matchtiles.game

import com.claudetest.matchtiles.model.LevelConfig
import com.claudetest.matchtiles.model.Tile
import kotlin.random.Random

object BoardFactory {

    /**
     * Builds a shuffled board holding exactly `config.pairCount` pairs.
     *
     * If a board ever needs more pairs than there are glyphs, glyphs repeat. That stays
     * playable — four tiles of one glyph simply form two pairs — it just looks less varied.
     */
    fun build(config: LevelConfig, random: Random = Random.Default): List<Tile> {
        val symbols = pickSymbols(config.pairCount, random)
        return symbols
            .flatMap { listOf(it, it) }
            .shuffled(random)
            .mapIndexed { index, symbol -> Tile(id = index, symbol = symbol) }
    }

    private fun pickSymbols(pairs: Int, random: Random): List<Int> {
        val pool = Symbols.faces.indices.shuffled(random)
        return List(pairs) { pool[it % pool.size] }
    }
}
