package com.example.pickuppal

import java.util.UUID

data class PostingData(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val location: String,
    val description: String
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "location" to location,
            "description" to description
        )
    }
}
