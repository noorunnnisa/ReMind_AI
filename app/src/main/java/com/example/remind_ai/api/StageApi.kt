package com.example.remind_ai.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class StageRequest(
    val mf: Int,
    val age: Double,
    val educ: Double,
    val ses: Double,
    val mmse: Double,
    val etiv: Double,
    val nwbv: Double,
    val asf: Double
)

data class StageResponse(
    val predicted_stage: String,
    val probabilities: Map<String, Double>
)

interface StageApi {
    @POST("predict-stage")
    fun predictStage(@Body request: StageRequest): Call<StageResponse>

    @Multipart
    @POST("predict-image-stage")
    fun predictImageStage(
        @Part file: MultipartBody.Part
    ): Call<StageResponse>
}