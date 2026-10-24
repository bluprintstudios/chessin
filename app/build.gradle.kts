import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// This URL is deliberately a local property, never an API key. Keep it out of source
// control and set it only after the Worker has been deployed and reviewed.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val aiCoachBaseUrl = (providers.gradleProperty("aiCoachBaseUrl").orNull
    ?: localProperties.getProperty("aiCoachBaseUrl"))
    ?.trim()
    ?.trimEnd('/')
    .orEmpty()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.pro.chess_review"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pro.chess_review"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AI_COACH_BASE_URL", "\"$aiCoachBaseUrl\"")
        buildConfigField("boolean", "AI_COACH_SERVICE_CONFIGURED", (aiCoachBaseUrl.isNotEmpty()).toString())
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    packaging {
        jniLibs {
            // Stockfish is a separate PIE executable packaged under jniLibs so Android extracts it
            // to nativeLibraryDir, where ProcessBuilder can launch it.
            useLegacyPackaging = true
            keepDebugSymbols += "**/libstockfish.so"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.bhlangonijr.chesslib)
    implementation(libs.okhttp)
    implementation(libs.billing.ktx)
    implementation(libs.androidsvg)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
