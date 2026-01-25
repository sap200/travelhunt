package com.starconsolidateden.travelhunt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.starconsolidateden.travelhunt.api.RestService
import com.starconsolidateden.travelhunt.api.models.DigitalAssetResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.bumptech.glide.request.target.CustomTarget


private const val LOCATION_PERMISSION_CODE = 1

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private lateinit var fabMyLocation: FloatingActionButton

    private var locationOverlay: MyLocationNewOverlay? = null
    private var gpsProvider: GpsMyLocationProvider? = null

    private var radiusCircle: Polygon? = null
    private val RADIUS_METERS = 100.0

    private val mainHandler = Handler(Looper.getMainLooper())

    // 🔹 POLLER (NO LISTENER — THIS IS CORRECT FOR OSMDROID)
    private val locationPoller = object : Runnable {
        override fun run() {
            locationOverlay?.myLocation?.let { geo ->
                updateRadiusCircle(geo)
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_map, container, false)

        Configuration.getInstance().userAgentValue = requireContext().packageName

        map = view.findViewById(R.id.mapview)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)

        fabMyLocation = view.findViewById(R.id.btn_my_location)
        fabMyLocation.setOnClickListener {
            locationOverlay?.myLocation?.let { geo ->
                mainHandler.post {
                    map.controller.animateTo(geo)
                    map.controller.setZoom(18.0)
                }
            }
        }

        requestLocationPermission()
        return view
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            enableLocation()
        }
    }

    private fun enableLocation() {
        gpsProvider = GpsMyLocationProvider(requireContext())

        val myLocation = ContextCompat.getDrawable(requireContext(), R.drawable.ic_my_location);
        val icon = drawableToBitmap(myLocation)

        locationOverlay = MyLocationNewOverlay(gpsProvider, map).apply {
            enableMyLocation()
            enableFollowLocation()
            isDrawAccuracyEnabled = true
            setDirectionArrow(icon, icon)
            setDirectionIcon(icon)
            setPersonIcon(icon)
        }

        map.overlays.add(locationOverlay)
        map.invalidate()

        // FIRST FIX → MOVE CAMERA
        locationOverlay?.runOnFirstFix {
            locationOverlay?.myLocation?.let { geo ->
                mainHandler.post {
                    map.controller.animateTo(geo)
                    map.controller.setZoom(18.0)
                }
            }
        }

        // 🔹 START CIRCLE UPDATES
        mainHandler.post(locationPoller)
        fetchAssetsAsync()
    }

    // 🔹 RADIUS CIRCLE (200 METERS)
    private fun updateRadiusCircle(center: GeoPoint) {
        mainHandler.post {

            if (radiusCircle == null) {
                radiusCircle = Polygon().apply {

                    fillPaint.color = Color.parseColor("#332196F3")
                    fillPaint.style = Paint.Style.FILL
                    fillPaint.isAntiAlias = true

                    outlinePaint.color = Color.BLUE
                    outlinePaint.strokeWidth = 2f
                    outlinePaint.style = Paint.Style.STROKE
                    outlinePaint.isAntiAlias = true
                }

                map.overlays.add(0, radiusCircle)
            }

            radiusCircle!!.points =
                Polygon.pointsAsCircle(center, RADIUS_METERS)

            map.invalidate()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocation()
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val bitmap = createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1)
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        mainHandler.removeCallbacks(locationPoller)
    }

    override fun onStop() {
        super.onStop()
        mainHandler.removeCallbacks(locationPoller)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocation()
    }


    private fun stopLocation() {
        // Stop handler/poller
        mainHandler.removeCallbacks(locationPoller)
        // Stop GPS
        locationOverlay?.disableMyLocation()
        locationOverlay?.disableFollowLocation()
        gpsProvider?.stopLocationProvider()
        // Clear overlays
        map.overlays.clear()

        // Stop map
        map.onPause()
        map.onDetach()

        locationOverlay = null
        gpsProvider = null
        radiusCircle = null
    }


    // loading icons
    private fun fetchAssetsAsync() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = RestService.getDigitalAssets(requireContext())
                result.onSuccess { assets ->
                    // Start processing each asset one by one
                    // TODO: remove this
                    Log.d("FETCH_GET", result.toString())
                    processAssetsSequentially(assets)
                }.onFailure { error ->
                    Log.e("MAP_ASSETS", "Failed to load assets: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("MAP_ASSETS", "Exception while fetching assets", e)
            }
        }
    }

    private suspend fun processAssetsSequentially(assets: List<DigitalAssetResponse>) {
        for (asset in assets) {
            try {
                val bitmap = loadBitmapFromUrlSuspend(asset.imagePath)
                if (bitmap != null) {
                    addMarkerOnMap(asset, bitmap)
                }
            } catch (e: Exception) {
                Log.e("MAP_ASSETS", "Failed to load image for ${asset.assetId}", e)
            }
        }
    }

    private suspend fun loadBitmapFromUrlSuspend(url: String): Bitmap? =
        suspendCancellableCoroutine { cont ->
            Glide.with(requireContext())
                .asBitmap()
                .load(url)
                .override(80, 80) // resize for small icon
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        cont.resume(resource, null)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        cont.resume(null, null)
                    }
                })
        }

    private fun addMarkerOnMap(asset: DigitalAssetResponse, bitmap: Bitmap) {
        // Create a circular bitmap with red gradient border
        val markerBitmap = createBoxMarkerWithBorder(bitmap, size = 100, borderWidth = 8f)

        // Create OSMDroid marker
        val marker = org.osmdroid.views.overlay.Marker(map)
        marker.position = GeoPoint(asset.latitude, asset.longitude)
        marker.icon = BitmapDrawable(resources, markerBitmap)
        marker.title = asset.assetId

        marker.setOnMarkerClickListener { _,_ ->
            val intent = Intent(requireContext(), AssetDetailsActivity::class.java)
            intent.putExtra("assetId", asset.assetId)
            intent.putExtra("radius", RADIUS_METERS) // radius for claim check
            locationOverlay?.myLocation?.let { geo ->
                intent.putExtra("userLat", geo.latitude)
                intent.putExtra("userLon", geo.longitude)
            }
            startActivity(intent)
            true
        }

        // Add marker to map on UI thread
        requireActivity().runOnUiThread {
            map.overlays.add(marker)
            map.invalidate()
        }
    }

    private fun createBoxMarkerWithBorder(
        bitmap: Bitmap,
        size: Int = 100,
        borderWidth: Float = 8f
    ): Bitmap {
        // Resize the image to fit inside the box
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // --- Draw the red border first ---
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }
        val borderRect = RectF(
            borderWidth / 2f,
            borderWidth / 2f,
            size - borderWidth / 2f,
            size - borderWidth / 2f
        )
        canvas.drawRect(borderRect, borderPaint)

        // --- Draw the image inside the box ---
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val imageRect = RectF(
            borderWidth,
            borderWidth,
            size - borderWidth,
            size - borderWidth
        )
        val bitmapShader = BitmapShader(
            smallBitmap,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        imagePaint.shader = bitmapShader
        canvas.drawRect(imageRect, imagePaint)

        return output
    }





}
