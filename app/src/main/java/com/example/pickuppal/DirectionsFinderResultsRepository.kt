package com.example.pickuppal

import com.example.pickuppal.directionsapi.DirectionsFinder
import com.example.pickuppal.directionsapi.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

class DirectionsFinderResultsRepository {
    private val directionsFinder: DirectionsFinder

    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        directionsFinder = retrofit.create<DirectionsFinder>()
    }

    suspend fun fetchDirections(destination: String, origin: String): Response =
        directionsFinder.fetchDirections(destination, origin)
}