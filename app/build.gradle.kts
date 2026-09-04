import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.nav.graph)
    jacoco
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.ajrpachon.chatapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ajrpachon.chatapp"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        fun secret(key: String) = "\"${localProperties.getProperty(key, "")}\""
        buildConfigField("String", "SUPABASE_URL", secret("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_ANON_KEY", secret("SUPABASE_ANON_KEY"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", secret("GOOGLE_WEB_CLIENT_ID"))
        buildConfigField("String", "LIVEKIT_URL", secret("LIVEKIT_URL"))
        // LIVEKIT_API_KEY and LIVEKIT_API_SECRET removed — token generation moved to Edge Function
        buildConfigField("String", "GIPHY_API_KEY", secret("GIPHY_API_KEY"))
    }

    signingConfigs {
        create("release") {
            // Local dev: read from local.properties (see local.properties.example).
            // CI: fall back to environment variables so no secrets need to live in the repo.
            // RELEASE_KEYSTORE_BASE64, if set, is decoded to a temp file — lets CI avoid
            // committing/mounting a raw .jks file.
            fun secret(propertyKey: String, envKey: String) =
                localProperties.getProperty(propertyKey, "").ifBlank { System.getenv(envKey).orEmpty() }

            val storeFilePath = localProperties.getProperty("RELEASE_STORE_FILE", "").ifBlank {
                System.getenv("RELEASE_KEYSTORE_BASE64")?.let { base64 ->
                    if (base64.isBlank()) {
                        null
                    } else {
                        val decoded = layout.buildDirectory.file("release-signing/upload-keystore.jks").get().asFile
                        decoded.parentFile?.mkdirs()
                        decoded.writeBytes(Base64.getDecoder().decode(base64))
                        decoded.absolutePath
                    }
                }.orEmpty()
            }

            val storePw = secret("RELEASE_STORE_PASSWORD", "RELEASE_KEYSTORE_PASSWORD")
            val alias = secret("RELEASE_KEY_ALIAS", "RELEASE_KEY_ALIAS")
            val keyPw = secret("RELEASE_KEY_PASSWORD", "RELEASE_KEY_PASSWORD")

            // All fields are optional so the build doesn't break for contributors without a
            // keystore (e.g. debug work, CI jobs that only assemble debug). assembleRelease
            // will fail at sign time with a clear "keystore not found" if these are empty —
            // see docs/release-signing.md.
            if (storeFilePath.isNotBlank()) {
                // Relative paths (e.g. "upload-keystore.jks" in local.properties) resolve
                // against the project root, matching where the keystore is generated.
                storeFile = rootProject.file(storeFilePath)
            }
            storePassword = storePw
            keyAlias = alias
            keyPassword = keyPw
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles("benchmark-rules.pro")
            isDebuggable = false
            isProfileable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all { test ->
                test.maxHeapSize = "3g"
                test.jvmArgs("-XX:+UseG1GC", "-XX:MaxMetaspaceSize=1g", "-XX:+HeapDumpOnOutOfMemoryError")
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-Xopt-in=kotlin.time.ExperimentalTime"))
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

navgraph {
    renderThumbnails.set(true)
}

ksp {
    arg("navgraph.annotatedOnly", "true")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html"))
    }
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(
                "**/R.class", "**/R\$*.class", "**/BuildConfig.*",
                "**/Manifest*.*", "**/*Test*.*", "android/**/*.*",
                "**/*\$Lambda\$*.*", "**/*\$inlined*.*",
                "**/ui/theme/**", "**/*_Factory*.*", "**/*_HiltComponents*.*"
            )
        }
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        layout.buildDirectory.file(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
        )
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.security.crypto)

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Activity + Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation 3
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.sqlcipher.android)
    implementation(libs.play.integrity)
    ksp(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    // Supabase BOM + plugins
    val supabaseBom = platform(libs.supabase.bom)
    implementation(supabaseBom)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)

    // LiveKit
    implementation(libs.livekit.android)

    // Media3 / ExoPlayer (inline video player)
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // Coil (image loading)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)

    // Google Sign-In
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.3")

    // QR Code generation (pure Kotlin, no native deps)
    implementation("io.github.g0dkar:qrcode-kotlin:4.1.1")

    // QR Code scanning via ZXing
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Firebase
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.messaging)

    // Baseline profiles — pre-compiles Compose hot paths for faster startup
    implementation(libs.profileinstaller)

    // Tests
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    // MigrationTestHelper needs Instrumentation in every constructor overload (even the
    // driver/KMP-style one) — it cannot run as a local JVM/Robolectric unit test, only here.
    androidTestImplementation(libs.androidx.room.testing)
}
