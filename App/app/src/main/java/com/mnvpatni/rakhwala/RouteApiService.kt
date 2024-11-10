package com.mnvpatni.rakhwala

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RouteApiService {
    @GET("v1/get-route")
    suspend fun getRoutes(
        @Query("start_lat") startLat: Double,
        @Query("start_lon") startLon: Double,
        @Query("end_lat") endLat: Double,
        @Query("end_lon") endLon: Double
    ): Response<RouteResponse>
}
