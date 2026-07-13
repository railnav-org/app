package com.app.railnav

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.railnav.data.TrainRepository
import com.app.railnav.ui.components.AdvancedSearchCard
import com.app.railnav.ui.components.FacilitiesSheet
import com.app.railnav.ui.components.FacilityQuickChips
import com.app.railnav.ui.components.InstructionsSheet
import com.app.railnav.ui.components.LiveTrainDashboardSheet
import com.app.railnav.ui.components.LocationPermissionDialog
import com.app.railnav.ui.components.MapControls
import com.app.railnav.ui.components.MapLegend
import com.app.railnav.ui.components.MiniLiveBoardCard
import com.app.railnav.ui.components.NodeSelectionDialog
import com.app.railnav.ui.components.SearchResultsList
import com.app.railnav.ui.components.StationSuggestionList
import com.app.railnav.ui.components.TrainDestinationCard
import com.app.railnav.ui.components.TrainListSheet
import com.app.railnav.ui.components.TurnByTurnCard
import com.app.railnav.ui.theme.RailNavTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RailNavTheme { PathfindingScreen() } }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathfindingScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel()
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val currentMinutes by mainViewModel.currentMinutes.collectAsState()
    val currentTimeFormatted by mainViewModel.currentTimeFormatted.collectAsState()
    val instructionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val trainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showInstructions by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val locationHandler = remember {
        LocationHandler(
            context = context.applicationContext,
            onLocationReceived = { mainViewModel.onLocationReceived(it) },
            onPermissionDenied = { showPermissionDialog = true }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locationHandler.startLocationUpdates() else showPermissionDialog = true
    }
    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // The user clicked "Turn on" in the Google dialog!
            mainViewModel.onGpsButtonClicked()
            locationHandler.startLocationUpdates()
        } else {
            // The user clicked "No Thanks" — guide them to use the map instead
            android.widget.Toast.makeText(
                context,
                "No worries! Tap anywhere on the map to set your starting point.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    DisposableEffect(Unit) { onDispose { locationHandler.stopLocationUpdates() } }

    // Removed: No longer auto-show the full step list sheet.
    // The compact TurnByTurnCard at the bottom is enough.

    // BackHandler priority: clear route first, then clear selected train
    BackHandler(enabled = uiState.calculatedPath != null) {
        mainViewModel.clearRoute()
        showInstructions = false
    }

    BackHandler(enabled = uiState.calculatedPath == null && uiState.selectedTrain != null) {
        mainViewModel.clearSelectedTrain()
    }

    RailNavTheme(darkTheme = isDarkTheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(16.dp))
                    // Header
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Train, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "RailNav",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Thane Station Navigator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    // Station navigation
                    Text(
                        "MAPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Map, null) },
                        label = { Text("Platform Navigator") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    // IRCTC section
                    Text(
                        "IRCTC SERVICES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ConfirmationNumber, null) },
                        label = { Text("PNR Status") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, PnrStatusActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Schedule, null) },
                        label = { Text("Train Schedule") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(
                                Intent(
                                    context,
                                    TrainScheduleActivity::class.java
                                )
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.TrackChanges, null) },
                        label = { Text("Live Station Board") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, LiveStationActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Train, null) },
                        label = { Text("Live Train Status") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, LiveTrainActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            Box(modifier = modifier.fillMaxSize()) {

                // ── Map ────────────────────────────────────────────────────────
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    path = uiState.calculatedPath,
                    boundingBox = uiState.pathBoundingBox,
                    onZoomComplete = { mainViewModel.onZoomToPathComplete() },
                    onMapTap = { mainViewModel.setStartNodeByTap(it) },
                    allEdges = mainViewModel.getAllEdges(),
                    allNodes = uiState.allNodeFeatures,
                    onMarkerTap = { mainViewModel.onMarkerTapped(it) },
                    userGpsLocation = uiState.userGpsLocation,
                    isTrackingModeActive = uiState.isTrackingModeActive,
                    onDisableTracking = { mainViewModel.disableTrackingMode() },
                    isDarkTheme = isDarkTheme,
                    startNode = uiState.startNode
                )

                // ── Top search card ────────────────────────────────────────────
                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        if (uiState.isAdvancedMode) {
                            AdvancedSearchCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                uiState = uiState,
                                onStartNodeSelected = { mainViewModel.onStartNodeSelected(it) },
                                onEndNodeSelected = { mainViewModel.onEndNodeSelected(it) },
                                onFindPath = {
                                    mainViewModel.findPath()
                                },
                                onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) },
                                onSwitchToSimpleMode = { mainViewModel.toggleAdvancedMode() },
                                // FIX: Passing the viewmodel calls as lambdas to resolve Unresolved References!
                                onClearStartNode = { mainViewModel.clearStartNode() },
                                onClearEndNode = { mainViewModel.clearEndNode() },
                                mainViewModel = mainViewModel
                            )
                        } else {
                            TrainDestinationCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                uiState = uiState,
                                onQueryChange = { mainViewModel.onTrainDestinationQueryChanged(it) },
                                onShowTrains = { mainViewModel.openTrainSheet() },
                                onFindPath = {
                                    mainViewModel.findPath()
                                },
                                onSwitchToAdvancedMode = { mainViewModel.toggleAdvancedMode() },
                                onClearStartNode = { mainViewModel.clearStartNode() },
                                onOpenLiveBoard = { mainViewModel.openLiveBoard() },
                                onClearTrain = { mainViewModel.clearSelectedTrain() },
                                onOpenMenu = { scope.launch { drawerState.open() } },
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        FacilityQuickChips(
                            onChipSelected = { keyword ->
                                // FIX: Intercept the click and check if we have a starting point
                                if (uiState.startNode == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Please set your location by tapping the map or the GPS icon first.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    mainViewModel.routeToNearestFacility(keyword)
                                }
                            }
                        )

                        // ── Map Legend ──────────────────────────────────────────
                        MapLegend()

                        if (!uiState.isAdvancedMode && uiState.destinationSuggestions.isNotEmpty()) {
                            StationSuggestionList(
                                suggestions = uiState.destinationSuggestions,
                                onSelect = { mainViewModel.onDestinationSelected(it) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        if (uiState.isAdvancedMode && uiState.searchResults.isNotEmpty()) {
                            SearchResultsList(
                                results = uiState.searchResults,
                                userLocation = uiState.userGpsLocation,
                                onSelect = { mainViewModel.onSearchResultSelected(it) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // ---------------- Map Controls ----------------
                MapControls(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onMyLocationClick = {
                        if (locationHandler.hasLocationPermission()) {
                            // NEW LOGIC: Ask Google to check settings
                            locationHandler.checkLocationSettingsAndStart(
                                onSuccess = {
                                    // GPS is already on
                                    mainViewModel.onGpsButtonClicked()
                                    locationHandler.startLocationUpdates()
                                },
                                onResolutionRequired = { resolvableException ->
                                    // GPS is off. Launch the beautiful Google bottom sheet dialog!
                                    try {
                                        val intentSenderRequest =
                                            IntentSenderRequest.Builder(resolvableException.resolution)
                                                .build()
                                        settingResultRequest.launch(intentSenderRequest)
                                    } catch (_: Exception) {
                                        // Fallback just in case Google Play Services crashes
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    }
                                }
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onSwapNodes = { mainViewModel.swapNodes() },
                    onFacilities = { mainViewModel.showFacilities() },
                    showSwap = uiState.isAdvancedMode,
                    isAccessiblePreferred = uiState.isAccessibleRoutePreferred,
                    onToggleAccessibility = { mainViewModel.toggleAccessibilityMode() },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                )

                // ── Mini Live Station Board (Peek State) ────────────────────────
                AnimatedVisibility(
                    // Only show if mini board is enabled, we aren't currently navigating, and the full sheet isn't open
                    visible = uiState.showMiniLiveBoard && uiState.calculatedPath == null && !uiState.showLiveBoardSheet,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MiniLiveBoardCard(
                        currentMinutes = currentMinutes,
                        onExpand = { mainViewModel.openLiveBoard() },
                        onClose = { mainViewModel.closeMiniLiveBoard() },
                        onTrainSelected = { train ->
                            mainViewModel.onTrainSelected(train)
                            mainViewModel.closeMiniLiveBoard()
                        }
                    )
                }

                // ── Dynamic Turn-By-Turn Banner ────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.calculatedPath != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    val currentText =
                        uiState.instructions.getOrNull(uiState.currentInstructionIndex)?.text
                            ?: "Loading steps..."

                    // NEW: Scan the route for inaccessible steps!
                    val hasInaccessibleSteps = uiState.instructions.any {
                        it.text.contains("STAIR", ignoreCase = true) ||
                                it.text.contains("ESCALATOR", ignoreCase = true)
                    }
                    val showWarning = uiState.isAccessibleRoutePreferred && hasInaccessibleSteps

                    TurnByTurnCard(
                        currentInstruction = currentText,
                        currentIndex = uiState.currentInstructionIndex,
                        totalInstructions = uiState.instructions.size,
                        totalDistanceMeters = uiState.totalRouteDistanceMeters,
                        etaMinutes = uiState.etaMinutes,
                        showAccessibilityWarning = showWarning, // <-- PASSED DOWN
                        onNext = { mainViewModel.nextInstruction() },
                        onPrev = { mainViewModel.prevInstruction() },
                        onViewList = { showInstructions = true },
                        onExit = { mainViewModel.clearRoute() }
                    )
                }

                // ── Loading ────────────────────────────────────────────────────
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                // ── Node selection dialog ──────────────────────────────────────
                if (uiState.showNodeSelectionDialog) {
                    NodeSelectionDialog(
                        nearestNodes = uiState.nearestNodeCandidates,
                        userLocation = uiState.userGpsLocation,
                        onNodeSelected = { mainViewModel.confirmStartNode(it) },
                        onDismiss = {
                            mainViewModel.dismissNodeSelectionDialog()
                            android.widget.Toast.makeText(
                                context,
                                "You can also tap directly on the map to set your start point.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // ── Location permission dialog ─────────────────────────────────
                if (showPermissionDialog) {
                    LocationPermissionDialog(
                        onGrant = {
                            showPermissionDialog = false
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onDismiss = {
                            showPermissionDialog = false
                            android.widget.Toast.makeText(
                                context,
                                "You can tap the map to set your location manually.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // ── Turn-by-turn instructions sheet ───────────────────────────
                if (showInstructions && uiState.instructions.isNotEmpty()) {
                    ModalBottomSheet(
                        onDismissRequest = { showInstructions = false },
                        sheetState = instructionsSheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        InstructionsSheet(
                            instructions = uiState.instructions,
                            startNode = uiState.startNode,
                            endNode = uiState.endNode,
                            onClose = { showInstructions = false }
                        )
                    }
                }

                // ── Train list sheet ───────────────────────────────────────────
                if (uiState.showTrainSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { mainViewModel.dismissTrainSheet() },
                        sheetState = trainSheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TrainListSheet(
                            destination = uiState.selectedDestination ?: "",
                            trains = uiState.availableTrains,
                            onTrainSelected = { mainViewModel.onTrainSelected(it) },
                            onClose = { mainViewModel.dismissTrainSheet() }
                        )
                    }
                }

                // ── Facilities sheet ───────────────────────────────────────────
                if (uiState.showFacilitiesSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { mainViewModel.dismissFacilities() },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        FacilitiesSheet(
                            facilities = TrainRepository.facilities,
                            onFacilitySelect = { mainViewModel.navigateToFacility(it) },
                            onDismiss = { mainViewModel.dismissFacilities() }
                        )
                    }
                }

                // ── Live Station Dashboard Sheet ───────────────────────────────
                if (uiState.showLiveBoardSheet) {
                    val liveBoardSheetState =
                        rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    ModalBottomSheet(
                        onDismissRequest = { mainViewModel.closeLiveBoard() },
                        sheetState = liveBoardSheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        LiveTrainDashboardSheet(
                            currentDirection = uiState.liveBoardDirection,
                            currentMinutes = currentMinutes,
                            currentTimeFormatted = currentTimeFormatted,
                            onDirectionChanged = { mainViewModel.setLiveBoardDirection(it) },
                            onClose = { mainViewModel.closeLiveBoard() },
                            onTrainSelected = { train ->
                                mainViewModel.onTrainSelected(train)
                                mainViewModel.closeLiveBoard()
                                mainViewModel.closeMiniLiveBoard()
                            },
                            // FIX: Back to the clean, adaptive height restriction
                            modifier = Modifier.fillMaxHeight(0.85f)
                        )
                    }
                }


            } // end Box
        } // end ModalNavigationDrawer
    } // end RailNavTheme
}