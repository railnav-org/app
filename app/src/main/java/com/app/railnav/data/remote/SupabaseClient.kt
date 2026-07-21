package com.app.railnav.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Lightweight HTTP client for calling Supabase PostgREST RPC endpoints.
 * Uses the existing OkHttp dependency already in the project.
 */
object SupabaseClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    /**
     * Calls the Supabase `get_station_data` RPC function to fetch all nodes and edges
     * within [radiusMeters] of the given GPS coordinates.
     *
     * @param lat User's latitude
     * @param lon User's longitude
     * @param radiusMeters Geofence search radius (default 500m)
     * @return [StationDataResponse] containing lists of nodes and edges, or null on failure
     */
    suspend fun fetchStationData(
        lat: Double,
        lon: Double,
        radiusMeters: Double = SupabaseConfig.DEFAULT_GEOFENCE_RADIUS_METERS
    ): StationDataResponse? = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.SUPABASE_URL}${SupabaseConfig.RPC_GET_STATION_DATA}"

            val requestBodyJson = """
                {
                    "user_lat": $lat,
                    "user_lon": $lon,
                    "radius_meters": $radiusMeters
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    json.decodeFromString<StationDataResponse>(body)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetches the list of all station hubs from the Supabase `station_hubs` table.
     * Used to register geofences around known transit hubs.
     *
     * @return List of [RemoteStationHub], or empty list on failure
     */
    suspend fun fetchStationHubs(): List<RemoteStationHub> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/station_hubs?select=id,station_name,lat,lon,geofence_radius_meters"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    json.decodeFromString<List<RemoteStationHub>>(body)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
