package com.example.pickuppal.directionsapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Objects

@JsonClass(generateAdapter = true)
data class Response (
    @Json(name="routes") val routes: List<Route>
)