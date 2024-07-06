/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.apache.tools.ant.taskdefs.condition.Os
import java.util.*
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.jaredsburrows.license")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    signingConfigs {
        create("config") {
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(propsFile))
                if (Os.isFamily(Os.FAMILY_MAC)) {
                    storeFile = file(props["storeFileMac"] as String)
                }
                if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                    storeFile = file(props["storeFileWin"] as String)
                }
                storePassword = props["storePassword"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        applicationId = "com.tommihirvonen.exifnotes"
        compileSdk = 34
        minSdk = 21
        targetSdk = 34
        versionCode = 47
        versionName = "1.21.0"
        // Enabling this allows us to use resources when PNGs are generated during build-time
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val propsFile = rootProject.file("googlemapsapi.properties")
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("config")
            if (propsFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(propsFile))
                resValue("string", "google_maps_key", props["googleMapsKey"] as String)
            } else {
                resValue("string", "google_maps_key", "")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("config")
            if (propsFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(propsFile))
                resValue("string", "google_maps_key", props["googleMapsKey"] as String)
            } else {
                resValue("string", "google_maps_key", "")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "com.tommihirvonen.exifnotes"

    tasks.withType(Test::class).configureEach {
        val propsFile = rootProject.file("googlemapsapi.properties")
        if (propsFile.exists()) {
            val props = Properties()
            props.load(FileInputStream(propsFile))
            environment("google_maps_key", props["googleMapsKey"] as String)
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.androidx.percentlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.coroutines)
    implementation(libs.androidx.lifecycle)
    implementation(libs.apache.commons.text)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.databinding)
    implementation(libs.androidx.work)
    implementation(libs.serialization)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.ktor.http)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.places)
    implementation(libs.hilt)
    implementation(project(":data"))
    implementation(project(":core"))
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    coreLibraryDesugaring(libs.desugaring)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

repositories {
    mavenCentral()
}
