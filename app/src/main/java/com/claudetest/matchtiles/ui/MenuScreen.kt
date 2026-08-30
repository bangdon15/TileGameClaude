package com.claudetest.matchtiles.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.claudetest.matchtiles.R
import com.claudetest.matchtiles.audio.LocalAudio
import com.claudetest.matchtiles.audio.Sfx
import com.claudetest.matchtiles.model.LevelPlan
import com.claudetest.matchtiles.ui.theme.CardCream
import com.claudetest.matchtiles.ui.theme.CardWhite
import com.claudetest.matchtiles.ui.theme.Grape
import com.claudetest.matchtiles.ui.theme.Mint
import com.claudetest.matchtiles.ui.theme.Navy
import com.claudetest.matchtiles.ui.theme.NavyFaint
import com.claudetest.matchtiles.ui.theme.NavySoft

/** Four faces along the top of the menu, there purely to look inviting. */
private val MENU_FACES = listOf(18, 28, 25, 31)

/**
 * The home screen: one big Play button in the middle, the two sound switches behind the cog
 * in the top right, and the way out on the left.
 */
@Composable
fun MenuScreen(
    musicOn: Boolean,
    soundOn: Boolean,
    onMusicChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var settingsOpen by remember { mutableStateOf(false) }
    val audio = LocalAudio.current

    Box(modifier = modifier.fillMaxSize().background(BackgroundBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ExitPill(onClick = {
                    audio.play(Sfx.Button)
                    onExit()
                })
                Spacer(Modifier.weight(1f))
                GlyphButton(
                    glyph = "⚙",
                    description = stringResource(R.string.cd_settings),
                    onClick = {
                        audio.play(Sfx.Button)
                        settingsOpen = true
                    },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MENU_FACES.forEach { LogoTile(symbol = it, size = 54.dp) }
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = Navy,
                    modifier = Modifier.padding(top = 22.dp),
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavySoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                PlayButton(
                    onClick = {
                        audio.play(Sfx.Button)
                        onPlay()
                    },
                    modifier = Modifier.padding(top = 34.dp),
                )
                Text(
                    text = stringResource(R.string.menu_hint, LevelPlan.LAST_LEVEL),
                    style = MaterialTheme.typography.labelSmall,
                    color = NavySoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }

        if (settingsOpen) {
            SoundSettingsDialog(
                musicOn = musicOn,
                soundOn = soundOn,
                onMusicChange = onMusicChange,
                onSoundChange = onSoundChange,
                onDismiss = { settingsOpen = false },
            )
        }
    }
}

/** The one thing on this screen a child needs to find, so it is huge and it breathes. */
@Composable
private fun PlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "playPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "playScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .width(252.dp)
            .height(78.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            },
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Navy),
    ) {
        Text(
            text = "▶  ${stringResource(R.string.menu_play)}",
            style = MaterialTheme.typography.displaySmall,
            color = Navy,
        )
    }
}

@Composable
private fun ExitPill(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(CardWhite)
            .border(2.dp, Navy.copy(alpha = 0.10f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🚪", style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.menu_exit),
            style = MaterialTheme.typography.labelLarge,
            color = NavySoft,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Exactly two switches: the music loop, and the sounds the interface makes. */
@Composable
private fun SoundSettingsDialog(
    musicOn: Boolean,
    soundOn: Boolean,
    onMusicChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(shape)
                .background(CardWhite)
                .border(4.dp, Grape, shape)
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Text(
                text = "⚙ ${stringResource(R.string.settings_title)}",
                style = MaterialTheme.typography.titleLarge,
                color = Navy,
            )
            SettingRow(stringResource(R.string.settings_music), musicOn, onMusicChange)
            SettingRow(stringResource(R.string.settings_sound), soundOn, onSoundChange)
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Navy),
            ) {
                Text(
                    text = stringResource(R.string.settings_done),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardCream)
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Navy)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CardWhite,
                checkedTrackColor = Mint,
                checkedBorderColor = Mint,
                uncheckedThumbColor = CardWhite,
                uncheckedTrackColor = NavyFaint,
                uncheckedBorderColor = NavySoft.copy(alpha = 0.3f),
            ),
        )
    }
}
