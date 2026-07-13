package com.app.railnav.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.railnav.data.TrainDirection
import com.app.railnav.data.TrainRepository
import com.app.railnav.data.TrainSchedule

// ─────────────────────────────────────────────────────────────────────────────
//  Live Train Dashboard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveTrainDashboardSheet(
    currentDirection: TrainDirection,
    currentMinutes: Int,
    currentTimeFormatted: String,
    onDirectionChanged: (TrainDirection) -> Unit,
    onClose: () -> Unit,
    onTrainSelected: (TrainSchedule) -> Unit, // <-- NEW PARAMETER
    modifier: Modifier = Modifier
) {

    val activeTrains = remember(currentDirection, currentMinutes) {
        TrainRepository.schedule
            .filter { it.direction == currentDirection }
            .filter { train ->
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440
                diff > -15
            }
            .sortedBy { train ->
                var diff = train.departureMinutes - currentMinutes
                if (diff < -720) diff += 1440
                diff
            }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        LiveClockHeader(onClose = onClose, currentTimeFormatted)
        Spacer(Modifier.height(16.dp))

        // Direction Switcher
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                TrainDirection.entries.forEach { dir ->
                    val isSelected = dir == currentDirection
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onDirectionChanged(dir) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (dir) {
                                TrainDirection.UP -> "UP"
                                TrainDirection.DOWN -> "DOWN"
                                TrainDirection.HARBOUR -> "HARBOUR"
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeTrains, key = { it.trainNumber }) { train ->
                // FIX: Passed the selection callback down
                LiveTrainCard(
                    train = train,
                    currentMinutes = currentMinutes,
                    onClick = { onTrainSelected(train) })
            }
        }
    }
}

@Composable
fun LiveTrainCard(train: TrainSchedule, currentMinutes: Int, onClick: () -> Unit) {
    var diff = train.departureMinutes - currentMinutes
    if (diff < -720) diff += 1440

    val (statusText, statusColor) = when {
        diff < 0 -> Pair("Departed", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        diff == 0 -> Pair("Now", MaterialTheme.colorScheme.error)
        diff <= 5 -> Pair("in $diff min", MaterialTheme.colorScheme.error)
        else -> Pair("in $diff min", MaterialTheme.colorScheme.primary)
    }

    val typeColor = Color(train.type.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        // FIX: Tint the entire card background to match the train type!
        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(84.dp)) {
                Text(
                    text = train.departureTimeString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = typeColor.copy(alpha = 0.2f), // Boosted to 20%
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = train.type.displayName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = train.destination.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (train.via.isNotEmpty()) {
                    Text(
                        text = "via ${train.via.last()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "PF ${train.platformAtThane}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun LiveClockHeader(onClose: () -> Unit, currentTimeFormatted: String) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "THANE DEPARTURES",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Live • $currentTimeFormatted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Close Board")
        }
    }
}
