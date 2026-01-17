import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // KSP version must match Kotlin version (2.0.21)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    // Plugin for publishing to JitPack
    id("maven-publish")
}
// ⚠️ VITAL: This closing brace '}' above ends the plugins block.
// Everything else must be BELOW this line.

android {

    defaultConfig {
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        val apiKey = properties.getProperty("VARIANT_API_KEY") ?: ""

        buildConfigField("String", "VARIANT_API_KEY", "\"$apiKey\"")

    // 2. Inject it into the generated BuildConfig class
    buildConfigField("String", "VARIANT_API_KEY", "\"$apiKey\"")
    }
    buildFeatures {
        buildConfig = true
    }

    namespace = "io.variant.android.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Optional: Include source code in the release (Great for portfolios)
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// KSP Configuration
ksp {
    arg("jvmTarget", "17")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    // Networking - Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // JSON Parsing - Moshi
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // JSON Parsing - Gson
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Async - Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
}

// Publishing Configuration
// We use 'configure' here to ensure it's safe even if Gradle gets confused
afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                // ⚠️ REPLACE 'YourUsername' below with your actual GitHub username!
                groupId = "com.github.YourUsername"
                artifactId = "variant-sdk"
                version = "1.0.0"
            }
        }
    }
}