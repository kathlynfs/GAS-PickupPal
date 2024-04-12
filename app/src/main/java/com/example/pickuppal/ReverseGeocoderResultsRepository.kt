package com.example.pickuppal

import com.example.pickuppal.reversegeocodingapi.Response
import com.example.pickuppal.reversegeocodingapi.ReverseGeocoder
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

class ReverseGeocoderResultsRepository {
    private val reverseGeocoder: ReverseGeocoder

    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        reverseGeocoder = retrofit.create<ReverseGeocoder>()
    }

    suspend fun fetchReverseGeocoderResults(latLng: String): Response =
        reverseGeocoder.fetchAddress(latLng)
}