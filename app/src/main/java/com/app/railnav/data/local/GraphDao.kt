package com.app.railnav.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GraphDao {

    // ══════════════════════════════════════════════════════════════════
    //  Insert Methods
    // ══════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<EdgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStationHub(hub: StationHubEntity)

    // ══════════════════════════════════════════════════════════════════
    //  Query Methods — Full table (backwards compatible)
    // ══════════════════════════════════════════════════════════════════

    @Query("SELECT * FROM nodes")
    suspend fun getAllNodes(): List<NodeEntity>

    @Query("SELECT * FROM edges")
    suspend fun getAllEdges(): List<EdgeEntity>

    // ══════════════════════════════════════════════════════════════════
    //  Query Methods — Hub-scoped (for on-demand streaming)
    // ══════════════════════════════════════════════════════════════════

    @Query("SELECT * FROM nodes WHERE stationHubId = :hubId")
    suspend fun getNodesByStationHub(hubId: Int): List<NodeEntity>

    @Query("SELECT * FROM edges WHERE stationHubId = :hubId")
    suspend fun getEdgesByStationHub(hubId: Int): List<EdgeEntity>

    // ══════════════════════════════════════════════════════════════════
    //  Station Hub Cache Management
    // ══════════════════════════════════════════════════════════════════

    @Query("SELECT COUNT(*) > 0 FROM station_hubs WHERE hubId = :hubId")
    suspend fun isStationHubCached(hubId: Int): Boolean

    @Query("SELECT * FROM station_hubs")
    suspend fun getAllStationHubs(): List<StationHubEntity>

    @Query("SELECT lastSyncedAt FROM station_hubs WHERE hubId = :hubId")
    suspend fun getHubLastSyncTime(hubId: Int): Long?

    // ══════════════════════════════════════════════════════════════════
    //  Search Method (unchanged from original)
    // ══════════════════════════════════════════════════════════════════

    // Finds nodes where the name OR the type matches the search query.
    @Query(
        """
        SELECT * FROM nodes 
        WHERE name LIKE '%' || :query || '%' 
        OR type LIKE '%' || :query || '%'
    """
    )
    suspend fun searchNodes(query: String): List<NodeEntity>

    // ══════════════════════════════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════════════════════════════

    // Check if DB is empty to know if we need to seed data
    @Query("SELECT COUNT(*) FROM nodes")
    suspend fun getNodeCount(): Int
}