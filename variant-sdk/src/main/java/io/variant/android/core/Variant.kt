package io.variant.android.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.UUID

// Data classes for API Communication
data class ExperimentConfig(val experimentId: String, val key: String, val value: String)
data class TrackRequest(val userId: String, val experimentId: String, val variantName: String, val event: String)

interface VariantApi {
    @GET("api/config")
    suspend fun getConfig(@Query("userId") userId: String): List<ExperimentConfig>

    @POST("api/track")
    suspend fun trackEvent(@Body request: TrackRequest)
}

object Variant {
    private const val TAG = "VariantSDK"
    private lateinit var prefs: SharedPreferences
    private lateinit var userId: String
    private lateinit var api: VariantApi
    private var configMap = mutableMapOf<String, ExperimentConfig>()
    private var fallbackConfig = mapOf<String, String>()
    private val trackedExposures = mutableSetOf<String>()
    private val sdkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val PRODUCTION_BASE_URL ="https://variant-backend-lfoa.onrender.com"

    interface VariantListener {
        fun onConfigUpdated()
    }

    private var listener: VariantListener? = null

    fun setListener(listener: VariantListener) {
        this.listener = listener
    }

    fun init(context: Context, apiKey: String, defaults: Map<String, String> = emptyMap()) {
        prefs = context.getSharedPreferences("variant_prefs", Context.MODE_PRIVATE)
        this.fallbackConfig = defaults

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // 1. Get or Generate User ID
        userId = prefs.getString("user_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("user_id", it).apply()
        }

        // 2. Setup Network (Retrofit)
        val retrofit = Retrofit.Builder()
            .baseUrl(PRODUCTION_BASE_URL)
            .client(okHttpClient)// Emulator address for localhost
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(VariantApi::class.java)

        // 3. Load Cache
        loadCache()

        // 4. Fetch Live Data
        fetchConfiguration()
    }

    private fun fetchConfiguration() {
        sdkScope.launch {
            try {
                val configs = api.getConfig(userId)
                configMap.clear()
                configs.forEach { configMap[it.key] = it }
                saveCache(configs)

                withContext(Dispatchers.Main) {
                    listener?.onConfigUpdated()
                }
                Log.d(TAG, "Sync successful. User ID: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}. Using cache/fallbacks.")
            }
        }
    }

    fun getString(key: String, defaultValue: String): String {
        val config = configMap[key]

        if (config != null) {
            if (!trackedExposures.contains(key)) {
                trackExposure(key, config)
                trackedExposures.add(key)
            }
            return config.value
        }

        return fallbackConfig[key] ?: defaultValue
    }

    fun track(key: String, eventName: String) {
        val config = configMap[key] ?: return
        sdkScope.launch {
            try {
                api.trackEvent(TrackRequest(userId, config.experimentId, config.value, eventName))
            } catch (e: Exception) {
                Log.e(TAG, "Track failed", e)
            }
        }
    }


    private fun trackExposure(key: String, config: ExperimentConfig) {
        sdkScope.launch {
            try {
                api.trackEvent(TrackRequest(userId, config.experimentId, config.value, "exposure"))
            } catch (e: Exception) { /* Silently fail exposures */ }
        }
    }

    fun resetUser(context: Context) {
        trackedExposures.clear()
        prefs.edit().remove("user_id").remove("config_cache").apply()
        init(context, "internal_key", fallbackConfig)
    }

    private fun saveCache(configs: List<ExperimentConfig>) {
        val json = Gson().toJson(configs)
        prefs.edit().putString("config_cache", json).apply()
    }

    private fun loadCache() {
        val json = prefs.getString("config_cache", null) ?: return
        val type = object : TypeToken<List<ExperimentConfig>>() {}.type
        val configs: List<ExperimentConfig> = Gson().fromJson(json, type)
        configs.forEach { configMap[it.key] = it }
    }
}