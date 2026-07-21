package com.app.railnav.data

import android.content.Context
import com.app.railnav.data.local.EdgeEntity
import com.app.railnav.data.local.NodeEntity
import com.app.railnav.data.local.RailNavDatabase
import com.app.railnav.data.local.StationHubEntity
import com.app.railnav.data.remote.SupabaseClient
import com.app.railnav.data.remote.SupabaseConfig

/**
 * Repository for station graph data (nodes + edges).
 *
 * Supports two data sources:
 * 1. **Network** — On-demand streaming from Supabase PostGIS (primary)
 * 2. **Room cache** — Local SQLite database for offline fallback
 */
object GraphRepository {

    // ══════════════════════════════════════════════════════════════════
    //  Cache-first loading (reads from Room)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Load all cached nodes from Room (all station hubs combined).
     * This is the backwards-compatible fallback used by the ViewModel.
     */
    suspend fun loadNodes(context: Context): List<NodeFeature> {
        val db = RailNavDatabase.getDatabase(context)
        val entities = db.graphDao().getAllNodes()

        return entities.map { entity ->
            NodeFeature(
                properties = NodeProperties(
                    node_id = entity.id,
                    node_name = entity.name,
                    node_type = entity.type,
                    node_level = entity.level,
                    note = null
                ),
                geometry = NodeGeometry(listOf(entity.lon, entity.lat))
            )
        }
    }

    /**
     * Load all cached edges from Room (all station hubs combined).
     */
    suspend fun loadEdges(context: Context): List<EdgeFeature> {
        val db = RailNavDatabase.getDatabase(context)
        val entities = db.graphDao().getAllEdges()

        return entities.map { entity ->
            EdgeFeature(
                properties = EdgeProperties(
                    edge_id = entity.edgeId,
                    start_id = entity.startNodeId.toString(),
                    end_id = entity.endNodeId.toString(),
                    edge_distance = entity.distance.toString(),
                    edge_type = entity.edgeType,
                    edge_level = null,
                    edge_accessibilty = null
                ),
                geometry = EdgeGeometry(entity.geometry)
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  On-demand network streaming (from Supabase PostGIS)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Result of a station data fetch — either from network or cache.
     */
    data class StationDataResult(
        val nodes: List<NodeFeature>,
        val edges: List<EdgeFeature>,
        val stationName: String?,
        val fromCache: Boolean
    )

    /**
     * Orchestrator: Loads station data for the user's current location.
     *
     * 1. Checks if the nearest station hub is already cached in Room.
     * 2. If cached, reads from Room (offline-friendly).
     * 3. If not cached, fetches from Supabase PostGIS and caches in Room.
     *
     * @param context Application context for Room database access
     * @param lat User's current latitude
     * @param lon User's current longitude
     * @param hubId Optional: If known (from geofence trigger), skip proximity search
     * @param stationName Optional: Station name from geofence trigger
     * @return [StationDataResult] with the graph data, or null on failure
     */
    suspend fun loadStationData(
        context: Context,
        lat: Double,
        lon: Double,
        hubId: Int? = null,
        stationName: String? = null
    ): StationDataResult? {
        val db = RailNavDatabase.getDatabase(context)
        val dao = db.graphDao()

        // If we have a specific hub ID (from geofence), check if it's cached
        if (hubId != null && dao.isStationHubCached(hubId)) {
            val cachedNodes = dao.getNodesByStationHub(hubId)
            val cachedEdges = dao.getEdgesByStationHub(hubId)

            if (cachedNodes.isNotEmpty()) {
                return StationDataResult(
                    nodes = cachedNodes.map { it.toNodeFeature() },
                    edges = cachedEdges.map { it.toEdgeFeature() },
                    stationName = stationName,
                    fromCache = true
                )
            }
        }

        // Fetch from Supabase
        val response = SupabaseClient.fetchStationData(
            lat = lat,
            lon = lon,
            radiusMeters = SupabaseConfig.DEFAULT_GEOFENCE_RADIUS_METERS
        ) ?: return null

        val remoteNodes = response.nodes ?: emptyList()
        val remoteEdges = response.edges ?: emptyList()

        if (remoteNodes.isEmpty()) return null

        // Determine the hub ID (use the first node's hub, or the provided one)
        val resolvedHubId = hubId ?: remoteNodes.firstOrNull()?.stationHubId ?: 0

        // Convert and insert into Room cache
        val nodeEntities = remoteNodes.map { remote ->
            NodeEntity(
                id = remote.nodeId,
                name = remote.nodeName,
                type = remote.nodeType,
                level = remote.nodeLevel,
                lat = remote.lat,
                lon = remote.lon,
                stationHubId = remote.stationHubId ?: resolvedHubId
            )
        }

        val edgeEntities = remoteEdges.map { remote ->
            EdgeEntity(
                edgeId = remote.edgeId,
                startNodeId = remote.startNodeId,
                endNodeId = remote.endNodeId,
                distance = remote.distance,
                edgeType = remote.edgeType,
                geometry = remote.geometry,
                stationHubId = remote.stationHubId ?: resolvedHubId
            )
        }

        // Write to Room
        dao.insertNodes(nodeEntities)
        dao.insertEdges(edgeEntities)
        dao.insertStationHub(
            StationHubEntity(
                hubId = resolvedHubId,
                stationName = stationName ?: "Station $resolvedHubId",
                lat = lat,
                lon = lon
            )
        )

        // Convert to domain models
        val nodeFeatures = nodeEntities.map { it.toNodeFeature() }
        val edgeFeatures = edgeEntities.map { it.toEdgeFeature() }

        return StationDataResult(
            nodes = nodeFeatures,
            edges = edgeFeatures,
            stationName = stationName,
            fromCache = false
        )
    }

    // ══════════════════════════════════════════════════════════════════
    //  Extension functions for entity → domain model conversion
    // ══════════════════════════════════════════════════════════════════

    private fun NodeEntity.toNodeFeature(): NodeFeature {
        return NodeFeature(
            properties = NodeProperties(
                node_id = id,
                node_name = name,
                node_type = type,
                node_level = level,
                note = null
            ),
            geometry = NodeGeometry(listOf(lon, lat))
        )
    }

    private fun EdgeEntity.toEdgeFeature(): EdgeFeature {
        return EdgeFeature(
            properties = EdgeProperties(
                edge_id = edgeId,
                start_id = startNodeId.toString(),
                end_id = endNodeId.toString(),
                edge_distance = distance.toString(),
                edge_type = edgeType,
                edge_level = null,
                edge_accessibilty = null
            ),
            geometry = EdgeGeometry(geometry)
        )
    }
}