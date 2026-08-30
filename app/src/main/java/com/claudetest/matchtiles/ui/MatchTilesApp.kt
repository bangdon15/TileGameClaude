package com.claudetest.matchtiles.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudetest.matchtiles.audio.GameAudio
import com.claudetest.matchtiles.audio.LocalAudio
import com.claudetest.matchtiles.data.SettingsStore
import com.claudetest.matchtiles.game.GameViewModel

/** The three places the app can be. Few enough that a navigation library would be all cost. */
enum class Screen { Splash, Menu, Game }

/**
 * Ties the app together: which screen is showing, where the sound settings live, and the
 * single [GameViewModel] that survives a trip out to the menu and back.
 *
 * [onExit] closes the app, which only the menu's exit button asks for.
 */
@Composable
fun MatchTilesApp(
    audio: GameAudio,
    settings: SettingsStore,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val musicOn by settings.musicOn.collectAsStateWithLifecycle()
    val soundOn by settings.soundOn.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.Splash) }
    val game: GameViewModel = viewModel()

    // The switches are the source of truth; the player hears the result on the next frame.
    LaunchedEffect(musicOn) { audio.setMusicEnabled(musicOn) }
    LaunchedEffect(soundOn) { audio.soundEnabled = soundOn }

    // Back out of a game to the menu rather than out of the app.
    BackHandler(enabled = screen == Screen.Game) { screen = Screen.Menu }

    CompositionLocalProvider(LocalAudio provides audio) {
        when (screen) {
            Screen.Splash -> SplashScreen(
                onDone = { screen = Screen.Menu },
                modifier = modifier,
            )

            Screen.Menu -> MenuScreen(
                musicOn = musicOn,
                soundOn = soundOn,
                onMusicChange = settings::setMusicOn,
                onSoundChange = settings::setSoundOn,
                onPlay = {
                    // Play always means a fresh run from level 1.
                    game.newGame()
                    screen = Screen.Game
                },
                onExit = onExit,
                modifier = modifier,
            )

            Screen.Game -> GameScreen(
                viewModel = game,
                onQuit = { screen = Screen.Menu },
                modifier = modifier,
            )
        }
    }
}
