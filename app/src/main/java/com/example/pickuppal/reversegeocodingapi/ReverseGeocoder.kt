package com.example.pickuppal.reversegeocodingapi

import com.example.pickuppal.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

private const val API_KEY = BuildConfig.MAPS_API_KEY

interface ReverseGeocoder {
    @GET("maps/api/geocode/json?")
    suspend fun fetchAddress(
        @Query("latlng") latLng: String,
        @Query("key") key: String = API_KEY
    ): Response
}