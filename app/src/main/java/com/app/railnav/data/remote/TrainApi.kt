package com.app.railnav.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface TrainApi {

    @GET("trainSchedule")
    suspend fun getTrainSchedule(
        @Query("trainNumber") trainNumber: String
    ): TrainRouteResponse

    @GET("pnrStatus")
    suspend fun getPnrStatus(
        @Query("pnr") pnrNumber: String
    ): PnrResponse

    @GET("liveStation")
    suspend fun getLiveStationBoard(
        @Query("source") source: String,
        @Query("hours") hours: String,
        @Query("destination") destination: String? = null
    ): LiveStationResponse

    @GET("liveTrain")
    suspend fun getLiveTrainStatus(
        @Query("trainNumber") trainNumber: String,
        @Query("startDay") startDay: String
    ): LiveTrainResponse
}