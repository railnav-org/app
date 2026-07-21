package com.app.railnav.data.remote

import com.app.railnav.BuildConfig

/**
 * Configuration for the Supabase PostGIS backend.
 * Credentials are injected from local.properties via BuildConfig.
 */
object SupabaseConfig {
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    /** Default geofence radius in meters */
    const val DEFAULT_GEOFENCE_RADIUS_METERS = 500.0

    /** RPC endpoint for fetching station data within a geofence */
    const val RPC_GET_STATION_DATA = "/rest/v1/rpc/get_station_data"
}
