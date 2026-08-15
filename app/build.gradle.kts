import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mapsplatform.secrets)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
}

// Security Configuration: Load secrets from local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.raphael.roadsystem"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.raphael.roadsystem"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject Secrets into Manifest and BuildConfig
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        val clienteId = localProperties.getProperty("ClienteID") ?: ""
        val certSha1 = localProperties.getProperty("CERT_SHA1") ?: ""
        
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "ClienteID", "\"$clienteId\"")
        buildConfigField("String", "CERT_SHA1", "\"$certSha1\"")
    }

    signingConfigs {
        create("release") {
            val storePath = localProperties.getProperty("RELEASE_STORE_FILE") ?: ""
            storeFile = if (storePath.isNotEmpty()) file(storePath) else null
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            optimization {
                enable = false
            }
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

secrets {
    // Ignore all keys starting with RELEASE_ as they are sensitive and only used for signing
    ignoreList.add("RELEASE_.*")
}

dependencies {
    // 1. AndroidX & Compose Base
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // 2. Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // 3. Firebase & Auth
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // 4. Room (Offline Database)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // 5. Google Maps SDK (Via Compose)
    implementation(libs.maps.compose)
    implementation("com.google.maps.android:maps-compose-utils:8.4.0")
    implementation(libs.play.services.location)

    // 6. Network & UI Utilities
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation(libs.androidx.work.runtime)
    implementation(libs.coil.compose)
    implementation(libs.lifecycleRuntimeCompose)

    // 7. Google Sheets API Direct Sync
    implementation(libs.googleApiClientAndroid)
    implementation(libs.googleApiServicesSheets)
    implementation(libs.googleAuthLibraryOauth2Http)

    // 8. Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Remove as duplicatas que conflitam com o Navigation SDK e Google API Client
configurations.all {
    // Resolve o erro do Manifest Merger (Cronet)
    exclude(group = "org.chromium.net", module = "cronet-fallback")
    
    // Resolve conflitos da Google API Client Library
    exclude(group = "com.google.guava", module = "listenablefuture")
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
}

android {
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}
