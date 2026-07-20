package com.app.railnav.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Map controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapControls(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onMyLocationClick: () -> Unit,
    onSwapNodes: () -> Unit,
    onFacilities: () -> Unit,
    showSwap: Boolean,
    isAccessiblePreferred: Boolean,
    onToggleAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapControlButton(
            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            onClick = onToggleTheme
        )

        FloatingActionButton(
            onClick = onToggleAccessibility,
            modifier = Modifier.size(48.dp),
            containerColor = if (isAccessiblePreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Accessible,
                contentDescription = "Accessible Route Only",
                tint = if (isAccessiblePreferred) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }

        MapControlButton(
            icon = Icons.Default.MedicalServices,
            onClick = onFacilities
        )
        if (showSwap) {
            MapControlButton(
                icon = Icons.Default.SwapVert,
                onClick = onSwapNodes
            )
        }
        MapControlButton(
            icon = Icons.Default.MyLocation,
            onClick = onMyLocationClick
        )
    }
}

@Composable
fun MapControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}
