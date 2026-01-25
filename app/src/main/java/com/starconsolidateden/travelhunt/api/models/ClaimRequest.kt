package com.starconsolidateden.travelhunt.api.models

data class ClaimRequest(
    val assetId: String,
    val newOwner: String
)
