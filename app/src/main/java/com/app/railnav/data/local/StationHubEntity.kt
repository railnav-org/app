package com.app.railnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks which station hubs have been downloaded and cached in Room.
 * Prevents redundant network requests for already-cached stations.
 */
@Entity(tableName = "station_hubs")
data class StationHubEntity(
    @PrimaryKey val hubId: Int,
    val stationName: String,
    val lat: Double,
    val lon: Double,
    val geofenceRadiusMeters: Double = 500.0,
    /** Timestamp of when this hub's data was last downloaded */
    val lastSyncedAt: Long = System.currentTimeMillis()
)
