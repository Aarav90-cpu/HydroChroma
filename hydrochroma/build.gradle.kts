import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "com.example.hydrochroma"
    compileSdk = 36 // Android 14

    defaultConfig {
        minSdk = 33 // Android 13 (Tiramisu) - Required for RuntimeShader stability

        // If you want to support older phones later, we can lower this
        // and add checks, but for now, let's keep it simple.
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10" // Matches standard Compose versions
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // The core Compose libraries
    implementation(platform(libs.androidx.compose.bom.v20240200))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.backdrop)
    implementation(libs.capsule)
    implementation(libs.capsule)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            // Replace 'YourGitHubUsername' with your actual GitHub username!
            groupId = "com.arkstudios.hydrochroma"
            artifactId = "hydrochroma"
            version = "alpha2.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}