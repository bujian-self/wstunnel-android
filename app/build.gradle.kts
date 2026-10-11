plugins {
    id("com.android.application")
}

android {
    namespace = "cn.bujian.wstunnel"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.bujian.wstunnel"
        // hev-socks5-tunnel.so (2.18.0) requires minSdk 29; wstunnel binary links API 26.
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "11.0.0"
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

dependencies {
    implementation(files("libs/hev-socks5-tunnel.aar"))
}
