package com.app.railnav

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.railnav.data.DirectionGenerator
import com.app.railnav.data.EdgeFeature
import com.app.railnav.data.FacilityItem
import com.app.railnav.data.Graph
import com.app.railnav.data.GraphNode
import com.app.railnav.data.GraphRepository
import com.app.railnav.data.NavigationInstruction
import com.app.railnav.data.NodeFeature
import com.app.railnav.data.Pathfinder
import com.app.railnav.data.TrainDirection
import com.app.railnav.data.TrainRepository
import com.app.railnav.data.TrainSchedule
import com.app.railnav.data.remote.RemoteStationHub
import com.app.railnav.data.remote.SupabaseClient
import com.app.railnav.utils.MapConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class SelectionField { START, END }

data class MainUiState(
    // ── existing graph/navigation state ─────────────────────────────────────
    val allNodeFeatures: List<NodeFeature> = emptyList(),
    val startNode: NodeFeature? = null,
    val endNode: NodeFeature? = null,
    val calculatedPath: List<GraphNode>? = null,
    val instructions: List<NavigationInstruction> = emptyList(),
    val pathBoundingBox: BoundingBox? = null,
    val isLoading: Boolean = true,
    val isAccessibleRoutePreferred: Boolean = false,
    val activeSelectionField: SelectionField = SelectionField.START,
    val currentInstructionIndex: Int = 0,

    // ── legacy manual-node search (used in advanced mode) ───────────────────
    val searchQuery: String = "",
    val searchResults: List<NodeFeature> = emptyList(),

    // ── location ────────────────────────────────────────────────────────────
    val userGpsLocation: GeoPoint? = null,
    val isTrackingModeActive: Boolean = false, // <-- NEW ADDITION
    val nearestNodeCandidates: List<NodeFeature> = emptyList(),
    val showNodeSelectionDialog: Boolean = false,

    // ── train / station mode (Feature 1 & 2) ────────────────────────────────
    /** false = train-destination mode (default);  true = manual node picker */
    val isAdvancedMode: Boolean = false,
    /** What the user is typing in the "Where are you going?" field */
    val trainDestinationQuery: String = "",
    /** Station name suggestions shown as autocomplete */
    val destinationSuggestions: List<String> = emptyList(),
    /** The station the user confirmed */
    val selectedDestination: String? = null,
    /** Upcoming trains for the selected destination */
    val availableTrains: List<TrainSchedule> = emptyList(),
    /** The train the user tapped */
    val selectedTrain: TrainSchedule? = null,
    /** Whether the train-list bottom-sheet is open */
    val showTrainSheet: Boolean = false,

    // ── facilities (Feature 1 – secondary) ──────────────────────────────────
    val showFacilitiesSheet: Boolean = false,

    val totalRouteDistanceMeters: Double = 0.0,
    val etaMinutes: Int = 0,

    // Live Board State
    val showLiveBoardSheet: Boolean = false,
    val liveBoardDirection: TrainDirection = TrainDirection.DOWN,
    val showMiniLiveBoard: Boolean = true,

    // ── on-demand station data streaming ─────────────────────────────────
    /** True while station data is being downloaded from Supabase */
    val isDownloadingStationData: Boolean = false,
    /** Error message if station data download fails */
    val stationDataError: String? = null,
    /** Name of the currently loaded station hub */
    val activeStationName: String? = null,
    /** Whether station graph data has been loaded (from network or cache) */
    val isStationDataLoaded: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _currentMinutes = MutableStateFlow(TrainRepository.currentMinutes())
    val currentMinutes: StateFlow<Int> = _currentMinutes.asStateFlow()

    private val _currentTimeFormatted = MutableStateFlow("")
    val currentTimeFormatted: StateFlow<String> = _currentTimeFormatted.asStateFlow()

    private var forceNextLocationPrompt = false

    private val graph = Graph()
    private lateinit var pathfinder: Pathfinder
    private var allEdges: List<EdgeFeature> = emptyList()

    /** Geofence manager for detecting proximity to station hubs */
    private var geofenceManager: GeofenceManager? = null

    /** Known station hubs fetched from Supabase (for software proximity check) */
    private var knownStationHubs: List<RemoteStationHub> = emptyList()

    /** Track which hub IDs have already been loaded to avoid duplicate fetches */
    private val loadedHubIds = mutableSetOf<Int>()

    companion object {
        private const val TAG = "MainViewModel"
    }

    init {
        // Start with an empty map — data will be loaded on-demand when
        // the user approaches a transit hub (geofence trigger).
        viewModelScope.launch {
            // Try to load any previously cached station data from Room
            val cachedNodes = GraphRepository.loadNodes(getApplication())
            val cachedEdges = GraphRepository.loadEdges(getApplication())

            if (cachedNodes.isNotEmpty()) {
                // We have cached data — build the graph immediately
                allEdges = cachedEdges
                graph.build(cachedNodes, cachedEdges)
                pathfinder = Pathfinder(graph)
                _uiState.value = MainUiState(
                    allNodeFeatures = cachedNodes.sortedBy { it.properties.node_name },
                    isLoading = false,
                    isStationDataLoaded = true,
                    activeStationName = "Cached Station"
                )
            } else {
                // No cached data — show empty map, wait for geofence
                _uiState.value = MainUiState(isLoading = false)
            }

            // Fetch the list of station hubs from Supabase for geofence registration
            fetchStationHubs()
        }

        // Start background timers bound to the ViewModel's lifecycle
        viewModelScope.launch {
            while (true) {
                val cal = java.util.Calendar.getInstance()
                _currentMinutes.value =
                    cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                delay(15000.milliseconds)
            }
        }

        viewModelScope.launch {
            while (true) {
                val cal = java.util.Calendar.getInstance()
                val hh = String.format(
                    java.util.Locale.US,
                    "%02d",
                    cal.get(java.util.Calendar.HOUR_OF_DAY)
                )
                val mm =
                    String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MINUTE))
                val ss =
                    String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.SECOND))
                _currentTimeFormatted.value = "$hh:$mm:$ss"
                delay(1000.milliseconds)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Mode toggle
    // ══════════════════════════════════════════════════════════════════════

    fun toggleAdvancedMode() {
        _uiState.value = _uiState.value.copy(
            isAdvancedMode = !_uiState.value.isAdvancedMode
        )
    }

    fun toggleAccessibilityMode() {
        val currentState = _uiState.value
        val newMode = !currentState.isAccessibleRoutePreferred

        _uiState.value = currentState.copy(isAccessibleRoutePreferred = newMode)

        // If a route is already drawn, instantly recalculate it with the new accessibility rules
        if (currentState.startNode != null && currentState.endNode != null) {
            findPath()
        }
    }

    fun setActiveSelectionField(field: SelectionField) {
        _uiState.value = _uiState.value.copy(activeSelectionField = field)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Train / station destination logic
    // ══════════════════════════════════════════════════════════════════════

    fun onTrainDestinationQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            trainDestinationQuery = query,
            destinationSuggestions = TrainRepository.searchDestinations(query),
            selectedDestination = null,
            availableTrains = emptyList(),
            selectedTrain = null,
            endNode = null
        )
    }

    fun onDestinationSelected(station: String) {
        val trains = TrainRepository.getUpcomingTrains(station)
        _uiState.value = _uiState.value.copy(
            trainDestinationQuery = station,
            destinationSuggestions = emptyList(),
            selectedDestination = station,
            availableTrains = trains,
            showTrainSheet = trains.isNotEmpty(),
            selectedTrain = null,
            endNode = null
        )
    }

    fun onTrainSelected(train: TrainSchedule) {
        val platformNodeId = TrainRepository.getPlatformNodeId(train.platformAtThane)
        val endNode = _uiState.value.allNodeFeatures.find {
            it.properties.node_id == platformNodeId
        }
        _uiState.value = _uiState.value.copy(
            selectedTrain = train,
            endNode = endNode,
            showTrainSheet = false
        )
    }

    fun openTrainSheet() {
        if (_uiState.value.availableTrains.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(showTrainSheet = true)
        }
    }

    fun dismissTrainSheet() {
        _uiState.value = _uiState.value.copy(showTrainSheet = false)
    }

    fun clearSelectedTrain() {
        _uiState.value = _uiState.value.copy(
            selectedTrain = null,
            endNode = null,
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null,
            currentInstructionIndex = 0,
            totalRouteDistanceMeters = 0.0,
            etaMinutes = 0
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Facilities
    // ══════════════════════════════════════════════════════════════════════

    fun showFacilities() {
        _uiState.value = _uiState.value.copy(showFacilitiesSheet = true)
    }

    fun dismissFacilities() {
        _uiState.value = _uiState.value.copy(showFacilitiesSheet = false)
    }

    fun navigateToFacility(facility: FacilityItem) {
        val node = _uiState.value.allNodeFeatures.find {
            it.properties.node_id == facility.nodeId
        }
        _uiState.value = _uiState.value.copy(
            endNode = node,
            showFacilitiesSheet = false
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Legacy manual-node search (advanced mode)
    // ══════════════════════════════════════════════════════════════════════

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        val allNodes = _uiState.value.allNodeFeatures
        val queryLower = query.lowercase()
        val matchedNodes = allNodes.filter { node ->
            val name = node.properties.node_name?.lowercase() ?: ""
            val type = node.properties.node_type?.lowercase() ?: ""
            name.contains(queryLower) || type.contains(queryLower) || fuzzyMatch(queryLower, name)
        }
        val sortedResults = sortByProximity(matchedNodes)
        _uiState.value = _uiState.value.copy(searchResults = sortedResults)
    }

    private fun sortByProximity(nodes: List<NodeFeature>): List<NodeFeature> {
        val referencePoint: GeoPoint? = _uiState.value.startNode?.let {
            GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0])
        } ?: _uiState.value.userGpsLocation

        return if (referencePoint != null) {
            nodes
                .map { node ->
                    node to GeoPoint(
                        node.geometry.coordinates[1],
                        node.geometry.coordinates[0]
                    ).distanceToAsDouble(referencePoint)
                }
                .sortedBy { (_, dist) -> dist }
                .map { (node, _) -> node }
        } else {
            nodes.sortedBy { it.properties.node_name }
        }
    }

    private fun fuzzyMatch(query: String, target: String): Boolean {
        if (query.length < 3) return target.contains(query, ignoreCase = true)
        val maxErrors = 2
        val dp = Array(query.length + 1) { IntArray(target.length + 1) }
        for (i in 0..query.length) dp[i][0] = i
        for (j in 0..target.length) dp[0][j] = j
        for (i in 1..query.length) {
            for (j in 1..target.length) {
                val cost = if (query[i - 1] == target[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[query.length][target.length] <= maxErrors
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Pathfinding
    // ══════════════════════════════════════════════════════════════════════

    fun findPath() {
        val startId = _uiState.value.startNode?.properties?.node_id
        val endId = _uiState.value.endNode?.properties?.node_id
        if (startId == null || endId == null) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val path = pathfinder.findShortestPath(
                        startId,
                        endId,
                        _uiState.value.isAccessibleRoutePreferred
                    )
                    val boundingBox = calculateBoundingBox(path)

                    // FIX 1: If path is null, return an empty list instead of a List of Strings
                    val instructions = if (path != null) {
                        DirectionGenerator.generate(path)
                    } else {
                        emptyList()
                    }

                    // Calculate Total Distance using GeoPoints
                    var totalDistance = 0.0
                    if (path != null && path.size > 1) {
                        for (i in 0 until path.size - 1) {
                            val p1 = GeoPoint(path[i].coordinates[1], path[i].coordinates[0])
                            val p2 =
                                GeoPoint(path[i + 1].coordinates[1], path[i + 1].coordinates[0])
                            totalDistance += p1.distanceToAsDouble(p2)
                        }
                    }
                    // Average human walking speed is ~80 meters per minute
                    val eta = Math.ceil(totalDistance / MapConstants.WALK_SPEED_METERS_PER_MIN).toInt()

                    object {
                        val finalPath = path
                        val finalInstructions = instructions
                        val finalBox = boundingBox
                        val dist = totalDistance
                        val time = eta
                    }
                }

                _uiState.value = _uiState.value.copy(
                    calculatedPath = result.finalPath,
                    instructions = result.finalInstructions,
                    pathBoundingBox = result.finalBox,
                    totalRouteDistanceMeters = result.dist,
                    etaMinutes = if (result.time < 1) 1 else result.time,
                    isLoading = false,
                    currentInstructionIndex = 0
                )
            } catch (e: Exception) {
                // Respect structured concurrency by rethrowing CancellationException
                if (e is CancellationException) throw e

                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    instructions = emptyList()
                )
            }
        }
    }

    private fun calculateBoundingBox(path: List<GraphNode>?): BoundingBox? {
        if (path.isNullOrEmpty()) return null
        val minLat = path.minOf { it.coordinates[1] }
        val maxLat = path.maxOf { it.coordinates[1] }
        val minLon = path.minOf { it.coordinates[0] }
        val maxLon = path.maxOf { it.coordinates[0] }
        val latPad = (maxLat - minLat) * 0.1
        val lonPad = (maxLon - minLon) * 0.1
        return BoundingBox(maxLat + latPad, maxLon + lonPad, minLat - latPad, minLon - lonPad)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Interaction helpers
    // ══════════════════════════════════════════════════════════════════════

    fun swapNodes() {
        val s = _uiState.value
        _uiState.value = s.copy(
            startNode = s.endNode,
            endNode = s.startNode,
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null
        )
    }

    fun onMarkerTapped(node: NodeFeature) {
        val s = _uiState.value

        // Smart routing based on which field the user is actually focused on
        if (s.activeSelectionField == SelectionField.START) {
            onStartNodeSelected(node)
            // Auto-switch focus to the destination field if it's currently empty
            if (s.endNode == null) {
                _uiState.value = _uiState.value.copy(activeSelectionField = SelectionField.END)
            }
        } else {
            onEndNodeSelected(node)
            // Auto-switch focus back to start if they want to change it later
            if (s.startNode == null) {
                _uiState.value = _uiState.value.copy(activeSelectionField = SelectionField.START)
            }
        }
    }

    fun setStartNodeByTap(tapPoint: GeoPoint) {
        val allNodes = _uiState.value.allNodeFeatures
        if (allNodes.isEmpty()) return
        val closest = allNodes.minByOrNull { node ->
            GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0]).distanceToAsDouble(
                tapPoint
            )
        }
        // Reuse the smart logic so touching the map respects the active input field
        if (closest != null) onMarkerTapped(closest)
    }

    fun onLocationReceived(location: GeoPoint) {
        _uiState.value = _uiState.value.copy(userGpsLocation = location)

        // ====================================================================
        //  SOFTWARE GEOFENCE: Check proximity to known station hubs
        // ====================================================================
        if (knownStationHubs.isNotEmpty() && !_uiState.value.isDownloadingStationData) {
            geofenceManager?.checkProximity(
                location.latitude, location.longitude, knownStationHubs
            )?.let { nearbyHub ->
                if (nearbyHub.id !in loadedHubIds) {
                    onGeofenceEntered(nearbyHub.id, nearbyHub.stationName)
                }
            }
        }

        val currentState = _uiState.value

        // ====================================================================
        //  DYNAMIC REROUTING: OFF-ROUTE DETECTION
        // ====================================================================
        // Only run this if a route is active, a destination exists, and we aren't currently loading a route
        if (currentState.calculatedPath != null && currentState.endNode != null && !currentState.isLoading) {

            // 1. Calculate how far the user is from the closest point on the active path
            val minDistanceToPath = currentState.calculatedPath.minOfOrNull { pathNode ->
                GeoPoint(pathNode.coordinates[1], pathNode.coordinates[0]).distanceToAsDouble(
                    location
                )
            } ?: 0.0

            // 2. If they wander more than 25 meters away from the line, they are off-route!
            if (minDistanceToPath > MapConstants.OFF_ROUTE_THRESHOLD_METERS) {
                // Find the nearest station node to their current rogue location
                val nearestNode = currentState.allNodeFeatures.minByOrNull { node ->
                    GeoPoint(
                        node.geometry.coordinates[1],
                        node.geometry.coordinates[0]
                    ).distanceToAsDouble(location)
                }

                if (nearestNode != null && nearestNode.properties.node_id != currentState.startNode?.properties?.node_id) {
                    // Update the start node to their new location and recalculate!
                    _uiState.value = currentState.copy(startNode = nearestNode)
                    findPath() // This automatically handles the loading state and draws the new line
                }
            }
        }

        // ====================================================================
        //  LIVE ETA & DISTANCE COUNTDOWN
        // ====================================================================
        val currentPath = _uiState.value.calculatedPath
        if (!currentPath.isNullOrEmpty()) {
            // 1. Find where the user is currently located on the path
            val closestNodeIndex = currentPath.indices.minByOrNull { i ->
                GeoPoint(
                    currentPath[i].coordinates[1],
                    currentPath[i].coordinates[0]
                ).distanceToAsDouble(location)
            } ?: 0

            // 2. Start with the distance from the user to the path
            var remainingDist = location.distanceToAsDouble(
                GeoPoint(
                    currentPath[closestNodeIndex].coordinates[1],
                    currentPath[closestNodeIndex].coordinates[0]
                )
            )

            // 3. Add up the rest of the path to the destination
            for (i in closestNodeIndex until currentPath.size - 1) {
                val p1 = GeoPoint(currentPath[i].coordinates[1], currentPath[i].coordinates[0])
                val p2 =
                    GeoPoint(currentPath[i + 1].coordinates[1], currentPath[i + 1].coordinates[0])
                remainingDist += p1.distanceToAsDouble(p2)
            }

            val newEta = Math.ceil(remainingDist / MapConstants.WALK_SPEED_METERS_PER_MIN).toInt()

            _uiState.value = _uiState.value.copy(
                totalRouteDistanceMeters = remainingDist,
                etaMinutes = if (newEta < 1) 1 else newEta
            )
        }
        // ====================================================================

        if (currentState.instructions.isNotEmpty() && currentState.calculatedPath != null) {
            val currentInstruction =
                currentState.instructions.getOrNull(currentState.currentInstructionIndex)

            if (currentInstruction != null) {
                val targetLoc = GeoPoint(
                    currentInstruction.targetNode.coordinates[1],
                    currentInstruction.targetNode.coordinates[0]
                )

                // If the user gets within 8 meters of the instruction's target node...
                if (location.distanceToAsDouble(targetLoc) <= MapConstants.NEXT_STEP_PROXIMITY_METERS) {
                    nextInstruction() // Automatically swipe to the next step!
                }
            }
        }
        // ====================================================================

        // If user didn't explicitly click the GPS button, and a node/path exists, stay quiet.
        if (!forceNextLocationPrompt && (currentState.startNode != null || currentState.calculatedPath != null)) {
            return
        }

        // Reset the flag since we are about to show the dialog
        forceNextLocationPrompt = false

        val allNodes = currentState.allNodeFeatures
        if (allNodes.isEmpty()) return

        val nearest = allNodes
            .sortedBy {
                GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0]).distanceToAsDouble(
                    location
                )
            }
            .take(3)

        _uiState.value = currentState.copy(
            nearestNodeCandidates = nearest,
            showNodeSelectionDialog = true
        )
    }

    fun onGpsButtonClicked() {
        forceNextLocationPrompt = true
        // Turn tracking mode ON when they explicitly hit the GPS button
        _uiState.value = _uiState.value.copy(isTrackingModeActive = true)
        _uiState.value.userGpsLocation?.let { onLocationReceived(it) }
    }

    fun disableTrackingMode() {
        // Turn tracking mode OFF (called when the user touches the map)
        if (_uiState.value.isTrackingModeActive) {
            _uiState.value = _uiState.value.copy(isTrackingModeActive = false)
        }
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            calculatedPath = null,
            instructions = emptyList(),
            pathBoundingBox = null,
            startNode = null,
            endNode = null,
            selectedTrain = null,
            selectedDestination = null,
            currentInstructionIndex = 0,
            totalRouteDistanceMeters = 0.0,
            etaMinutes = 0
        )
    }

    fun clearStartNode() {
        _uiState.value =
            _uiState.value.copy(startNode = null, calculatedPath = null, instructions = emptyList())
    }

    fun clearEndNode() {
        _uiState.value =
            _uiState.value.copy(endNode = null, calculatedPath = null, instructions = emptyList())
    }

    fun onSearchResultSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(
            endNode = node,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun onStartNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(startNode = node)
    }

    fun onEndNodeSelected(node: NodeFeature) {
        _uiState.value = _uiState.value.copy(endNode = node)
    }

    fun getAllEdges(): List<EdgeFeature> = allEdges

    fun onZoomToPathComplete() {
        _uiState.value = _uiState.value.copy(pathBoundingBox = null)
    }

    fun confirmStartNode(node: NodeFeature) {
        onStartNodeSelected(node)
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }

    fun dismissNodeSelectionDialog() {
        _uiState.value = _uiState.value.copy(showNodeSelectionDialog = false)
    }

    // ================================
    // FACILITY QUICK CHIPS LOGIC
    // ================================

    fun routeToNearestFacility(keyword: String) {
        val currentState = _uiState.value

        // Use GPS location if available; otherwise, use the selected start node
        val referenceLocation = currentState.userGpsLocation
            ?: currentState.startNode?.let {
                GeoPoint(
                    it.geometry.coordinates[1],
                    it.geometry.coordinates[0]
                )
            }

        if (referenceLocation == null) return

        val allNodes = currentState.allNodeFeatures

        val matchedNodes = allNodes.filter { node ->
            val nodeName = node.properties.node_name?.lowercase() ?: ""
            val nodeType = node.properties.node_type?.uppercase() ?: ""

            when (keyword.lowercase()) {
                "exit" -> nodeType == "ENTRY/EXIT"
                "stairs" -> nodeType.contains("STAIRWAY")
                "ticket", "toilet" -> nodeType == "FACILITY" && nodeName.contains(keyword.lowercase())
                else -> nodeName.contains(keyword.lowercase()) || nodeType.lowercase()
                    .contains(keyword.lowercase())
            }
        }

        if (matchedNodes.isEmpty()) {
            Toast.makeText(getApplication(), "No facilities found.", Toast.LENGTH_SHORT).show()
            return
        }

        var finalStartNode = currentState.startNode
        if (finalStartNode == null) {
            finalStartNode = allNodes.minByOrNull {
                GeoPoint(it.geometry.coordinates[1], it.geometry.coordinates[0]).distanceToAsDouble(
                    referenceLocation
                )
            }
        }
        if (finalStartNode == null) return

        val startId = finalStartNode.properties.node_id

        _uiState.value = currentState.copy(isLoading = true)

        viewModelScope.launch {
            val bestTarget = withContext(Dispatchers.Default) {
                var shortestPathCost = Double.MAX_VALUE
                var closestFacilityNode: NodeFeature? = null

                // FIX: Calculate actual walking distance using Pathfinder instead of straight-line distance
                for (targetNode in matchedNodes) {
                    val targetId = targetNode.properties.node_id
                    val path = pathfinder.findShortestPath(
                        startId,
                        targetId,
                        currentState.isAccessibleRoutePreferred
                    )

                    if (path != null && path.size > 1) {
                        var pathDistance = 0.0
                        for (i in 0 until path.size - 1) {
                            val p1 = GeoPoint(path[i].coordinates[1], path[i].coordinates[0])
                            val p2 =
                                GeoPoint(path[i + 1].coordinates[1], path[i + 1].coordinates[0])
                            pathDistance += p1.distanceToAsDouble(p2)
                        }

                        if (pathDistance < shortestPathCost) {
                            shortestPathCost = pathDistance
                            closestFacilityNode = targetNode
                        }
                    }
                }

                // Fallback to straight-line distance if no valid path exists (e.g. disconnected nodes)
                closestFacilityNode ?: matchedNodes.minByOrNull { node ->
                    GeoPoint(
                        node.geometry.coordinates[1],
                        node.geometry.coordinates[0]
                    ).distanceToAsDouble(referenceLocation)
                }
            }

            if (bestTarget != null) {
                _uiState.value = _uiState.value.copy(
                    startNode = finalStartNode,
                    endNode = bestTarget,
                    searchQuery = "",
                    searchResults = emptyList()
                )
                findPath() // This will draw the final route and turn off the loading spinner
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun nextInstruction() {
        val s = _uiState.value
        if (s.currentInstructionIndex < s.instructions.lastIndex) {
            _uiState.value = s.copy(currentInstructionIndex = s.currentInstructionIndex + 1)
        }
    }

    fun prevInstruction() {
        val s = _uiState.value
        if (s.currentInstructionIndex > 0) {
            _uiState.value = s.copy(currentInstructionIndex = s.currentInstructionIndex - 1)
        }
    }

    // ======================================================================
    // LIVE BOARD DASHBOARD LOGIC
    // ======================================================================

    fun openLiveBoard() {
        _uiState.value = _uiState.value.copy(showLiveBoardSheet = true, showMiniLiveBoard = true)
    }

    fun closeLiveBoard() {
        _uiState.value = _uiState.value.copy(showLiveBoardSheet = false)
    }

    fun closeMiniLiveBoard() {
        _uiState.value = _uiState.value.copy(showMiniLiveBoard = false)
    }

    fun setLiveBoardDirection(direction: TrainDirection) {
        _uiState.value = _uiState.value.copy(liveBoardDirection = direction)
    }

    // ======================================================================
    // ON-DEMAND STATION DATA STREAMING (Issue #18)
    // ======================================================================

    /**
     * Fetches the list of station hubs from Supabase and initializes the
     * [GeofenceManager] to register hardware geofences.
     */
    private fun fetchStationHubs() {
        viewModelScope.launch {
            try {
                val hubs = withContext(Dispatchers.IO) {
                    SupabaseClient.fetchStationHubs()
                }

                if (hubs.isNotEmpty()) {
                    knownStationHubs = hubs

                    // Initialize GeofenceManager and register hardware geofences
                    geofenceManager = GeofenceManager(
                        context = getApplication(),
                        onGeofenceEntered = { hubId, stationName ->
                            onGeofenceEntered(hubId, stationName)
                        }
                    )
                    geofenceManager?.registerGeofences(hubs)

                    Log.d(TAG, "Registered ${hubs.size} station hub geofences")
                } else {
                    Log.w(TAG, "No station hubs returned from Supabase")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to fetch station hubs", e)
            }
        }
    }

    /**
     * Called when the user enters a geofence zone around a transit hub.
     * Fetches station graph data from Supabase PostGIS and rebuilds the graph.
     *
     * @param hubId The ID of the station hub that was entered
     * @param stationName Human-readable name of the station
     */
    fun onGeofenceEntered(hubId: Int, stationName: String) {
        // Don't re-fetch if already loaded or currently downloading
        if (hubId in loadedHubIds || _uiState.value.isDownloadingStationData) return

        Log.d(TAG, "Geofence entered: $stationName (hub $hubId) — loading station data")

        _uiState.value = _uiState.value.copy(
            isDownloadingStationData = true,
            stationDataError = null
        )

        viewModelScope.launch {
            try {
                // Find the hub's coordinates for the RPC call
                val hub = knownStationHubs.find { it.id == hubId }
                val lat = hub?.lat ?: _uiState.value.userGpsLocation?.latitude ?: return@launch
                val lon = hub?.lon ?: _uiState.value.userGpsLocation?.longitude ?: return@launch

                val result = withContext(Dispatchers.IO) {
                    GraphRepository.loadStationData(
                        context = getApplication(),
                        lat = lat,
                        lon = lon,
                        hubId = hubId,
                        stationName = stationName
                    )
                }

                if (result != null && result.nodes.isNotEmpty()) {
                    // Mark this hub as loaded
                    loadedHubIds.add(hubId)

                    // Merge with existing data (support multi-station)
                    val existingNodes = _uiState.value.allNodeFeatures
                    val existingNodeIds = existingNodes.map { it.properties.node_id }.toSet()
                    val newNodes = result.nodes.filter { it.properties.node_id !in existingNodeIds }
                    val mergedNodes = existingNodes + newNodes

                    val mergedEdges = allEdges + result.edges
                    allEdges = mergedEdges

                    // Rebuild the full graph
                    graph.build(mergedNodes, mergedEdges)
                    pathfinder = Pathfinder(graph)

                    _uiState.value = _uiState.value.copy(
                        allNodeFeatures = mergedNodes.sortedBy { it.properties.node_name },
                        isDownloadingStationData = false,
                        isStationDataLoaded = true,
                        activeStationName = stationName,
                        isLoading = false
                    )

                    val source = if (result.fromCache) "cache" else "network"
                    Log.d(TAG, "Loaded ${result.nodes.size} nodes + ${result.edges.size} edges from $source for $stationName")

                    Toast.makeText(
                        getApplication(),
                        "$stationName station map loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDownloadingStationData = false,
                        stationDataError = "No data available for $stationName"
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e(TAG, "Failed to load station data for $stationName", e)
                _uiState.value = _uiState.value.copy(
                    isDownloadingStationData = false,
                    stationDataError = "Failed to load station data: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears the station data error message from the UI state.
     */
    fun clearStationDataError() {
        _uiState.value = _uiState.value.copy(stationDataError = null)
    }

    override fun onCleared() {
        super.onCleared()
        geofenceManager?.removeAllGeofences()
    }
}