package com.starconsolidateden.travelhunt.api.models

data class ClaimResponse(
    val id: String? = null,
    val assetId: String? = null,
    val owner: String? = null,
    val imagePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val claimed: Boolean? = null,
    val createdAt: String? = null,

    // Meta
    val address: String? = null,
    val token: String? = null,
    val status: String? = null,          // "success" | "failure"
    val errorMessage: String? = null
)
