package com.app.railnav.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.railnav.data.NodeFeature
import org.osmdroid.util.GeoPoint

// ─────────────────────────────────────────────────────────────────────────────
//  Search results list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchResultsList(
    results: List<NodeFeature>,
    userLocation: GeoPoint?,
    onSelect: (NodeFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(results) { node ->
                SearchResultItem(
                    node = node,
                    userLocation = userLocation,
                    onClick = { onSelect(node) }
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    node: NodeFeature,
    userLocation: GeoPoint?,
    onClick: () -> Unit
) {
    val distance = userLocation?.let {
        val pt = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
        it.distanceToAsDouble(pt).toInt()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(node.properties.node_name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                node.properties.node_type?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                node.properties.note?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            distance?.let {
                Text(
                    "~${it}m",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
