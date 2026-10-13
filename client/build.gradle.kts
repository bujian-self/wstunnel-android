plugins {
    id("com.android.application")
}

android {
    namespace = "cn.bujian.wstunnel.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.bujian.wstunnel.client"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-beta"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/upload-keystore.jks")
            storePassword = "wstunnel-android"
            keyAlias = "upload"
            keyPassword = "wstunnel-android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
