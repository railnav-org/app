package com.app.railnav.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.railnav.MainUiState

// ─────────────────────────────────────────────────────────────────────────────
//  Train destination card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TrainDestinationCard(
    uiState: MainUiState,
    onQueryChange: (String) -> Unit,
    onShowTrains: () -> Unit,
    onFindPath: () -> Unit,
    onSwitchToAdvancedMode: () -> Unit,
    onClearStartNode: () -> Unit,
    onOpenLiveBoard: () -> Unit,
    onClearTrain: () -> Unit,
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger menu icon
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Open menu",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "🚆  Where are you going?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSwitchToAdvancedMode) {
                    Text(
                        "Advanced",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            OutlinedTextField(
                value = uiState.trainDestinationQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Station  (e.g. Kalyan, Kurla, CSMT…)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = { Icon(Icons.Default.Train, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (uiState.trainDestinationQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // FIX: Replaced hardcoded grey with adaptive surfaceVariant
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        uiState.startNode?.properties?.node_name ?: "Tap the map to set start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.startNode == null) MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        ) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.startNode != null) {
                    IconButton(onClick = onClearStartNode, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Clear,
                            "Clear Start Node",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (uiState.selectedTrain != null) {
                val train = uiState.selectedTrain
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowTrains() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${train.departureTimeString}  →  ${train.destination}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${train.type.displayName}  •  Platform ${train.platformAtThane}  •  ${train.trainNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onClearTrain,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Clear train",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { onShowTrains() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Change train",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else if (uiState.selectedDestination != null && uiState.availableTrains.isNotEmpty()) {
                OutlinedButton(
                    onClick = onShowTrains,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose a train to ${uiState.selectedDestination}")
                }
            }

            OutlinedButton(
                onClick = onOpenLiveBoard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.DynamicFeed, null)
                Spacer(Modifier.width(8.dp))
                Text("View Live Station Board", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onFindPath,
                enabled = uiState.startNode != null && uiState.endNode != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(8.dp))
                Text("Navigate to Platform", fontWeight = FontWeight.Bold)
            }
        }
    }
}
