import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val supportedAbis = listOf("armeabi-v7a", "arm64-v8a", "x86_64")
val requiredNativeLibraries = listOf("libc++_shared.so", "liblivetracks_audio.so", "liboboe.so")

android {
    namespace = "com.thomrnowtea.livetracks"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.thomrnowtea.livetracks"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 3
        versionName = providers.environmentVariable("VERSION_NAME").orNull ?: "0.2.0-alpha.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-Wall", "-Wextra", "-Werror")
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
        ndk {
            abiFilters += supportedAbis
        }
    }

    signingConfigs {
        create("release") {
            providers.environmentVariable("RELEASE_STORE_FILE").orNull?.let { storeFile = file(it) }
            storeType = providers.environmentVariable("RELEASE_STORE_TYPE").orNull ?: "PKCS12"
            storePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "LiveTracks Debug")
        }
        release {
            isMinifyEnabled = false
            resValue("string", "app_name", "LiveTracks")
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        prefab = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

fun registerAbiVerificationTask(taskName: String, variantName: String) {
    tasks.register(taskName) {
        group = "verification"
        description = "Verifies that the $variantName APK contains every supported native ABI."
        dependsOn("assemble${variantName.replaceFirstChar(Char::uppercaseChar)}")

        doLast {
            val apk = layout.buildDirectory
                .file("outputs/apk/$variantName/app-$variantName.apk")
                .get()
                .asFile
            check(apk.isFile) { "APK not found: ${apk.absolutePath}" }

            val entries = mutableSetOf<String>()
            ZipFile(apk).use { zip ->
                val iterator = zip.entries()
                while (iterator.hasMoreElements()) {
                    entries += iterator.nextElement().name
                }
            }

            val missing = supportedAbis.flatMap { abi ->
                requiredNativeLibraries
                    .map { library -> "lib/$abi/$library" }
                    .filterNot(entries::contains)
            }
            check(missing.isEmpty()) {
                "APK is missing required native libraries: ${missing.joinToString()}"
            }
        }
    }
}

registerAbiVerificationTask("verifyDebugApkAbis", "debug")
registerAbiVerificationTask("verifyReleaseApkAbis", "release")

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.oboe)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
