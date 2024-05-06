package com.example.pickuppal.directionsapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Response (
    @Json(name="routes") val routes: List<Route>
)