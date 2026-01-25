package com.starconsolidateden.travelhunt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.starconsolidateden.travelhunt.api.RestService
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateDigitalAssetFragment : Fragment() {

    // ----------- LOCATION -----------
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ----------- CAMERA -----------
    private lateinit var imageFile: File
    private var photoUri: Uri? = null
    private var cameraOpened = false

    // ----------- UI -----------
    private lateinit var lottieView: LottieAnimationView
    private lateinit var tvStatus: TextView

    // ----------- CAMERA RESULT -----------
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Log.d("CAMERA", "Photo captured: $photoUri")
                showLoading()
                getLastLocation()
            } else {
                showError("Camera cancelled")
            }
        }

    // ----------- PERMISSION RESULT -----------
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] == true
            val locationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (cameraGranted && locationGranted) {
                openCamera()
            } else {
                showError("Camera or location permission denied")
            }
        }

    // ----------- LIFECYCLE -----------
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_create_digital_asset, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        lottieView = view.findViewById(R.id.lottieView)
        tvStatus = view.findViewById(R.id.tvStatus)

        if (!cameraOpened) {
            cameraOpened = true
            checkPermissionsAndLaunchCamera()
        }
    }

    // ----------- PERMISSIONS -----------
    private fun checkPermissionsAndLaunchCamera() {
        val cameraPermission =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)

        val locationPermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        if (cameraPermission == PackageManager.PERMISSION_GRANTED &&
            locationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    // ----------- CAMERA -----------
    private fun openCamera() {
        imageFile = createImageFile()

        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )

        cameraLauncher.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "IMG_$timeStamp",
            ".jpg",
            storageDir
        )
    }

    // ----------- UI STATES -----------
    private fun showLoading() {
        lottieView.setAnimation(R.raw.loading)
        lottieView.loop(true)
        lottieView.visibility = View.VISIBLE
        lottieView.playAnimation()

        tvStatus.text = "Creating asset..."
        tvStatus.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )
        tvStatus.visibility = View.VISIBLE
    }

    private fun showSuccess() {
        lottieView.setAnimation(R.raw.success)
        lottieView.loop(true)
        lottieView.playAnimation()

        tvStatus.text = "Created asset successfully"
        tvStatus.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        )
        tvStatus.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        lottieView.cancelAnimation()
        lottieView.visibility = View.GONE

        tvStatus.text = message
        tvStatus.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )
        tvStatus.visibility = View.VISIBLE
    }

    // ----------- LOCATION -----------
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("LOCATION", "Lat: ${location.latitude}")
                    Log.d("LOCATION", "Lng: ${location.longitude}")

                    createAsset(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    showError("Unable to fetch location")
                }
            }
            .addOnFailureListener {
                showError("Location error")
                Log.e("LOCATION", "Error", it)
            }
    }

    // ----------- API CALL (COROUTINE) -----------
    private fun createAsset(latitude: Double, longitude: Double) {
        val publicAddress = SecurePrefs.getString("ADDRESS") ?: run {
            Toast.makeText(requireContext(), "User address not found", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {

            val result = RestService.createDigitalAsset(
                latitude = latitude,
                longitude = longitude,
                owner = publicAddress,
                imageFile = imageFile
            )

            if (result.isSuccess) {
                showSuccess()
            } else {
                showError(result.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }
}
