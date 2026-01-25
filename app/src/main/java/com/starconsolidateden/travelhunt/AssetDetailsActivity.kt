package com.starconsolidateden.travelhunt

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.starconsolidateden.travelhunt.api.RestService
import com.starconsolidateden.travelhunt.api.models.DigitalAssetResponse
import com.starconsolidateden.travelhunt.base.BaseActivity
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL
import kotlin.math.*

class AssetDetailsActivity : BaseActivity() {

    private lateinit var imgAsset: ImageView
    private lateinit var tvAssetId: TextView
    private lateinit var tvOwner: TextView
    private lateinit var tvCity: TextView
    private lateinit var btnClaim: Button
    private lateinit var tvClaimMessage: TextView

    private lateinit var assetId: String
    private var radius: Double = 0.0
    private var userLat: Double = 0.0
    private var userLon: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_asset_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.AssetDetails)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        imgAsset = findViewById(R.id.img_asset)
        tvAssetId = findViewById(R.id.tv_asset_id)
        tvOwner = findViewById(R.id.tv_owner)
        tvCity = findViewById(R.id.tv_city)
        btnClaim = findViewById(R.id.btn_claim)
        tvClaimMessage = findViewById(R.id.tv_claim_message)

        // Get intent data
        assetId = intent.getStringExtra("assetId") ?: ""
        radius = intent.getDoubleExtra("radius", 0.0)
        userLat = intent.getDoubleExtra("userLat", 0.0)
        userLon = intent.getDoubleExtra("userLon", 0.0)

        // Fetch asset and populate UI
        fetchAssetDetails()

    }


    private fun fetchAssetDetails() {
        lifecycleScope.launch {
            // Runs whenever lifecycle is at least CREATED
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.CREATED) {
                try {
                    val result = RestService.getAssetById(this@AssetDetailsActivity, assetId)
                    result.onSuccess { asset ->
                        populateAssetUI(asset)
                    }.onFailure { error ->
                        Toast.makeText(
                            this@AssetDetailsActivity,
                            "Failed to load asset: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    private fun populateAssetUI(asset: DigitalAssetResponse) {
        // Load image
        Glide.with(this)
            .load(asset.imagePath)
            .placeholder(R.drawable.ic_outline_ac_unit_24)
            .into(imgAsset)

        tvAssetId.text = "Asset ID: ${asset.assetId}"
        tvOwner.text = "Owner: ${asset.owner}"

        // Resolve city asynchronously
        lifecycleScope.launch {
            val city = getCityFromLatLon(asset.latitude, asset.longitude)
            tvCity.text = "City: $city"
        }

        // Claim eligibility
        val currentUserAddress = SecurePrefs.getString("ADDRESS") ?: ""

        val isOwner = asset.owner.equals(currentUserAddress, ignoreCase = true)
        val isClaimed = asset.claimed
        val distance = distanceInMeters(userLat, userLon, asset.latitude, asset.longitude)

        if (!isClaimed && !isOwner && distance <= radius) {
            btnClaim.isEnabled = true
            tvClaimMessage.visibility = View.VISIBLE
            tvClaimMessage.text = "You can claim this asset!"
        } else {
            tvClaimMessage.visibility = View.VISIBLE
            tvClaimMessage.text = "you cannot claim this asset!"
            tvClaimMessage.setTextColor(0xFFFF0000.toInt())
        }

        // TODO: Claim button
        btnClaim.setOnClickListener {

            // Disable immediately to avoid double click
            btnClaim.isEnabled = false
            tvClaimMessage.text = "Claiming asset..."
            tvClaimMessage.visibility = View.VISIBLE

            lifecycleScope.launch {

                val result = RestService.claimAsset(this@AssetDetailsActivity, assetId, currentUserAddress)

                result.onSuccess { response ->
                    Toast.makeText(this@AssetDetailsActivity, "Asset claimed successfully!", Toast.LENGTH_SHORT).show()

                    tvOwner.text = "Owner: ${response.owner}"
                    tvClaimMessage.text = "✅ Asset claimed!"
                    btnClaim.isEnabled = false
                }

                result.onFailure { error ->
                    Toast.makeText(this@AssetDetailsActivity, error.message ?: "Claim failed", Toast.LENGTH_SHORT).show()
                    tvClaimMessage.text = error.message
                    btnClaim.isEnabled = true
                }
            }
        }




    }
    }

// Haversine formula
private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

// Reverse geocode using OpenStreetMap Nominatim
private suspend fun getCityFromLatLon(lat: Double, lon: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon")
                .header("User-Agent", "TravelHunt/1.0")
                .build()

            val response = client.newCall(request).execute()
            val jsonText = response.body?.string() ?: return@withContext "Unknown"

            val jsonObj = JSONObject(jsonText)
            val address = jsonObj.optJSONObject("address")

            address?.optString("city")
                ?: address?.optString("town")
                ?: address?.optString("village")
                ?: "Unknown"

        } catch (e: Exception) {
            Log.e("GEOCODER", "Reverse geocoding failed", e)
            "Unknown"
        }
    }
}

