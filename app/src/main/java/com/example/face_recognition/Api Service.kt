package com.example.face_recognition


import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.Response

interface ApiService {
    @Multipart
    @POST("predict")
    suspend fun uploadVideo(
        @Part video: MultipartBody.Part,
        @Part("seconds") seconds: RequestBody
    ): Response<Map<String, Any>>
}

data class VitalsResponse(
    val heart_rate: Double?,
    val sdnn: Double?,
    val rmssd: Double?,
    val pnn50: Double?,
    val stress_level: String?,
    val spo2: Double?,
    val resp_rate: Double?
)


