package com.claudetest.matchtiles

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.claudetest.matchtiles.audio.GameAudio
import com.claudetest.matchtiles.data.SettingsStore
import com.claudetest.matchtiles.ui.MatchTilesApp
import com.claudetest.matchtiles.ui.theme.TileMatchTheme

class MainActivity : ComponentActivity() {

    // Audio outlives composition but not the activity: it holds a SoundPool and a
    // MediaPlayer, both of which have to be released by hand.
    private lateinit var audio: GameAudio
    private lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        // The game is always a bright daytime screen, so pin dark system icons instead of
        // letting the device's dark mode decide.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        audio = GameAudio(this)
        settings = SettingsStore(this)
        setContent {
            TileMatchTheme {
                MatchTilesApp(
                    audio = audio,
                    settings = settings,
                    onExit = { finish() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        audio.onForeground()
    }

    /** Nothing of ours should be heard once the player has moved on to another app. */
    override fun onStop() {
        super.onStop()
        audio.onBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        audio.release()
    }
}
