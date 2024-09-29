import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.bg.jetbird"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.bg.jetbird"
        minSdk = 24
        targetSdk = 34
        versionCode = 7
        versionName = "1.0.6"

        buildConfigField("String", "NETBIRD_COMMIT", getNetbirdCommit())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        vectorDrawables {
            useSupportLibrary = true
            generatedDensities?.clear()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs.keepDebugSymbols.add("**/*.so") // NetBird binary already stripped by "-s -w" linker flags
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        aaptOptions.cruncherEnabled = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))

    // Netbird binding
    implementation(projects.lib)

    // UI
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)

    // Dependency Injection
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.androidx.compiler)
    ksp(libs.hilt.compiler)

    // Preferences
    implementation(libs.mmkv)

    // Logging
    implementation(libs.timber)

    // Misc
    implementation(libs.ipaddress)
    debugImplementation(libs.leakCanary)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.all {
    exclude("com.google.android.gms") // Just in case
}

fun getNetbirdCommit(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "submodule", "status")
        standardOutput = stdout
    }
    return "\"${stdout.toString().trim().split(" ").first().take(10).dropWhile { c -> c == '+' }}\""
}
