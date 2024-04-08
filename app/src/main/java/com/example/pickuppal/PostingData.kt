package com.example.pickuppal

import java.util.UUID

data class PostingData(
    val postID: String = UUID.randomUUID().toString(),
    val userID: String,
    val title: String,
    val location: String,
    val description: String,
    val claimed: Boolean,
    val photoUrl: String
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userID" to userID,
            "title" to title,
            "location" to location,
            "description" to description,
            "claimed" to claimed,
            "photoUrl" to photoUrl
        )
    }
}
