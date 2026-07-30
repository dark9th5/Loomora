import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? =
    providers.environmentVariable(name).orNull
        ?: keystoreProperties.getProperty(name)
        ?: keystoreProperties.getProperty(name.removePrefix("LOOMORA_").lowercase())

val releaseStoreFile = signingValue("LOOMORA_STORE_FILE")
val releaseStorePassword = signingValue("LOOMORA_STORE_PASSWORD")
val releaseKeyAlias = signingValue("LOOMORA_KEY_ALIAS")
val releaseKeyPassword = signingValue("LOOMORA_KEY_PASSWORD")

val releaseSigningInputs = mapOf(
    "LOOMORA_STORE_FILE" to releaseStoreFile,
    "LOOMORA_STORE_PASSWORD" to releaseStorePassword,
    "LOOMORA_KEY_ALIAS" to releaseKeyAlias,
    "LOOMORA_KEY_PASSWORD" to releaseKeyPassword
)
val hasCompleteReleaseSigning = releaseSigningInputs.values.all { !it.isNullOrBlank() }

android {
    namespace = "com.loomora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loomora"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasCompleteReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasCompleteReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("validateReleaseSigning") {
    group = "verification"
    description = "Fails when production release signing inputs are missing."

    doLast {
        val missingInputs = releaseSigningInputs
            .filterValues { it.isNullOrBlank() }
            .keys

        check(missingInputs.isEmpty()) {
            "Production release signing is not configured. Missing: " +
                missingInputs.joinToString() +
                ". Configure these as environment variables or in local keystore.properties. " +
                "Pull-request CI may run assembleRelease unsigned, but release publishing must not."
        }

        val configuredStoreFile = rootProject.file(requireNotNull(releaseStoreFile))
        check(configuredStoreFile.isFile) {
            "Production release signing keystore was configured but not found: " +
                configuredStoreFile.absolutePath
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:audio"))
    implementation(project(":core:offlineai"))
    implementation(project(":core:network"))
    implementation(files("../core/offlineai/libs/sherpa-onnx-1.13.4.aar"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:recorder"))
    implementation(project(":feature:library"))
    implementation(project(":feature:recordingdetail"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:subscription"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(project(":core:testing"))

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    debugImplementation(libs.compose.ui.tooling)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
