import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.lillah.dhikr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lillah.dhikr"
        minSdk = 26
        targetSdk = 35
        // versionCode must only ever increase; Android rejects a downgrade install.
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Backend credentials come from a gitignored backend.properties (see backend/README.md).
        // Absent, the app builds and runs as a purely local counter; present, it gains accounts,
        // sync and the Universal Dhikr board. Nothing here is baked into version control.
        val backendPropertiesFile = rootProject.file("backend.properties")
        val backend = Properties().apply {
            if (backendPropertiesFile.exists()) {
                backendPropertiesFile.inputStream().use { load(it) }
            }
        }
        fun backendValue(key: String): String = backend.getProperty(key).orEmpty()

        buildConfigField("String", "FIREBASE_API_KEY", "\"${backendValue("firebaseApiKey")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${backendValue("firebaseAppId")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${backendValue("firebaseProjectId")}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${backendValue("firebaseSenderId")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${backendValue("googleWebClientId")}\"")
        buildConfigField("String", "FACEBOOK_APP_ID", "\"${backendValue("facebookAppId")}\"")
        buildConfigField("String", "FACEBOOK_CLIENT_TOKEN", "\"${backendValue("facebookClientToken")}\"")

        // The Facebook SDK reads these from resources. Placeholders keep the manifest valid in a
        // build with no Facebook app; auto-init is off, so the SDK never sees them.
        resValue("string", "facebook_app_id", backendValue("facebookAppId").ifBlank { "0" })
        resValue(
            "string",
            "facebook_client_token",
            backendValue("facebookClientToken").ifBlank { "0" },
        )
        resValue(
            "string",
            "fb_login_protocol_scheme",
            backendValue("facebookAppId").ifBlank { "0" }.let { "fb$it" },
        )
    }

    // Signing has two jobs here, in priority order.
    //
    // 1. Continuity. Android refuses to install an update signed by a different key than the
    //    installed app, so builds default to the key that signed the released v1.0.0 APK
    //    (signing/dhikr-upgrade.jks, SHA-256 35:DA:2A:FB...). Without it, existing users could
    //    not update in place, which is the one thing this release must not break.
    // 2. Publishing. A keystore.properties in the project root overrides the continuity key.
    //    It is gitignored, and no publishing secret is committed.
    //
    // Note: switching to a publishing key breaks in-place updates for anyone who sideloaded
    // v1.0.0. See README, "Signing and upgrade continuity".
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val continuityKeystore = rootProject.file("signing/dhikr-upgrade.jks")
    val releaseProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

    signingConfigs {
        create("upgrade") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(releaseProperties.getProperty("storeFile"))
                storePassword = releaseProperties.getProperty("storePassword")
                keyAlias = releaseProperties.getProperty("keyAlias")
                keyPassword = releaseProperties.getProperty("keyPassword")
            } else {
                storeFile = continuityKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("upgrade")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Compose screens are rendered under Robolectric in unit tests, which needs the
            // merged resources and manifest on the test classpath.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    implementation(libs.facebook.login)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
