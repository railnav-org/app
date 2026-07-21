package com.app.railnav

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.app.railnav.data.remote.RemoteStationHub
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Manages geofences around known transit station hubs.
 *
 * When the user enters a geofence radius (default 500m), the [onGeofenceEntered]
 * callback fires with the station hub ID, triggering on-demand data download.
 */
class GeofenceManager(
    private val context: Context,
    private val onGeofenceEntered: (hubId: Int, stationName: String) -> Unit
) {
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val registeredGeofences = mutableMapOf<String, RemoteStationHub>()

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_REQUEST_ID_PREFIX = "station_hub_"

        // Singleton callback holder so the BroadcastReceiver can notify
        @Volatile
        var activeCallback: ((hubId: Int, stationName: String) -> Unit)? = null
            private set

        // Lookup map for registered hubs
        @Volatile
        var hubLookup: Map<String, RemoteStationHub> = emptyMap()
            private set
    }

    init {
        activeCallback = onGeofenceEntered
    }

    /**
     * Registers geofences for a list of station hubs.
     * Each hub gets a circular geofence at its center point.
     */
    fun registerGeofences(hubs: List<RemoteStationHub>) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Missing location permission, cannot register geofences")
            return
        }

        val geofenceList = hubs.map { hub ->
            val requestId = "$GEOFENCE_REQUEST_ID_PREFIX${hub.id}"
            registeredGeofences[requestId] = hub

            Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(
                    hub.lat,
                    hub.lon,
                    hub.geofenceRadiusMeters.toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        if (geofenceList.isEmpty()) return

        // Update the static lookup so the BroadcastReceiver can resolve hub info
        hubLookup = registeredGeofences.toMap()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered ${geofenceList.size} geofences")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering geofences", e)
        }
    }

    /**
     * Removes all registered geofences.
     */
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
                registeredGeofences.clear()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofences", e)
            }
    }

    /**
     * Software-based proximity check as a fallback/supplement to hardware geofencing.
     * Call this from every location update to detect if the user is within range
     * of a station hub, even if the OS geofence hasn't fired yet.
     *
     * @param lat Current user latitude
     * @param lon Current user longitude
     * @param hubs List of known station hubs to check against
     * @return The nearest hub within range, or null if none
     */
    fun checkProximity(
        lat: Double,
        lon: Double,
        hubs: List<RemoteStationHub>
    ): RemoteStationHub? {
        for (hub in hubs) {
            val distance = haversineDistance(lat, lon, hub.lat, hub.lon)
            if (distance <= hub.geofenceRadiusMeters) {
                return hub
            }
        }
        return null
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Haversine formula to calculate distance between two GPS coordinates in meters.
     */
    private fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}

/**
 * BroadcastReceiver that handles geofence transition events from the OS.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofenceBroadcast"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = com.google.android.gms.location.GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing event error: ${geofencingEvent?.errorCode}")
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            for (geofence in triggeringGeofences) {
                val hub = GeofenceManager.hubLookup[geofence.requestId]
                if (hub != null) {
                    Log.d(TAG, "Entered geofence for station: ${hub.stationName}")
                    GeofenceManager.activeCallback?.invoke(hub.id, hub.stationName)
                }
            }
        }
    }
}
