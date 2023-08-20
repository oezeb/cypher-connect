plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
}

setupCommon()

android {
    namespace = "com.github.oezeb.cypher_connect.design"
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.android.gms:play-services-ads:22.2.0")
}