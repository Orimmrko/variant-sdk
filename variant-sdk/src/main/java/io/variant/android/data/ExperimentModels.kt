package io.variant.android.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExperimentConfig(
    @Json(name = "experimentId") val experimentId: String,
    @Json(name = "key") val key: String,
    @Json(name = "value") val value: String
)

@JsonClass(generateAdapter = true)
data class TrackRequest(
    val userId: String,
    val experimentId: String,
    val variantName: String, // The 'value' from the config
    val event: String
)