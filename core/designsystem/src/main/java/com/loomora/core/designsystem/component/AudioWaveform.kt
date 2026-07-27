package com.loomora.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.loomora.core.designsystem.theme.LoomoraTheme
import kotlin.math.roundToInt

@Composable
fun AudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color = LoomoraTheme.extraColors.waveformActive,
    inactiveColor: Color = LoomoraTheme.extraColors.waveformInactive,
    playedFraction: Float = 0f,
    onSeekFraction: ((Float) -> Unit)? = null
) {
    val barWidth = 4.dp
    val barGap = 3.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(onSeekFraction) {
                if (onSeekFraction == null) {
                    return@pointerInput
                }
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position?.x ?: continue
                        if (event.changes.any { it.pressed }) {
                            val fraction = (position / size.width).coerceIn(0f, 1f)
                            onSeekFraction(fraction)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val totalBars = (width / (barWidth.toPx() + barGap.toPx())).toInt().coerceAtLeast(1)
        val visibleAmplitudes = amplitudes.takeLast(totalBars)

        visibleAmplitudes.forEachIndexed { index, amp ->
            val x = index * (barWidth.toPx() + barGap.toPx())
            val barHeight = (amp * (height * 0.8f)).coerceAtLeast(6.dp.toPx())
            val top = centerY - (barHeight / 2f)
            val progressBarCount = (visibleAmplitudes.size * playedFraction.coerceIn(0f, 1f)).roundToInt()

            drawRect(
                color = if (index < progressBarCount) activeColor else inactiveColor,
                topLeft = Offset(x, top),
                size = Size(barWidth.toPx(), barHeight)
            )
        }
    }
}
