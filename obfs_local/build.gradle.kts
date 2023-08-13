plugins {
    id("com.android.library")
    kotlin("android")
}

setupCommon()

android {
    namespace = "com.github.shadowsocks.obfs_local"
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }
}

dependencies {
    implementation(project(":plugin"))
}

