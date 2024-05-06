package com.example.pickuppal

import java.io.Serializable

// Used this for login https://www.youtube.com/watch?v=zCIfBbm06QM
data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)

data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?
) : Serializable
