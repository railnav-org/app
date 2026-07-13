package com.app.railnav.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.railnav.data.TrainDirection
import com.app.railnav.data.TrainRepository
import com.app.railnav.data.TrainSchedule

// ─────────────────────────────────────────────────────────────────────────────
//  Mini Persistent Live Board (Peek State)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniLiveBoardCard(
    currentMinutes: Int,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onTrainSelected: (TrainSchedule) -> Unit, // <-- NEW PARAMETER
    modifier: Modifier = Modifier
) {
    fun getNextTrain(dir: TrainDirection): TrainSchedule? {
        return TrainRepository.schedule
            .filter { it.direction == dir }
            .map { train ->
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440
                train to diff
            }
            .filter { it.second >= -1 }
            .minByOrNull { it.second }
            ?.first
    }

    val upTrain = remember(currentMinutes) { getNextTrain(TrainDirection.UP) }
    val downTrain = remember(currentMinutes) { getNextTrain(TrainDirection.DOWN) }
    val harbourTrain = remember(currentMinutes) { getNextTrain(TrainDirection.HARBOUR) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            // FIX: Removed the ugly 'androidx...' prefixes since we imported them properly!
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -10) onExpand()
                    else if (dragAmount > 15) onClose()
                }
            },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Pull up",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Next Departures",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Board")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTrainBox(
                    TrainDirection.UP,
                    upTrain,
                    currentMinutes,
                    onTrainSelected,
                    Modifier.weight(1f)
                )
                MiniTrainBox(
                    TrainDirection.DOWN,
                    downTrain,
                    currentMinutes,
                    onTrainSelected,
                    Modifier.weight(1f)
                )
                MiniTrainBox(
                    TrainDirection.HARBOUR,
                    harbourTrain,
                    currentMinutes,
                    onTrainSelected,
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun MiniTrainBox(
    direction: TrainDirection,
    train: TrainSchedule?,
    currentMinutes: Int,
    onClick: (TrainSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (train != null) {
        Color(train.type.color).copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable(enabled = train != null) { train?.let { onClick(it) } }
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            if (train != null) {
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440

                val statusColor =
                    if (diff <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                val typeColor = Color(train.type.color)

                // ── Top row: Destination + PF ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = train.destination,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "PF ${train.platformAtThane}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(2.dp))

                // ── Middle: Departure time ──
                Text(
                    train.departureTimeString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // ── Bottom row: Type badge + Timer ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = train.type.displayName,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (diff <= 0) {
                        Text(
                            "Now",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = statusColor
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "${diff} min",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                // ── Empty state ──
                Text(
                    text = when (direction) {
                        TrainDirection.UP -> "UP"
                        TrainDirection.DOWN -> "DOWN"
                        TrainDirection.HARBOUR -> "HARBOUR"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No trains",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
