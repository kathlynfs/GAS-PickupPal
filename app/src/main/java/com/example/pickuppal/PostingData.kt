package com.example.pickuppal

import java.util.UUID

data class PostingData(
    var postID: String,
    val userID: String,
    val title: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val reverseGeocodedAddress: String,
    val description: String,
    val claimed: Boolean,
    var photoUrl: String
) {
    constructor() : this("", "", "", "", 0.0, 0.0,"", "",false, "")

    fun toMap(): Map<String, Any> {
        return mapOf(
            "userID" to userID,
            "title" to title,
            "location" to location,
            "lat" to lat,
            "lng" to lng,
            "reverseGeocodedAddress" to reverseGeocodedAddress,
            "description" to description,
            "claimed" to claimed,
            "photoUrl" to photoUrl
        )
    }
}
