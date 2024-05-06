package com.example.pickuppal

import com.example.pickuppal.geocodingapi.Geocoder
import com.example.pickuppal.geocodingapi.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

// Repository for responses from API call to Google Maps Geocoding API
class GeocoderResultsRepository {
    private val geocoder: Geocoder

    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        geocoder = retrofit.create<Geocoder>()
    }

    suspend fun fetchGeocoderResults(location: String): Response =
        geocoder.fetchLatLong(location)
}