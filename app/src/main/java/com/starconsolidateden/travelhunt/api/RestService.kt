package com.starconsolidateden.travelhunt.api

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.starconsolidateden.travelhunt.api.models.ClaimRequest
import com.starconsolidateden.travelhunt.api.models.ClaimResponse
import com.starconsolidateden.travelhunt.api.models.DigitalAssetResponse
import com.starconsolidateden.travelhunt.api.models.GoogleSignInRequest
import com.starconsolidateden.travelhunt.api.models.LoginRequest
import com.starconsolidateden.travelhunt.api.models.LoginResponse
import com.starconsolidateden.travelhunt.api.models.ObjectIdLoginRequest
import com.starconsolidateden.travelhunt.api.models.SignupRequest
import com.starconsolidateden.travelhunt.api.models.SignupResponse
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File


object RestService {

    private val retrofit: Retrofit
    val api: ApiService

    init {
        val client = OkHttpClient.Builder()
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)
    }

    // Signup call
    suspend fun signup(email: String, password: String): Result<SignupResponse> {
        return try {
            val response = api.signup(SignupRequest(email, password))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBodyString = response.errorBody()?.string() // can be null
                val errorResponse = Gson().fromJson(errorBodyString, SignupResponse::class.java)

                Log.d("SIGNUP ERROR", errorResponse.errorMessage);
                val errorMessage = errorResponse?.errorMessage ?: "Unknown error"
                Result.failure(Exception("${response.code()} $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if(response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBodyString, LoginResponse::class.java)

                Log.d("LOGIN ERROR", errorResponse.errorMessage)
                val errorMessage = errorResponse?.errorMessage ?: "Unknown error"
                Result.failure(Exception("${response.code()} $errorMessage"))
            }
        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDigitalAssets(context: Context): Result<List<DigitalAssetResponse>> {
        return try {
            val jwt = SecurePrefs.getString("JWT_TOKEN")
                ?: return Result.failure(Exception("JWT not found"))

            val call = api.getDigitalAssets("Bearer $jwt")

            // convert callback to suspend
            val assets = kotlinx.coroutines.suspendCancellableCoroutine<List<DigitalAssetResponse>> { cont ->
                call.enqueue(object : retrofit2.Callback<List<DigitalAssetResponse>> {
                    override fun onResponse(
                        call: retrofit2.Call<List<DigitalAssetResponse>>,
                        response: retrofit2.Response<List<DigitalAssetResponse>>
                    ) {
                        if (response.isSuccessful) {
                            cont.resume(response.body() ?: emptyList(), null)
                        } else {
                            val err = response.errorBody()?.string() ?: "Unknown error"
                            cont.resumeWith(Result.failure(Exception("Error ${response.code()}: $err")))
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<List<DigitalAssetResponse>>, t: Throwable) {
                        cont.resumeWith(Result.failure(t))
                    }
                })
            }

            Result.success(assets)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getAssetById(context: Context, assetId: String): Result<DigitalAssetResponse> {
        return try {
            // Get JWT token from SharedPreferences
            val jwtToken = SecurePrefs.getString("JWT_TOKEN")
                ?: return Result.failure(Exception("JWT token not found"))

            val response = api.getAssetById("Bearer $jwtToken", assetId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Failed to fetch asset: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimAsset(
        context: Context,
        assetId: String,
        newOwner: String
    ): Result<ClaimResponse> {

        return try {
            val token = SecurePrefs.getString("JWT_TOKEN") ?: return Result.failure(Exception("User not authenticated"))

            val response = api.claimAsset(
                token = "Bearer $token",
                request = ClaimRequest(assetId, newOwner)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.errorMessage != null) {
                    Result.failure(Exception(body.errorMessage))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("Claim failed (${response.code()})"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAssetsByOwner(context: Context, ownerAddress: String): Result<List<DigitalAssetResponse>> {
        return try {
            // Get JWT from secure prefs
            val token = SecurePrefs.getString("JWT_TOKEN")
                ?: return Result.failure(Exception("User not authenticated"))

            // Call API
            val response = api.getAssetsByOwner(
                token = "Bearer $token",
                ownerAddress = ownerAddress
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch assets (${response.code()})"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun objectIdLogin(objectId: String): Result<LoginResponse> {
        return try {
            val response = api.objectIdLogin(ObjectIdLoginRequest(objectId))
            if(response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBodyString, LoginResponse::class.java)

                Log.d("LOGIN ERROR", errorResponse.errorMessage)
                val errorMessage = errorResponse?.errorMessage ?: "Unknown error"
                Result.failure(Exception("${response.code()} $errorMessage"))
            }
        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDigitalAsset(
        latitude: Double,
        longitude: Double,
        owner: String,
        imageFile: File
    ): Result<DigitalAssetResponse> {
        return try {
            val token = SecurePrefs.getString("JWT_TOKEN")
                ?: return Result.failure(Exception("User not authenticated"))

            // ---- Build request bodies ----
            val latitudeBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudeBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val ownerBody = owner.toRequestBody("text/plain".toMediaTypeOrNull())

            // ---- Determine real mime type ----
            val mimeType = when (imageFile.extension.lowercase()) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }

            val imageRequestBody = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())

            val imagePart = MultipartBody.Part.createFormData(
                name = "image",
                filename = imageFile.name, // important: keep proper extension
                body = imageRequestBody
            )

            // ---- API call ----
            val response = api.createDigitalAsset(
                token = "Bearer $token",
                latitude = latitudeBody,
                longitude = longitudeBody,
                owner = ownerBody,
                image = imagePart
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Create asset failed (${response.code()}): $errorBody"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }


    }

    suspend fun googleLogin(token: String): Result<LoginResponse> {
        return try {
            val response = api.googleLogin(GoogleSignInRequest(token))
            if(response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBodyString, LoginResponse::class.java)

                Log.d("GOOGLE LOGIN ERROR", errorResponse.errorMessage)
                val errorMessage = errorResponse?.errorMessage ?: "Unknown error"
                Result.failure(Exception("${response.code()} $errorMessage"))
            }
        } catch(e: Exception) {
            Result.failure(e)
        }
    }



}
