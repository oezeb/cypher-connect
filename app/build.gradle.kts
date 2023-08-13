plugins {
    id("com.android.application")
    id("com.google.android.gms.oss-licenses-plugin")
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
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(project(":core"))
    implementation(project(":obfs_local"))
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
}
