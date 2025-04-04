import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.skie)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

val serverId= gradleLocalProperties(rootDir, providers).getProperty("serverId","")
val backendUrl= gradleLocalProperties(rootDir, providers).getProperty("backendUrl","")

kotlin {
    // https://kotlinlang.org/docs/multiplatform-expect-actual.html#expected-and-actual-classes
    // To suppress this warning about usage of expected and actual classes
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(libs.touchlab.kermit.simple)
        }

    }
    
    jvm()

    sourceSets {

        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.multiplatformSettings.common)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)
            api(libs.touchlab.kermit)
            implementation(libs.touchlab.kermit.koin)
            implementation(libs.bundles.ktor.common)
            implementation(libs.touchlab.skie.annotations)
            api(libs.kmpnotifier)
            api(libs.filekit.core)
            api(libs.filekit.compose)
            api(libs.kotlin.stdlib)
            implementation(libs.ktor.client.core) // For the Ktor client
            api(libs.ktor.client.socket)
            implementation(libs.kotlinx.rpc.krpc.ktor.client)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            api(libs.ktor.server.cio)
            api(libs.ktor.server.core)
            api(libs.ktor.http)
        }

        iosMain.dependencies {
            api(libs.touchlab.kermit.simple)
        }

        nativeMain.dependencies {
            implementation(libs.ktor.client.ios)
        }

        jvmMain.dependencies {

            implementation(libs.commons.compress) // For extracting tar.gz
            implementation(libs.retrofit)
            implementation(libs.converter.kotlinx.serialization)
            implementation(libs.adapter.rxjava)
            implementation(libs.okhttp)
            implementation(libs.logging.interceptor)
            implementation(libs.tika.core)
            implementation(libs.opentelemetry.sdk.extension.autoconfigure)
            implementation(libs.opentelemetry.exporter.otlp)
            implementation(libs.opentelemetry.semconv)
            implementation(libs.opentelemetry.ktor)
        }
        getByName("commonMain") {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "live.jkbx.zeroshare.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        resValue("string",
            "serverId",
            "\"" + serverId + "\"")
        resValue("string",
            "backendUrl",
            "\"" + backendUrl + "\"")
    }

    buildFeatures {
        compose=true
    }

    packaging {
        resources {
            excludes += "**/*"
        }
    }

    dependencies {
        implementation(libs.koin.android)
        implementation(libs.koin.androidx.compose)
        implementation(libs.ktor.client.okHttp)
        api(files("../libs/mobileNebula.aar"))
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)
        implementation(libs.googleid)
        implementation(libs.androidx.security.crypto.ktx)
        implementation(libs.androidx.media3.exoplayer)
        implementation(libs.tika.core)
        implementation(libs.opentelemetry.sdk.extension.autoconfigure)
        implementation(libs.opentelemetry.exporter.otlp)
        implementation(libs.opentelemetry.semconv)
        implementation(libs.opentelemetry.ktor)
    }

}
dependencies {
    implementation(libs.googleid)
    implementation(libs.androidx.security.crypto.ktx)
    implementation(libs.androidx.media3.exoplayer)

}

tasks.withType<JavaExec> {
    environment("OTEL_METRICS_EXPORTER", "none")
    environment("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317/")
}