package com.example.pickuppal.directionsapi


import com.example.pickuppal.BuildConfig
import com.example.pickuppal.directionsapi.Response
import retrofit2.http.GET
import retrofit2.http.Query

private const val API_KEY = BuildConfig.MAPS_API_KEY
interface DirectionsFinder
{
    @GET("maps/api/directions/json")
    suspend fun fetchDirections(
        @Query("destination") destination: String,
        @Query("origin") origin: String,
        @Query("key") key: String = API_KEY
    ): Response
}