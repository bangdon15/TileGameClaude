package com.claudetest.matchtiles.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudetest.matchtiles.R
import com.claudetest.matchtiles.game.Symbols
import com.claudetest.matchtiles.ui.theme.CardWhite
import com.claudetest.matchtiles.ui.theme.Navy
import com.claudetest.matchtiles.ui.theme.NavySoft
import kotlinx.coroutines.delay

/** Four faces bounce in, the title lands, then the menu takes over. */
private const val SPLASH_HOLD_MS = 900L
private const val TILE_STAGGER_MS = 110L
private val SPLASH_FACES = listOf(0, 10, 17, 33)

/**
 * The opening logo screen.
 *
 * Hand-rolled rather than built on the platform splash-screen library, which this project
 * cannot fetch while building offline. The window background is already the same sky blue,
 * so there is no white flash before this draws. A tap anywhere skips ahead.
 */
@Composable
fun SplashScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val pops = SPLASH_FACES.map { remember { Animatable(0f) } }
    val title = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pops.forEachIndexed { index, pop ->
            delay(if (index == 0) 60L else TILE_STAGGER_MS)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
        }
        title.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        delay(SPLASH_HOLD_MS)
        onDone()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBrush)
            .clickable(onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SPLASH_FACES.forEachIndexed { index, symbol ->
                    val pop = pops[index].value
                    LogoTile(
                        symbol = symbol,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                            alpha = pop
                            rotationZ = (1f - pop) * if (index % 2 == 0) -18f else 18f
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = Navy,
                modifier = Modifier
                    .padding(top = 26.dp)
                    .graphicsLayer { alpha = title.value },
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = NavySoft,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .graphicsLayer { alpha = title.value },
            )
        }
    }
}

/** One logo tile: the same white sticker as the board, at a fixed size. */
@Composable
internal fun LogoTile(symbol: Int, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val face = Symbols[symbol]
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(CardWhite)
            .border(4.dp, Color(face.accent), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = face.glyph, fontSize = size.value.times(0.47f).sp)
    }
}
