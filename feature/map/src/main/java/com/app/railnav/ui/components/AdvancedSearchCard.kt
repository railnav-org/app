package com.app.railnav.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.railnav.MainUiState
import com.app.railnav.MainViewModel
import com.app.railnav.SelectionField
import com.app.railnav.data.NodeFeature

// ─────────────────────────────────────────────────────────────────────────────
//  Advanced mode card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdvancedSearchCard(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onStartNodeSelected: (NodeFeature) -> Unit,
    onEndNodeSelected: (NodeFeature) -> Unit,
    onFindPath: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSwitchToSimpleMode: () -> Unit,
    onClearStartNode: () -> Unit,
    onClearEndNode: () -> Unit,
    mainViewModel: MainViewModel // Pass the viewModel to trigger the focus state
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
                Text("Manual node selection", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onSwitchToSimpleMode) {
                    Text("← Back", style = MaterialTheme.typography.labelSmall)
                }
            }

            ModernNodeSelector(
                label = "Start Point",
                icon = Icons.Default.LocationOn,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.startNode,
                isActive = uiState.activeSelectionField == SelectionField.START, // NEW
                onActiveClick = { mainViewModel.setActiveSelectionField(SelectionField.START) }, // NEW
                onNodeSelected = onStartNodeSelected,
                onClearSelection = onClearStartNode,
                iconTint = MaterialTheme.colorScheme.primary
            )
            ModernNodeSelector(
                label = "Destination",
                icon = Icons.Default.Flag,
                nodes = uiState.allNodeFeatures,
                selectedNode = uiState.endNode,
                isActive = uiState.activeSelectionField == SelectionField.END, // NEW
                onActiveClick = { mainViewModel.setActiveSelectionField(SelectionField.END) }, // NEW
                onNodeSelected = onEndNodeSelected,
                onClearSelection = onClearEndNode,
                iconTint = MaterialTheme.colorScheme.error
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search node…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

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
                Text("Find Route", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Modern node selector dropdown
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNodeSelector(
    label: String,
    icon: ImageVector,
    nodes: List<NodeFeature>,
    selectedNode: NodeFeature?,
    isActive: Boolean,         // NEW: Tells the UI if this is the focused field
    onActiveClick: () -> Unit, // NEW: Tells ViewModel this field was clicked
    onNodeSelected: (NodeFeature) -> Unit,
    onClearSelection: () -> Unit,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            if (expanded) onActiveClick() // Set focus when user taps the box
        }
    ) {
        OutlinedTextField(
            value = selectedNode?.properties?.node_name ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(label, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (selectedNode != null) {
                    IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Clear,
                            "Clear Selection",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            // Visually highlight the box if it is the currently active map-tap target
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                unfocusedContainerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // FIX: Prevent the dropdown from taking over the entire screen!
            modifier = Modifier.heightIn(max = 250.dp)
        ) {
            nodes.forEach { node ->
                DropdownMenuItem(
                    text = { Text(node.properties.node_name ?: "Unnamed") },
                    onClick = {
                        onActiveClick() // Ensure focus is set
                        onNodeSelected(node)
                        expanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(20.dp)) }
                )
            }
        }
    }
}
