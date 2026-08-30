package com.claudetest.matchtiles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.claudetest.matchtiles.R
import com.claudetest.matchtiles.game.Symbols
import com.claudetest.matchtiles.model.GameUiState
import com.claudetest.matchtiles.model.Tile
import com.claudetest.matchtiles.model.TileState

/** Tiles wash in on a diagonal; each step further from the top-left waits this much longer. */
private const val APPEAR_STAGGER_MS = 22

/**
 * Lays the grid out by measuring the space available and picking the largest tile size
 * that fits both axes, so every board from 4x4 up to 8x8 stays fully on screen.
 *
 * [onBoardShown] fires once the last tile has finished washing in. A cold start can spend
 * seconds getting the first board on screen, and the memorise window has to be counted from
 * the moment the faces are readable rather than from the deal.
 */
@Composable
fun Board(
    state: GameUiState,
    onTileTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBoardShown: () -> Unit = {},
) {
    val rows = state.config.rows
    val cols = state.config.cols
    val gap = if (cols >= 7) 6.dp else 9.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val tileSize = minOf(
            (maxWidth - gap * (cols - 1)) / cols,
            (maxHeight - gap * (rows - 1)) / rows,
        ).coerceAtLeast(12.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until cols) {
                        val tile = state.tiles[row * cols + col]
                        // Keyed on the deal, not the level: a retry or a shuffle puts a new
                        // arrangement up at the same level, and it has to animate in again
                        // (which is also what re-arms the look window).
                        key(state.dealId, tile.id) {
                            TileCard(
                                tile = tile,
                                face = Symbols[tile.symbol],
                                revealed = state.isRevealed(tile),
                                shaking = tile.id in state.missIds,
                                enabled = state.isTappable(tile),
                                size = tileSize,
                                appearDelayMillis = (row + col) * APPEAR_STAGGER_MS,
                                description = tileDescription(state, tile, row, col),
                                onClick = { onTileTapped(tile.id) },
                                // The far corner carries the longest stagger, so it lands last.
                                onAppeared = if (row == rows - 1 && col == cols - 1) {
                                    onBoardShown
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun tileDescription(state: GameUiState, tile: Tile, row: Int, col: Int): String {
    val r = row + 1
    val c = col + 1
    val glyph = Symbols[tile.symbol].glyph
    return when {
        tile.state == TileState.Matched ->
            "${stringResource(R.string.cd_tile_matched, r, c)}, $glyph"
        state.isRevealed(tile) ->
            "${stringResource(R.string.cd_tile_face_up, r, c)}, $glyph"
        else -> stringResource(R.string.cd_tile_face_down, r, c)
    }
}
