// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    apply(from = "repositories.gradle.kts")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        val kotlinVersion = rootProject.extra["kotlinVersion"].toString()
        classpath(rootProject.extra["androidPlugin"].toString())
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.24.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
        classpath("org.mozilla.rust-android-gradle:plugin:0.9.3")
    }
}

allprojects {
    apply(from = "${rootProject.projectDir}/repositories.gradle.kts")
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

// skip uploading the mapping to Crashlytics
subprojects {
    tasks.whenTaskAdded {
        if (name.contains("uploadCrashlyticsMappingFile")) enabled = false
    }
}
