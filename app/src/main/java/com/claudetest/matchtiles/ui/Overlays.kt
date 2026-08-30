package com.claudetest.matchtiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudetest.matchtiles.R
import com.claudetest.matchtiles.model.GameUiState
import com.claudetest.matchtiles.ui.theme.CardCream
import com.claudetest.matchtiles.ui.theme.CardWhite
import com.claudetest.matchtiles.ui.theme.Grape
import com.claudetest.matchtiles.ui.theme.Mint
import com.claudetest.matchtiles.ui.theme.Navy
import com.claudetest.matchtiles.ui.theme.NavySoft
import com.claudetest.matchtiles.ui.theme.Sunny

@Composable
fun LevelClearCard(
    state: GameUiState,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    ResultCard(
        emoji = "🎉",
        title = stringResource(R.string.level_clear_title, state.config.level),
        accent = Mint,
        stats = listOf(
            stringResource(R.string.stat_moves) to state.moves.toString(),
            stringResource(R.string.stat_chances_earned) to "+${state.chancesEarned}",
            stringResource(R.string.stat_chances_left) to state.chancesLeft.toString(),
            stringResource(R.string.stat_level_score) to state.levelScore.toString(),
            stringResource(R.string.stat_total_score) to state.totalScore.toString(),
        ),
        primaryLabel = stringResource(R.string.next_level),
        onPrimary = onNext,
        secondaryLabel = stringResource(R.string.retry_level),
        onSecondary = onRetry,
    )
}

@Composable
fun GameOverCard(
    state: GameUiState,
    onRetry: () -> Unit,
    onNewGame: () -> Unit,
) {
    ResultCard(
        emoji = "💪",
        title = stringResource(R.string.game_over_title),
        accent = Sunny,
        stats = listOf(
            stringResource(R.string.stat_reached) to
                stringResource(R.string.level_label, state.config.level),
            stringResource(R.string.stat_pairs_found) to
                "${state.matchedPairs} / ${state.config.pairCount}",
            stringResource(R.string.stat_best_streak) to state.bestStreak.toString(),
            stringResource(R.string.stat_total_score) to state.totalScore.toString(),
        ),
        primaryLabel = stringResource(R.string.retry_level),
        onPrimary = onRetry,
        secondaryLabel = stringResource(R.string.new_game),
        onSecondary = onNewGame,
    )
}

/** Shown once the last level falls: the run is finished, and there is nothing left to beat. */
@Composable
fun ThankYouCard(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
) {
    ResultCard(
        emoji = "🏆",
        title = stringResource(R.string.thanks_title),
        accent = Grape,
        stats = listOf(
            stringResource(R.string.stat_levels_cleared) to state.config.level.toString(),
            stringResource(R.string.stat_best_streak) to state.bestStreak.toString(),
            stringResource(R.string.stat_total_score) to state.totalScore.toString(),
        ),
        primaryLabel = stringResource(R.string.play_again),
        onPrimary = onPlayAgain,
        secondaryLabel = stringResource(R.string.go_home),
        onSecondary = onHome,
    )
}

/** A white sticker card: fat coloured outline, one big emoji, and pill buttons. */
@Composable
private fun ResultCard(
    emoji: String,
    title: String,
    accent: Color,
    stats: List<Pair<String, String>>,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier = Modifier
            .widthIn(max = 350.dp)
            .clip(shape)
            .background(CardWhite)
            .border(4.dp, accent, shape)
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, fontSize = 40.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Navy,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        stats.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardCream)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                Text(value, style = MaterialTheme.typography.titleMedium, color = Navy)
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Navy),
        ) {
            Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
        }
        TextButton(
            onClick = onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text(secondaryLabel, color = NavySoft, style = MaterialTheme.typography.labelLarge)
        }
    }
}
