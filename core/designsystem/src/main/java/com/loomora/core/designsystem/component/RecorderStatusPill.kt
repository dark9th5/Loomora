package com.loomora.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.loomora.core.designsystem.theme.LoomoraTheme

enum class StatusPillType {
    PREPARING,
    RECORDING,
    PAUSED,
    FINALIZING
}

private data class StatusData(
    val backgroundColor: Color,
    val contentColor: Color,
    val icon: ImageVector
)

@Composable
fun RecorderStatusPill(
    type: StatusPillType,
    label: String,
    modifier: Modifier = Modifier
) {
    val statusData = when (type) {
        StatusPillType.PREPARING -> StatusData(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.HourglassEmpty
        )
        StatusPillType.RECORDING -> StatusData(
            backgroundColor = LoomoraTheme.extraColors.recordingContainer,
            contentColor = LoomoraTheme.extraColors.recording,
            icon = Icons.Default.FiberManualRecord
        )
        StatusPillType.PAUSED -> StatusData(
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary,
            icon = Icons.Default.Pause
        )
        StatusPillType.FINALIZING -> StatusData(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.HourglassEmpty
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusData.backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusData.icon,
            contentDescription = null,
            tint = statusData.contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = statusData.contentColor
        )
    }
}
