package com.example.pickuppal

data class UserStatistics(
    val userID: String,
    val averageRating: Float = 0.0f,
    val numItemsPosted: Int,
    val numItemsClaimed: Int
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

