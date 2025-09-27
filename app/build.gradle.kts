import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.hilt)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.license)
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

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    defaultConfig {
        applicationId = "com.tommihirvonen.exifnotes"
        compileSdk = 35
        minSdk = 21
        targetSdk = 35
        versionCode = 50
        versionName = "1.22.2"
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
        compose = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work)
    implementation(libs.androidx.documentfile)
    implementation(libs.jetbrains.kotlinx.serialization.json)
    implementation(libs.apache.commons.text)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.android.libraries.places)
    implementation(libs.maps.compose)
    implementation(libs.ktor.http)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.test.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.test.manifest)
    coreLibraryDesugaring(libs.android.tools.desugar)
    implementation(project(":data"))
    implementation(project(":core"))
}