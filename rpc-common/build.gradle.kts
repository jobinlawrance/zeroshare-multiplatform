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
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:0.4.0")
    implementation(libs.jedis)
    implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactive)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}