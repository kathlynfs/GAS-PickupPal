package com.example.pickuppal.geocodingapi


import retrofit2.http.GET
import retrofit2.http.Query

private const val API_KEY = "AIzaSyClUeiZ-Umu8FyVB605Dy9THtinsmRoxkU"
// will need to properly hide this
interface Geocoder {
    @GET("maps/api/geocode/json?")
    suspend fun fetchLatLong(
        @Query("address") location: String,
        @Query("key") key: String = API_KEY
    ): Response
}