plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.room)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pro.chessin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pro.chessin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testOptions {
            unitTests {
                isIncludeAndroidResources = false
                isReturnDefaultValues = true
                all {
                    it.maxHeapSize = "2g"
                }
            }
        }

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -O2 -fexceptions")
            }
        }

        // Room schema export for AutoMigration support
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    // Configure Room plugin for schema export
    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        getByName("test") {
            resources.srcDirs("src/test/resources")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            ndk {
                abiFilters.clear()
                abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
            }
        }
        debug {
            ndk {
                abiFilters.clear()
                abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
            }
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    
    dynamicFeatures.addAll(setOf(":nnue_assets"))
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }
    
    ndkVersion = libs.versions.ndk.get()
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    implementation(libs.play.asset.delivery.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.work:work-runtime:2.9.1")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.jtokkit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.kotlinx.coroutines.test)
}

// Force all dependency versions to ones compatible with AGP 8.5.2 + compileSdk 34.
// Newer transitive dependencies (pulled in by hilt-navigation-compose, work-runtime, etc.)
// require AGP 8.6+ / compileSdk 35+, which causes AAR metadata check failures.
configurations.all {
    resolutionStrategy {
        force("androidx.hilt:hilt-navigation-compose:1.2.0")
        force("androidx.hilt:hilt-work:1.2.0")

        force("androidx.navigation:navigation-compose:2.7.7")
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
        force("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        force("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
        force("androidx.activity:activity-compose:1.9.3")
        force("androidx.compose.ui:ui:1.7.0")
        force("androidx.compose.ui:ui-graphics:1.7.0")
        force("androidx.compose.ui:ui-tooling:1.7.0")
        force("androidx.compose.ui:ui-tooling-preview:1.7.0")
        force("androidx.compose.ui:ui-test-manifest:1.7.0")
        force("androidx.compose.ui:ui-test-junit4:1.7.0")
        force("androidx.compose.material3:material3:1.3.0")
        force("androidx.compose.foundation:foundation:1.7.0")
        force("androidx.compose.foundation:foundation-layout:1.7.0")
        force("androidx.compose.runtime:runtime:1.7.0")
        force("androidx.compose.runtime:runtime-saveable:1.7.0")
        force("androidx.annotation:annotation:1.8.0")
        force("androidx.collection:collection:1.4.0")
        force("androidx.profileinstaller:profileinstaller:1.4.0")
    }
}