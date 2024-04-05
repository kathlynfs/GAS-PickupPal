package com.example.pickuppal

data class UserStatistics(
    var userID: String = "",
    var averageRating: Float = 0.0f,
    var numItemsPosted: Int = 0,
    var numItemsClaimed: Int = 0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userID" to userID,
            "averageRating" to averageRating,
            "numItemsPosted" to numItemsPosted,
            "numItemsClaimed" to numItemsClaimed
        )
    }
}

