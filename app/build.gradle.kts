import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val supabaseProperties = Properties().apply {
    val configFile = rootProject.file("supabase.properties")
    check(configFile.isFile) {
        "Brakuje lokalnego pliku supabase.properties. Skopiuj supabase.properties.example i uzupełnij konfigurację środowiska."
    }
    configFile.inputStream().use(::load)
}

fun supabaseSetting(name: String): String =
    supabaseProperties.getProperty(name)?.takeIf(String::isNotBlank)
        ?: error("Brakuje wartości $name w supabase.properties")

val devSupabaseProperties = Properties().apply {
    val configFile = rootProject.file("supabase-dev.properties")
    if (configFile.isFile) {
        configFile.inputStream().use(::load)
    }
}

fun devSupabaseSetting(name: String, fallback: String): String =
    devSupabaseProperties.getProperty(name)?.takeIf(String::isNotBlank) ?: fallback

val releaseSigningProperties = Properties().apply {
    val configFile = rootProject.file("keystore.properties")
    if (configFile.isFile) {
        configFile.inputStream().use(::load)
    }
}

val hasReleaseSigning = releaseSigningProperties.isNotEmpty()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}
android {
    namespace = "pl.razem.myszy"
    compileSdk = 36
    defaultConfig {
        applicationId = "pl.razem.myszy"
        minSdk = 26
        targetSdk = 35
        versionCode = 39
        versionName = "0.9.7-balance-fix"
        manifestPlaceholders["appLabel"] = "Myszy"
        buildConfigField("boolean", "IS_DEV", "false")
    }
    flavorDimensions += "environment"
    productFlavors {
        create("private") {
            dimension = "environment"
            manifestPlaceholders["appLabel"] = "Myszy"
        }
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appLabel"] = "Myszy DEV"
            buildConfigField("boolean", "IS_DEV", "true")
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"${devSupabaseSetting("SUPABASE_URL", "https://missing-dev-config.invalid")}\""
            )
            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                "\"${devSupabaseSetting("SUPABASE_PUBLISHABLE_KEY", "missing-dev-key")}\""
            )
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("C:/Users/Admin/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("upload") {
                storeFile = rootProject.file(
                    requireNotNull(releaseSigningProperties.getProperty("storeFile")) {
                        "Brakuje storeFile w keystore.properties"
                    }
                )
                storePassword = requireNotNull(releaseSigningProperties.getProperty("storePassword")) {
                    "Brakuje storePassword w keystore.properties"
                }
                keyAlias = requireNotNull(releaseSigningProperties.getProperty("keyAlias")) {
                    "Brakuje keyAlias w keystore.properties"
                }
                keyPassword = requireNotNull(releaseSigningProperties.getProperty("keyPassword")) {
                    "Brakuje keyPassword w keystore.properties"
                }
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("upload")
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
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseSetting("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${supabaseSetting("SUPABASE_PUBLISHABLE_KEY")}\"")
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-android:3.4.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
