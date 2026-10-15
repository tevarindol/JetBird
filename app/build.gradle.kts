
import com.android.build.api.variant.FilterConfiguration
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.bg.jetbird"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "dev.bg.jetbird"
        minSdk = 24
        targetSdk = 37
        versionCode = 56
        versionName = "1.8.13"

        buildConfigField("String", "NETBIRD_COMMIT", getShortNetbirdCommit())
        buildConfigField("String", "NETBIRD_VERSION", getNetbirdVersionFromCommit())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        vectorDrawables {
            useSupportLibrary = true
            generatedDensities?.clear()
        }
    }

    externalNativeBuild {
        cmake {
            path = File("../jni/CMakeLists.txt")
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
}

val abiMap = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2,
    "x86" to 3,
    "x86_64" to 4
)

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val name = output.filters.find { f -> f.filterType == FilterConfiguration.FilterType.ABI }?.identifier
            val code = abiMap[name]
            if (code != null) {
                output.versionCode.set("${(output.versionCode.get() as Int? ?: 1)}${code}".toInt())
            }
        }
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
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Dependency Injection
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.androidx.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata)

    // Preferences
    implementation(libs.mmkv)

    // Logging
    implementation(libs.timber)

    // Misc
    implementation(libs.ipaddress)
    implementation(libs.permissions)
    implementation(libs.zxing)
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

tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}

interface InjectedExecOps {
    @Inject //@javax.inject.Inject
    fun getExecOps(): ExecOperations
}

fun getNetbirdCommit(): String {
    val stdout = ByteArrayOutputStream()
    objects.newInstance(InjectedExecOps::class).getExecOps().exec {
        commandLine("git", "submodule", "status")
        standardOutput = stdout
    }.assertNormalExitValue()
    return stdout.toString().trim().split(" ").first()
}

fun getShortNetbirdCommit(): String {
    return "\"${getNetbirdCommit().take(10).dropWhile { c -> c == '+' }}\""
}

fun getNetbirdVersionFromCommit(): String {
    val stdout = ByteArrayOutputStream()
    objects.newInstance(InjectedExecOps::class).getExecOps().exec {
        commandLine("git", "-C", "${rootDir}/netbird", "describe", "--tags", "--abbrev=0")
        standardOutput = stdout
    }.assertNormalExitValue()
    return "\"${stdout.toString().trim()}\""
}
