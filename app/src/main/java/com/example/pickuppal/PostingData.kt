package com.example.pickuppal

// data class that defines a PostingData object
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
    val claimedBy: String,
    var photoUrl: String,
    var rating: Int,
) {
    constructor() : this("", "", "", "", 0.0, 0.0,"", "",false, "", "", 0)
    // function that creates and returns map of PostingData attribute names along with their values
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
            "photoUrl" to photoUrl,
            "claimedBy" to claimedBy,
            "rating" to rating,
        )
    }
}
