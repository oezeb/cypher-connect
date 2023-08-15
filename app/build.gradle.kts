plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("android")
}

setupApp()

android {
    namespace = "com.github.oezeb.cypher_connect"
    defaultConfig.applicationId = "com.github.oezeb.cypher_connect"
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:22.2.0")
}
