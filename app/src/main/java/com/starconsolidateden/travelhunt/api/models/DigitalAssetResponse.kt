package com.starconsolidateden.travelhunt.api.models

data class DigitalAssetResponse(
    val assetId: String,
    val owner: String,
    val imagePath: String, // URL of the icon
    val latitude: Double,
    val longitude: Double,
    val claimed: Boolean

)
