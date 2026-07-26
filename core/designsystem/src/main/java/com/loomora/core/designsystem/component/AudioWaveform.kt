package com.loomora.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loomora.core.designsystem.theme.LoomoraTheme

@Composable
fun AudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color = LoomoraTheme.extraColors.waveformActive,
    inactiveColor: Color = LoomoraTheme.extraColors.waveformInactive
) {
    val barWidth = 4.dp
    val barGap = 3.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
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

            drawRect(
                color = if (amp > 0.05f) activeColor else inactiveColor,
                topLeft = Offset(x, top),
                size = Size(barWidth.toPx(), barHeight)
            )
        }
    }
}
