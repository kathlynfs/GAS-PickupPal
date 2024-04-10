package com.example.pickuppal.geocodingapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Results (
    @Json(name="geometry") val geometry: Geometry
)