package com.claudetest.matchtiles.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.claudetest.matchtiles.game.TileFace
import com.claudetest.matchtiles.model.Tile
import com.claudetest.matchtiles.model.TileState
import com.claudetest.matchtiles.ui.theme.CardWhite
import com.claudetest.matchtiles.ui.theme.Coral
import com.claudetest.matchtiles.ui.theme.Grape
import com.claudetest.matchtiles.ui.theme.Ocean
import kotlin.math.PI
import kotlin.math.sin

private const val FLIP_MS = 340
private const val APPEAR_MS = 320
private const val SHAKE_MS = 360
private const val POP_MS = 150

/**
 * A single board tile, drawn as a chunky sticker: a fat white outline on the candy-coloured
 * back, a fat coloured outline on the white front. The flip is a real Y-axis rotation - the
 * back is drawn below 90°, the front above it, counter-rotated so the glyph is not mirrored.
 *
 * Callers should wrap this in `key(level, tile.id)` so the entrance animation replays
 * when a new board is dealt. [onAppeared] fires once this tile has finished washing in.
 */
@Composable
fun TileCard(
    tile: Tile,
    face: TileFace,
    revealed: Boolean,
    shaking: Boolean,
    enabled: Boolean,
    size: Dp,
    appearDelayMillis: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAppeared: (() -> Unit)? = null,
) {
    val matched = tile.state == TileState.Matched

    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(FLIP_MS, easing = FastOutSlowInEasing),
        label = "flip",
    )
    val matchedFade by animateFloatAsState(
        targetValue = if (matched) 0.68f else 1f,
        animationSpec = tween(320),
        label = "matchedFade",
    )

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = tween(APPEAR_MS, delayMillis = appearDelayMillis, easing = FastOutSlowInEasing),
        )
        onAppeared?.invoke()
    }

    // A found pair should feel like a prize, so it bounces out before settling back down.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(matched) {
        if (matched) {
            pop.animateTo(1.14f, tween(POP_MS, easing = FastOutSlowInEasing))
            pop.animateTo(0.94f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(shaking) {
        if (shaking) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(SHAKE_MS, easing = LinearEasing))
        } else {
            shake.snapTo(0f)
        }
    }

    val showFront = rotation >= 90f
    val accent = Color(face.accent)
    val corner = RoundedCornerShape(size * 0.26f)
    val density = LocalDensity.current
    val glyphSize = with(density) { (size * 0.48f).toSp() }
    val backGlyphSize = with(density) { (size * 0.34f).toSp() }
    val shakePx = with(density) { 6.dp.toPx() }
    val outline = (size * 0.055f).coerceIn(2.dp, 4.dp)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * this.density
                val scale = pop.value * (0.84f + 0.16f * appear.value)
                scaleX = scale
                scaleY = scale
                translationX = sin(shake.value * PI.toFloat() * 4f) * shakePx
                alpha = matchedFade * appear.value
            }
            .clip(corner)
            .background(if (showFront) frontBrush(accent) else BackBrush)
            .border(
                width = outline,
                color = when {
                    shaking -> Coral
                    showFront -> accent
                    else -> Color.White
                },
                shape = corner,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (showFront) {
            Text(
                text = face.glyph,
                fontSize = glyphSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            )
        } else {
            Text(
                text = "?",
                fontSize = backGlyphSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** White card with a wash of the glyph's own colour, so a face reads as a printed sticker. */
private fun frontBrush(accent: Color) = Brush.verticalGradient(
    listOf(CardWhite, accent.copy(alpha = 0.20f)),
)

private val BackBrush = Brush.verticalGradient(listOf(Ocean, Grape))
