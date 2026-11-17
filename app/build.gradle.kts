import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun local(name: String): String = localProps.getProperty(name, "").replace("\"", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.pay2own"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pay2own"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseStore = local("pay2own.storeFile")
    if (releaseStore.isNotBlank()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStore)
                storePassword = local("pay2own.storePassword")
                keyAlias = local("pay2own.keyAlias")
                keyPassword = local("pay2own.keyPassword")
                // Setup-wizard provisioning reads the JAR signature. v2 alone fails with
                // "Can't set up device" after the phone downloads the app.
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080\"")
            buildConfigField("String", "SENTRY_DSN", "\"${local("pay2own.sentryDsn")}\"")
            buildConfigField("String", "CERT_PIN_SHA256", "\"\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.pay2own.app\"")
            buildConfigField("String", "SENTRY_DSN", "\"${local("pay2own.sentryDsn")}\"")
            buildConfigField("String", "CERT_PIN_SHA256", "\"${local("pay2own.certPinSha256")}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseStore.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.androidx.work)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.serialization.converter)
    implementation(libs.coroutines.android)
    implementation(libs.sentry.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.configureEach {
    val packagesRelease = name == "assembleRelease" || name == "bundleRelease" || name == "packageRelease"
    if (packagesRelease) {
        doFirst {
            if (local("pay2own.certPinSha256").isBlank()) {
                throw GradleException(
                    "Release builds pin api.pay2own.app. Set pay2own.certPinSha256 in local.properties."
                )
            }
            if (local("pay2own.storeFile").isBlank()) {
                throw GradleException(
                    "Release builds are signed. Set pay2own.storeFile, pay2own.storePassword, pay2own.keyAlias and pay2own.keyPassword in local.properties."
                )
            }
        }
    }
}
