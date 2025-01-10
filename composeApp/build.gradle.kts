import com.android.build.api.dsl.Packaging
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.security.crypto.ktx)
            implementation (libs.logback.android)
            implementation(libs.ktor.client.core)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(projects.shared)
            implementation(libs.koin.core)
            api(libs.multiplatformSettings.common)
            implementation(libs.ktor.client.okHttp)
            implementation(libs.bundles.voyager.common)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.commons.net)
            api(libs.ftpserver.core)
            api(libs.ktor.server.cio)
            api(libs.ktor.server.core)
            api(libs.ktor.http)
            implementation(libs.ktor.server.call.logging)
            implementation(libs.okio)
//            implementation(project(":rpc-common"))
            implementation(libs.kotlinx.rpc.krpc.client)
            implementation(libs.kotlinx.rpc.krpc.ktor.client)
            implementation(libs.kotlinx.rpc.krpc.serialization.json)

            implementation(libs.opentelemetry.sdk.extension.autoconfigure)
            implementation(libs.opentelemetry.exporter.otlp)
            implementation(libs.opentelemetry.semconv)
            implementation(libs.opentelemetry.ktor)

        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.slf4j.api)
            // Logback Classic (as the SLF4J backend)
            implementation(libs.logback.classic)

        }
    }
}

android {
    namespace = "live.jkbx.zeroshare"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "live.jkbx.zeroshare"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Excludes the file from all libraries

    // Alternatively, exclude from specific libraries:
    packaging {

        resources.excludes.add("META-INF/DEPENDENCIES")
        // Alternatively, exclude from specific libraries:
        resources.excludes.add("com/apache/ftpserver/ftpserver-core/1.2.0/META-INF/DEPENDENCIES")
        resources.excludes.add("com/apache/ftpserver/ftplet-api/1.2.0/META-INF/DEPENDENCIES")
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    implementation(libs.koin.android)
    implementation(libs.multiplatformSettings.common)
    implementation(libs.ktor.client.okHttp)
}

compose.desktop {
    application {
        mainClass = "live.jkbx.zeroshare.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ZeroShare"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "live.jkbx.zeroshare"
                iconFile.set(project.file("src/commonMain/composeResources/drawable/MyApp.icns"))
            }
            windows {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/neural.png"))
            }
        }
    }
}
