package com.example.pickuppal

data class UserStatistics(
    var userID: String = "",
    var numItemsPosted: Int = 0,
    var numItemsClaimed: Int = 0,
    var numRatings: Int = 0,
    var totalRating: Int = 0,
) {
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

