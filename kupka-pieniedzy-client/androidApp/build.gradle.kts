import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }

// Wersja z version.properties (jedyne źródło prawdy dla Android i iOS)
val versionProperties =
    Properties().apply {
        val file = rootProject.file("version.properties")
        if (file.exists()) {
            load(file.inputStream())
        }
    }
val appVersionName: String = versionProperties.getProperty("VERSION_NAME", "1.0")
val appVersionCode: Int = versionProperties.getProperty("VERSION_CODE", "1").toInt()

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.sd.kupka_pieniedzy_client"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sd.kupka_pieniedzy_client"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
