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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}