package io.variant.android.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface VariantApi {
    @GET("api/config")
    suspend fun getConfig(@Query("userId") userId: String): Response<List<ExperimentConfig>>

    @POST("api/track")
    suspend fun trackEvent(@Body request: TrackRequest): Response<Unit>
}