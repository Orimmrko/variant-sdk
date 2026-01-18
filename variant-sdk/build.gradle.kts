import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // KSP version must match your Kotlin version (2.0.21)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("maven-publish")
}

android {
    namespace = "io.variant.android.core"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // --- Safe API Key Injection ---
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }

        // Retrieve and clean the key to prevent double-quote errors
        val rawApiKey = properties.getProperty("VARIANT_API_KEY") ?: "default_dev_key"
        val cleanApiKey = rawApiKey.replace("\"", "")

        // Inject as a Java String literal
        buildConfigField("String", "VARIANT_API_KEY", "\"$cleanApiKey\"")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

ksp {
    arg("jvmTarget", "17")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Networking - Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // JSON Parsing - Moshi (Required for ExperimentModels)
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    // JSON Parsing - Gson (Required for SDK Persistence/Variant.kt)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Async - Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
}

// JitPack / Maven Publishing Configuration
afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.Orimmrko"
                artifactId = "variant-sdk"
                version = "1.0.2" // Updated Version
            }
        }
    }
}