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
import java.util.concurrent.TimeUnit

// --- Data classes for API Communication ---
data class ExperimentConfig(val experimentId: String, val key: String, val value: String)
data class TrackRequest(val userId: String, val experimentId: String, val variantName: String, val event: String)

// --- Retrofit API Interface ---
interface VariantApi {
    @GET("api/config")
    suspend fun getConfig(@Query("userId") userId: String): List<ExperimentConfig>

    @POST("api/track")
    suspend fun trackEvent(@Body request: TrackRequest)
}

// --- Main SDK Singleton ---
object Variant {
    private const val TAG = "VariantSDK"
    private lateinit var prefs: SharedPreferences
    private lateinit var userId: String
    private lateinit var appId: String
    private lateinit var api: VariantApi
    private var configMap = mutableMapOf<String, ExperimentConfig>()
    private var fallbackConfig = mapOf<String, String>()
    private val trackedExposures = mutableSetOf<String>()
    private val sdkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val PRODUCTION_BASE_URL = "https://variant-backend-lfoa.onrender.com/"

    interface VariantListener {
        fun onConfigUpdated()
    }

    private var listener: VariantListener? = null

    fun setListener(listener: VariantListener) {
        this.listener = listener
    }

    /**
     * Initializes the SDK.
     * @param appId The unique identifier for your application (Multi-tenancy).
     * @param apiKey Your secret API key.
     * @param defaults Fallback values if the server is unreachable.
     */
    fun init(context: Context, apiKey: String, appId: String, defaults: Map<String, String> = emptyMap()) {
        prefs = context.getSharedPreferences("variant_prefs", Context.MODE_PRIVATE)
        this.fallbackConfig = defaults
        this.appId = appId

        // Persist App ID locally
        prefs.edit().putString("app_id", appId).apply()

        // Setup OkHttpClient with Multi-tenancy Interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("X-App-ID", this.appId) // Multi-tenancy Header
                    .header("X-API-Key", apiKey)
                    .method(original.method, original.body)
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        // 1. Get or Generate User ID
        userId = prefs.getString("user_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("user_id", it).apply()
        }

        // 2. Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(PRODUCTION_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(VariantApi::class.java)

        // 3. Load Cache
        loadCache()

        // 4. Fetch Live Data from Cloud
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
                Log.d(TAG, "Sync successful for App: $appId | User: $userId")
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
        val currentAppId = prefs.getString("app_id", "") ?: ""
        prefs.edit().remove("user_id").remove("config_cache").apply()
        // Re-init with same appId but new userId
        init(context, "internal_key", currentAppId, fallbackConfig)
    }

    private fun saveCache(configs: List<ExperimentConfig>) {
        val json = Gson().toJson(configs)
        prefs.edit().putString("config_cache", json).apply()
    }

    private fun loadCache() {
        appId = prefs.getString("app_id", "") ?: ""
        val json = prefs.getString("config_cache", null) ?: return
        try {
            val type = object : TypeToken<List<ExperimentConfig>>() {}.type
            val configs: List<ExperimentConfig> = Gson().fromJson(json, type)
            configs.forEach { configMap[it.key] = it }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache", e)
        }
    }
}