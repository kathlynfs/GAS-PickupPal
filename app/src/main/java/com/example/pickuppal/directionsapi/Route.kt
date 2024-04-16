package com.example.pickuppal.directionsapi

import android.view.WindowInsetsAnimation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Route(
    @Json(name="overview_polyline") val overviewPolyline: DirectionsPolyline
)