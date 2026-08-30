package com.claudetest.matchtiles.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.claudetest.matchtiles.R
import com.claudetest.matchtiles.model.GameUiState
import com.claudetest.matchtiles.ui.theme.CardWhite
import com.claudetest.matchtiles.ui.theme.Coral
import com.claudetest.matchtiles.ui.theme.Grape
import com.claudetest.matchtiles.ui.theme.Mint
import com.claudetest.matchtiles.ui.theme.Navy
import com.claudetest.matchtiles.ui.theme.NavyFaint
import com.claudetest.matchtiles.ui.theme.NavySoft
import com.claudetest.matchtiles.ui.theme.Sunny
import kotlinx.coroutines.delay

/**
 * The status bar, built out of sticker-style chips: level and board size on the left, score
 * in a sunny chip, hearts and progress on fat rounded meters underneath.
 *
 * The three glyph buttons are the level controls: replay this board, quit to the menu, and
 * the shuffle lifeline.
 */
@Composable
fun Hud(
    state: GameUiState,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip {
                Label(stringResource(R.string.level_label, state.config.level).uppercase())
                Text(
                    text = stringResource(R.string.board_label, state.config.rows, state.config.cols),
                    style = MaterialTheme.typography.titleLarge,
                    color = Navy,
                )
            }
            Spacer(Modifier.weight(1f))
            Chip(fill = Sunny, modifier = Modifier.padding(start = 10.dp)) {
                Label(stringResource(R.string.score_label).uppercase(), color = Navy)
                Text(
                    text = state.totalScore.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Navy,
                )
            }
            GlyphButton(
                glyph = "⟳",
                description = stringResource(R.string.cd_restart),
                onClick = onRestart,
                modifier = Modifier.padding(start = 8.dp),
            )
            GlyphButton(
                glyph = "🏠",
                description = stringResource(R.string.cd_quit),
                onClick = onQuit,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val low = state.chanceFraction <= 0.5f
            Chip(fill = chanceTint(state.chanceFraction), padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "♥",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (low) Navy else Coral,
                    )
                    Text(
                        text = " ${state.chancesLeft}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Navy,
                    )
                }
            }
            RewardFlash(
                reward = state.config.chanceReward,
                matchedPairs = state.matchedPairs,
                level = state.config.level,
                modifier = Modifier.padding(start = 8.dp),
            )
            Meter(
                fraction = state.chanceFraction,
                color = chanceFill(state.chanceFraction),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
            )
            GlyphButton(
                glyph = "🔀",
                description = stringResource(R.string.cd_shuffle),
                onClick = onShuffle,
                enabled = state.canShuffle,
                fill = Grape.copy(alpha = 0.14f),
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Meter(
                fraction = state.progress,
                color = Grape,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.pairs_progress, state.matchedPairs, state.config.pairCount),
                style = MaterialTheme.typography.labelSmall,
                color = NavySoft,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

private val ChipShape = RoundedCornerShape(18.dp)
private const val REWARD_FLASH_MS = 900L

/** A rounded card with a hairline navy outline - the shape every HUD readout shares. */
@Composable
private fun Chip(
    fill: Color = CardWhite,
    padding: Dp = 12.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(ChipShape)
            .background(fill)
            .border(2.dp, Navy.copy(alpha = 0.10f), ChipShape)
            .padding(horizontal = padding, vertical = 8.dp),
        content = content,
    )
}

@Composable
private fun Label(text: String, color: Color = NavySoft) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** Pops the level's match reward in beside the hearts whenever a pair is found. */
@Composable
private fun RewardFlash(
    reward: Int,
    matchedPairs: Int,
    level: Int,
    modifier: Modifier = Modifier,
) {
    var lastSeen by remember(level) { mutableStateOf(matchedPairs) }
    var showing by remember(level) { mutableStateOf(false) }

    LaunchedEffect(level, matchedPairs) {
        if (matchedPairs > lastSeen) {
            showing = true
            delay(REWARD_FLASH_MS)
            showing = false
        }
        lastSeen = matchedPairs
    }

    val alpha by animateFloatAsState(
        targetValue = if (showing) 1f else 0f,
        animationSpec = tween(240),
        label = "rewardFlash",
    )
    Text(
        text = "+$reward",
        style = MaterialTheme.typography.labelLarge,
        color = Navy,
        modifier = modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(50))
            .background(Mint)
            .padding(horizontal = 9.dp, vertical = 2.dp),
    )
}

@Composable
private fun Meter(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(420),
        label = "meter",
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(14.dp)
            .clip(shape)
            .background(NavyFaint)
            .border(2.dp, Navy.copy(alpha = 0.10f), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(color),
        )
    }
}

/** A round-cornered sticker button holding a single glyph. Greys out when [enabled] is false. */
@Composable
internal fun GlyphButton(
    glyph: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Color = CardWhite,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(ChipShape)
            .background(fill)
            .border(2.dp, Navy.copy(alpha = 0.10f), ChipShape)
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge,
            color = NavySoft,
        )
    }
}

/** Meter fill: green while comfortable, amber when it bites, red when the run is nearly over. */
@Composable
private fun chanceFill(fraction: Float): Color {
    val target = when {
        fraction > 0.5f -> Mint
        fraction > 0.25f -> Sunny
        else -> Coral
    }
    val color by animateColorAsState(target, tween(320), label = "chanceFill")
    return color
}

/** The hearts chip itself turns yellow then red, so the warning lands without reading a number. */
@Composable
private fun chanceTint(fraction: Float): Color {
    val target = when {
        fraction > 0.5f -> CardWhite
        fraction > 0.25f -> Sunny
        else -> Coral
    }
    val color by animateColorAsState(target, tween(320), label = "chanceTint")
    return color
}
