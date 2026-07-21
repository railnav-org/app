package com.app.railnav.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level response from the Supabase `get_station_data` RPC function.
 * Contains the full graph data (nodes + edges) for a single station hub.
 */
@Serializable
data class StationDataResponse(
    val nodes: List<RemoteNode>? = null,
    val edges: List<RemoteEdge>? = null
)

/**
 * A node record as returned by the Supabase PostGIS `nodes` table.
 */
@Serializable
data class RemoteNode(
    @SerialName("node_id") val nodeId: Int,
    @SerialName("node_name") val nodeName: String? = null,
    @SerialName("node_type") val nodeType: String? = null,
    @SerialName("node_level") val nodeLevel: Int = 0,
    val lat: Double,
    val lon: Double,
    @SerialName("station_hub_id") val stationHubId: Int? = null
)

/**
 * An edge record as returned by the Supabase PostGIS `edges` table.
 */
@Serializable
data class RemoteEdge(
    @SerialName("edge_id") val edgeId: String,
    @SerialName("start_node_id") val startNodeId: Int,
    @SerialName("end_node_id") val endNodeId: Int,
    val distance: Double = 0.0,
    @SerialName("edge_type") val edgeType: String? = null,
    val geometry: List<List<Double>>, // LineString coordinates
    @SerialName("station_hub_id") val stationHubId: Int? = null
)

/**
 * Represents a station hub entry from the `station_hubs` table.
 * Used to register geofences around known transit hubs.
 */
@Serializable
data class RemoteStationHub(
    val id: Int,
    @SerialName("station_name") val stationName: String,
    val lat: Double,
    val lon: Double,
    @SerialName("geofence_radius_meters") val geofenceRadiusMeters: Double = 500.0
)
