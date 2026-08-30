package com.claudetest.matchtiles.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.claudetest.matchtiles.ui.theme.Coral
import com.claudetest.matchtiles.ui.theme.Grape
import com.claudetest.matchtiles.ui.theme.Leaf
import com.claudetest.matchtiles.ui.theme.Mint
import com.claudetest.matchtiles.ui.theme.Ocean
import com.claudetest.matchtiles.ui.theme.Sunny
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val FALL_MS = 2200

private val Palette = listOf(Sunny, Coral, Mint, Ocean, Grape, Leaf)

private class Piece(
    val x: Float,
    val size: Float,
    val color: Color,
    val delay: Float,
    val sway: Float,
    val phase: Float,
    val round: Boolean,
)

/**
 * One shower of paper for a cleared level. Deliberately a single pass rather than a loop:
 * a reward should land and stop, not animate behind the card forever.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 34) {
    val pieces = remember {
        val random = Random(0xC0FFEE)
        List(count) {
            Piece(
                x = random.nextFloat(),
                size = 8f + random.nextFloat() * 10f,
                color = Palette[random.nextInt(Palette.size)],
                delay = random.nextFloat() * 0.45f,
                sway = 10f + random.nextFloat() * 26f,
                phase = random.nextFloat() * 6f,
                round = random.nextBoolean(),
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(FALL_MS, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        pieces.forEach { piece ->
            val travelled = ((progress.value - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (travelled <= 0f) return@forEach

            val px = piece.size.dp.toPx()
            val y = -px + travelled * (size.height + 2f * px)
            val x = piece.x * size.width +
                sin(travelled * PI.toFloat() * 3f + piece.phase) * piece.sway.dp.toPx()
            val alpha = if (travelled > 0.85f) 1f - (travelled - 0.85f) / 0.15f else 1f
            val color = piece.color.copy(alpha = alpha.coerceIn(0f, 1f))

            if (piece.round) {
                drawCircle(color = color, radius = px / 2f, center = Offset(x, y))
            } else {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - px / 2f, y - px / 2f),
                    size = Size(px, px * 0.62f),
                    cornerRadius = CornerRadius(px * 0.2f),
                )
            }
        }
    }
}
