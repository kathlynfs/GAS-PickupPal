package com.example.pickuppal.geocodingapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Geometry (
    @Json(name = "location") val location: Location
)