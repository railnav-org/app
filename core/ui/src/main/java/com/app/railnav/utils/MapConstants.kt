package com.app.railnav.utils

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor

object MapConstants {
    // --- Navigation & Routing Math ---
    /** Average walking speed (approx. 4.8 km/h) */
    const val WALK_SPEED_METERS_PER_MIN = 80.0
    
    /** Distance from the path before triggering an off-route recalculation */
    const val OFF_ROUTE_THRESHOLD_METERS = 25.0
    
    /** Distance to a node to consider it "reached" for turn-by-turn */
    const val NEXT_STEP_PROXIMITY_METERS = 8.0

    // --- OSMDroid Map Line Colors (Requires standard Android Colors) ---
    val COLOR_ESCALATOR_ANDROID = AndroidColor.parseColor("#9C27B0") // Purple
    val COLOR_STAIRS_ANDROID = AndroidColor.parseColor("#FF0800")    // Red
    val COLOR_WALKWAY_ANDROID = AndroidColor.parseColor("#EED202")   // Yellow
    val COLOR_ACTIVE_ROUTE_ANDROID = AndroidColor.parseColor("#1976D2") // Blue

    // --- Compose UI Colors (For the Legend) ---
    val COLOR_ESCALATOR_COMPOSE = ComposeColor(0xFF9C27B0)
    val COLOR_STAIRS_COMPOSE = ComposeColor(0xFFFF0800)
    val COLOR_WALKWAY_COMPOSE = ComposeColor(0xFFEED202)
    val COLOR_ACTIVE_ROUTE_COMPOSE = ComposeColor(0xFF1976D2)
}