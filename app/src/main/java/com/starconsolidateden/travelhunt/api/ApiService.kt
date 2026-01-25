package com.starconsolidateden.travelhunt.api;

import com.starconsolidateden.travelhunt.api.models.ClaimRequest
import com.starconsolidateden.travelhunt.api.models.ClaimResponse
import com.starconsolidateden.travelhunt.api.models.DigitalAssetResponse
import com.starconsolidateden.travelhunt.api.models.LoginRequest
import com.starconsolidateden.travelhunt.api.models.LoginResponse
import com.starconsolidateden.travelhunt.api.models.ObjectIdLoginRequest
import com.starconsolidateden.travelhunt.api.models.SignupResponse;
import com.starconsolidateden.travelhunt.api.models.SignupRequest;
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST;
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    @POST("/api/auth/register")
    suspend fun signup(@Body request: SignupRequest): Response<SignupResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

     @GET("/api/assets/list-unclaimed") // replace with your GET API path
     fun getDigitalAssets(@Header("Authorization") token: String): Call<List<DigitalAssetResponse>>

     @GET("/api/assets/get/{assetId}")
     suspend fun getAssetById(
         @Header("Authorization") token: String, // JWT token
         @Path("assetId") assetId: String
     ): Response<DigitalAssetResponse>

    @POST("/api/assets/claim")
    suspend fun claimAsset(
        @Header("Authorization") token: String,
        @Body request: ClaimRequest
    ): Response<ClaimResponse>

   @GET("/api/assets/owner/{ownerAddress}")
    suspend fun getAssetsByOwner(
        @Header("Authorization") token: String,    // JWT token
        @Path("ownerAddress") ownerAddress: String // Owner's public address in path
    ): Response<List<DigitalAssetResponse>>

    @POST("/api/auth/objectid-login")
    suspend fun objectIdLogin(@Body request: ObjectIdLoginRequest): Response<LoginResponse>

    @Multipart
    @POST("/api/assets/create")
    suspend fun createDigitalAsset(
        @Header("Authorization") token: String,

        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("owner") owner: RequestBody,

        @Part image: MultipartBody.Part
    ): Response<DigitalAssetResponse>
}
