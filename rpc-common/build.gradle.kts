plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.rpc.plugin)

}

group = "live.jkbx.zeroshare"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.rpc.krpc.client)
    implementation(libs.jedis)
    implementation(libs.lettuce.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactive)

    implementation(libs.opentelemetry.sdk.extension.autoconfigure)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.semconv)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaExec> {
    environment("OTEL_METRICS_EXPORTER", "none")
    environment("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317/")
}