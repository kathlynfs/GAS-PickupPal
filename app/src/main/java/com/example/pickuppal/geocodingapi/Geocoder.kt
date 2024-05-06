package com.example.pickuppal.geocodingapi

import com.example.pickuppal.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

private const val API_KEY = BuildConfig.MAPS_API_KEY

// Interface that queries the API call to Geocoder API
// using address
interface Geocoder {
    @GET("maps/api/geocode/json?")
    suspend fun fetchLatLong(
        @Query("address") location: String,
        @Query("key") key: String = API_KEY
    ): Response
}