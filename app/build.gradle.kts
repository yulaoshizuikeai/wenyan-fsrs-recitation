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
        versionCode = 18
        versionName = "1.6.0"

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
        val buildDir = layout.buildDirectory.asFile.get()
        testClassesDirs += files("$buildDir/tmp/kotlin-classes/debugUnitTest")
        classpath += files(
            "$buildDir/tmp/kotlin-classes/debugUnitTest",
            "$buildDir/tmp/kotlin-classes/debug"
        )
    }
}

tasks.register("runInProcessTests") {
    dependsOn("compileDebugUnitTestKotlin", "compileDebugKotlin")
    doLast {
        val buildDir = layout.buildDirectory.asFile.get()
        val classDirs = listOf(
            file("$buildDir/tmp/kotlin-classes/debug"),
            file("$buildDir/tmp/kotlin-classes/debugUnitTest")
        )
        val allUrls = (classDirs + configurations.getByName("debugUnitTestRuntimeClasspath").files)
            .map { it.toURI().toURL() }
            .toTypedArray()

        val classLoader = URLClassLoader(allUrls, ClassLoader.getPlatformClassLoader())
        val junitCoreClass = classLoader.loadClass("org.junit.runner.JUnitCore")
        val testClassNames = listOf(
            "com.ancient.wenyan.DailyGoalsAndQueueTest",
            "com.ancient.wenyan.FSRSOptimizerTest",
            "com.ancient.wenyan.MultiClozeVariantAndIntensifiedFsrsTest",
            "com.ancient.wenyan.BookSelectionAndHeatmapTest",
            "com.ancient.wenyan.SequentialRecitationOrderTest",
            "com.ancient.wenyan.ActiveSessionPersistenceTest",
            "com.ancient.wenyan.Phase2Phase3FixesTest",
            "com.ancient.wenyan.RoadmapPhaseExecutionTest",
            "com.ancient.wenyan.TypeSafeDiagnosisTest",
            "com.ancient.wenyan.e2e.Tier1FeatureCoverageTest",
            "com.ancient.wenyan.e2e.Tier2BoundaryCornerCasesTest"
        )
        val testClasses = testClassNames.map { classLoader.loadClass(it) }.toTypedArray()

        val junitCore = junitCoreClass.getDeclaredConstructor().newInstance()
        val classArrayType = Class.forName("[Ljava.lang.Class;")
        val runMethod = junitCoreClass.getMethod("run", classArrayType)
        val result = runMethod.invoke(junitCore, testClasses)

        val wasSuccessful = result.javaClass.getMethod("wasSuccessful").invoke(result) as Boolean
        val runCount = result.javaClass.getMethod("getRunCount").invoke(result) as Int
        val failureCount = result.javaClass.getMethod("getFailureCount").invoke(result) as Int
        val failures = result.javaClass.getMethod("getFailures").invoke(result) as List<*>

        println("==================================================")
        println("TEST SUMMARY: Ran $runCount tests, Failures: $failureCount")
        println("==================================================")
        if (!wasSuccessful) {
            for (f in failures) {
                println("FAILURE: $f")
                val getException = f?.javaClass?.getMethod("getException")
                val ex = getException?.invoke(f) as? Throwable
                ex?.printStackTrace()
            }
            throw GradleException("Tests failed! ($failureCount failures)")
        }
    }
}




