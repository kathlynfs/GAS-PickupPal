package com.example.pickuppal.reversegeocodingapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Results(
    @Json(name = "formatted_address") val address: String
)