import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "kr.app.feeling"
    compileSdk = 35

    bundle {
        storeArchive {
            enable = true  // 이 부분 추가
        }
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("production") {
            dimension = "environment"

            val versionFile = rootProject.file("version.properties")
            val versionProperties = Properties()

            if (versionFile.exists()) {
                versionProperties.load(FileInputStream(versionFile))
            }

            val currentVersionCode = (versionProperties.getProperty("versionCode") ?: "0").toInt()
            val currentMinorVersion = (versionProperties.getProperty("minorVersion") ?: "0").toInt()
            val newVersionCode = currentVersionCode + 1
            val newMinorVersion = currentMinorVersion + 1

            versionCode = newVersionCode
            versionName = "$newVersionCode.$newMinorVersion"

            // 새 버전 저장
            versionProperties.setProperty("versionCode", versionCode.toString())
            versionProperties.setProperty("minorVersion", newMinorVersion.toString())
            versionFile.outputStream().use {
                versionProperties.store(it, null)
            }

            buildConfigField("String", "WEB_VIEW_URL", "\"https://app-feeling.com\"")
            buildConfigField("Boolean", "DEBUG_MODE", "false")

        }
        create("development") {
            dimension = "environment"
            applicationId = "kr.app.feeling.dev"  // 개발용 applicationId
            resValue("string", "app_name", "Feeling(Dev)")  // 개발용 앱 이름

            versionCode = 1
            versionName = "1.0.0-dev"

            buildConfigField("String", "WEB_VIEW_URL", "\"http://192.168.1.3:3000\"")
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
    }

    defaultConfig {
        applicationId = "kr.app.feeling"
        minSdk = 26
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("C:\\필링(Feeling)\\구글키스토어\\keystore.jks")
            storePassword = "fjqmfpxj7&"
            keyAlias = "feeling_key"
            keyPassword = "fjqmfpxj7&"
        }
        create("release") {
            storeFile = file("C:\\필링(Feeling)\\구글키스토어\\keystore.jks")
            storePassword = "fjqmfpxj7&"
            keyAlias = "feeling_key"
            keyPassword = "fjqmfpxj7&"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.3.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.gms:google-services:4.4.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}