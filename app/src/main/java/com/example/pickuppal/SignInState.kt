package com.example.pickuppal
//  Used this for login https://www.youtube.com/watch?v=zCIfBbm06QM
data class SignInState (
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val isSignInAttempted: Boolean = false
)