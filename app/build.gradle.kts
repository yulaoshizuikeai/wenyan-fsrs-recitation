import java.net.URLClassLoader
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.ancient.wenyan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ancient.wenyan"
        minSdk = 26
        targetSdk = 34
        versionCode = 20
        versionName = "1.6.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val keystorePropertiesFile = rootProject.file("local.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

    val releaseStorePath = System.getenv("KEYSTORE_FILE")
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: keystoreProperties.getProperty("KEYSTORE_FILE")
        ?: keystoreProperties.getProperty("RELEASE_STORE_FILE")
        ?: keystoreProperties.getProperty("storeFile")
        ?: "${rootDir}/wenyan-release.jks"

    val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD")
        ?: System.getenv("RELEASE_STORE_PASSWORD")
        ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
        ?: keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
        ?: keystoreProperties.getProperty("storePassword")
        ?: ""

    val releaseKeyAlias = System.getenv("KEY_ALIAS")
        ?: System.getenv("RELEASE_KEY_ALIAS")
        ?: keystoreProperties.getProperty("KEY_ALIAS")
        ?: keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
        ?: keystoreProperties.getProperty("keyAlias")
        ?: "wenyan"

    val releaseKeyPassword = System.getenv("KEY_PASSWORD")
        ?: System.getenv("RELEASE_KEY_PASSWORD")
        ?: keystoreProperties.getProperty("KEY_PASSWORD")
        ?: keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
        ?: keystoreProperties.getProperty("keyPassword")
        ?: ""

    signingConfigs {
        create("release") {
            storeFile = file(releaseStorePath)
            storePassword = releaseStorePassword.ifEmpty { "wenyanpassword" }
            keyAlias = releaseKeyAlias.ifEmpty { "wenyan" }
            keyPassword = releaseKeyPassword.ifEmpty { "wenyanpassword" }
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.jvmArgs("-Dfile.encoding=UTF-8")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose BOM & UI
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Duolingo-style Confetti / Particle Animation Library
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt Dependency Injection
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // WorkManager for Daily Reminders
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.json:json:20231013")

    // Debug UI Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

afterEvaluate {
    tasks.named<Test>("testDebugUnitTest").configure {
        doFirst {
            val rootPath = rootDir.canonicalPath
            val hasNonAscii = rootPath.any { it.code > 127 }
            if (hasNonAscii) {
                val linkDir = File(System.getProperty("java.io.tmpdir"), "wenyan_test_root")
                if (!linkDir.exists()) {
                    try {
                        ProcessBuilder("cmd.exe", "/c", "mklink", "/J", linkDir.absolutePath, rootPath).start().waitFor()
                    } catch (_: Exception) {}
                }
                if (linkDir.exists()) {
                    val asciiBase = linkDir.canonicalPath
                    testClassesDirs = files(testClassesDirs.files.map { f ->
                        val p = f.canonicalPath
                        if (p.startsWith(rootPath)) File(asciiBase + p.substring(rootPath.length)) else f
                    })
                    classpath = files(classpath.files.map { f ->
                        val p = f.canonicalPath
                        if (p.startsWith(rootPath)) File(asciiBase + p.substring(rootPath.length)) else f
                    })
                }
            }
        }
    }
}






