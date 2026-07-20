package com.app.railnav.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.railnav.data.NodeFeature
import org.osmdroid.util.GeoPoint

// ─────────────────────────────────────────────────────────────────────────────
//  Location permission dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LocationPermissionDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOff, null) },
        title = { Text("Location Permission") },
        text = { Text("Grant location to auto-detect your position, or tap the map manually to set your start point.") },
        confirmButton = { TextButton(onClick = onGrant) { Text("Grant Permission") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Use Map Instead") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Node selection dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NodeSelectionDialog(
    nearestNodes: List<NodeFeature>,
    userLocation: GeoPoint?,
    onNodeSelected: (NodeFeature) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Choose Your Starting Point", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("We found these nearby locations:", Modifier.padding(bottom = 12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(nearestNodes) { node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNodeSelected(node) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        node.properties.node_name ?: "Unknown",
                                        fontWeight = FontWeight.Bold
                                    )
                                    node.properties.node_type?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    userLocation?.let {
                                        val pt = GeoPoint(
                                            node.geometry.coordinates[1],
                                            node.geometry.coordinates[0]
                                        )
                                        Text(
                                            "~${it.distanceToAsDouble(pt).toInt()}m",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tap Map Instead") } }
    )
}
