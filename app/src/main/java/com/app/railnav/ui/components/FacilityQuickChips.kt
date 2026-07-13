package com.app.railnav.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Facility Quick Chips
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityQuickChips(onChipSelected: (String) -> Unit) {
    val facilities = listOf(
        Pair("Ticket", Icons.Default.ConfirmationNumber),
        Pair("Exit", Icons.AutoMirrored.Filled.ExitToApp),
        Pair("Toilet", Icons.Default.Wc),
        Pair("Stairs", Icons.Default.Stairs)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 72.dp, top = 0.dp, bottom = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(facilities) { (name, icon) ->
            ElevatedFilterChip(
                selected = false,
                onClick = { onChipSelected(name) },
                label = { Text(name, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = name,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.primary,
                    iconColor = MaterialTheme.colorScheme.primary
                ),
                elevation = FilterChipDefaults.filterChipElevation(elevation = 4.dp)
            )
        }
    }
}
