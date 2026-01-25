package com.starconsolidateden.travelhunt.api.models

data class LoginResponse (
    val address: String,
    val token : String,
    val status : String,
    val errorMessage: String,
    val email: String,
    val objectId: String,
)