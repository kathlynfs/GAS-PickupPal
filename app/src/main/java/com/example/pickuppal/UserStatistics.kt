package com.example.pickuppal

// data class that defines a UserStatistics object
data class UserStatistics(
    var userID: String = "",
    var numItemsPosted: Int = 0,
    var numItemsClaimed: Int = 0,
    var numRatings: Int = 0,
    var totalRating: Int = 0,
) {
    // function that creates and returns map of UserStatistics attribute names along with their values
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userID" to userID,
            "numItemsPosted" to numItemsPosted,
            "numItemsClaimed" to numItemsClaimed,
            "numRatings" to numRatings,
            "totalRating" to totalRating,
        )
    }
}

