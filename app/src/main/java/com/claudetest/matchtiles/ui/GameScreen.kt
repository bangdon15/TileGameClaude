package com.claudetest.matchtiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudetest.matchtiles.R
import com.claudetest.matchtiles.audio.LocalAudio
import com.claudetest.matchtiles.audio.Sfx
import com.claudetest.matchtiles.game.GameViewModel
import com.claudetest.matchtiles.model.GameEvent
import com.claudetest.matchtiles.model.GameUiState
import com.claudetest.matchtiles.model.Phase
import com.claudetest.matchtiles.ui.theme.CardWhite
import com.claudetest.matchtiles.ui.theme.Cream
import com.claudetest.matchtiles.ui.theme.Navy
import com.claudetest.matchtiles.ui.theme.SkyMid
import com.claudetest.matchtiles.ui.theme.SkyTop

/**
 * The game itself. [onQuit] backs out to the home screen, which the house button in the
 * HUD and the buttons on the finished-run cards both use.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The rules post events; here is where they turn into noise.
    val audio = LocalAudio.current
    LaunchedEffect(viewModel, audio) {
        viewModel.events.collect { event -> audio.play(event.toSfx()) }
    }

    GameScreenContent(
        state = state,
        onTileTapped = viewModel::onTileTapped,
        onNext = viewModel::nextLevel,
        onRetry = viewModel::retryLevel,
        onNewGame = viewModel::newGame,
        onShuffle = viewModel::shuffleBoard,
        onQuit = onQuit,
        onBoardShown = viewModel::onBoardShown,
        modifier = modifier,
    )
}

private fun GameEvent.toSfx(): Sfx = when (this) {
    GameEvent.Flip -> Sfx.Tap
    GameEvent.Match -> Sfx.Match
    GameEvent.Miss -> Sfx.Miss
    GameEvent.Shuffle -> Sfx.Shuffle
    GameEvent.LevelClear -> Sfx.Clear
    GameEvent.GameOver -> Sfx.Over
    GameEvent.GameWon -> Sfx.Win
}

@Composable
fun GameScreenContent(
    state: GameUiState,
    onTileTapped: (Int) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onNewGame: () -> Unit,
    onShuffle: () -> Unit = {},
    onQuit: () -> Unit = {},
    onBoardShown: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // The result card keeps rendering while it fades out, by which time the game state has
    // already moved on to the next level. Freeze the state it was built from.
    var resultSnapshot by remember { mutableStateOf<GameUiState?>(null) }
    LaunchedEffect(state.phase) {
        if (state.phase != Phase.Preview && state.phase != Phase.Playing) {
            resultSnapshot = state
        }
    }
    val shown = resultSnapshot ?: state

    Box(modifier = modifier.fillMaxSize().background(BackgroundBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Hud(
                state = state,
                onRestart = onRetry,
                onQuit = onQuit,
                onShuffle = onShuffle,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Board(state = state, onTileTapped = onTileTapped, onBoardShown = onBoardShown)
            }

            AnimatedVisibility(
                visible = state.phase == Phase.Preview,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val shuffled = state.lookMillis >= GameViewModel.SHUFFLE_LOOK_MS
                    Text(
                        text = if (shuffled) {
                            stringResource(R.string.shuffled_look)
                        } else {
                            stringResource(R.string.memorize)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Navy,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CardWhite)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.phase == Phase.LevelClear,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scrim { LevelClearCard(state = shown, onNext = onNext, onRetry = onRetry) }
                Confetti(modifier = Modifier.fillMaxSize())
            }
        }

        AnimatedVisibility(
            visible = state.phase == Phase.GameOver,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
        ) {
            Scrim { GameOverCard(state = shown, onRetry = onRetry, onNewGame = onNewGame) }
        }

        AnimatedVisibility(
            visible = state.phase == Phase.GameWon,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scrim { ThankYouCard(state = shown, onPlayAgain = onNewGame, onHome = onQuit) }
                Confetti(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy.copy(alpha = 0.45f))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Sky at the top, warm sand at the bottom: the board sits outdoors on a bright day. */
internal val BackgroundBrush = Brush.verticalGradient(listOf(SkyTop, SkyMid, Cream))
