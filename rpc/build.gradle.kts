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
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)

    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.server)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)

    implementation(libs.jedis)
    implementation(project(":rpc-common"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "live.jkbx.zeroshare.rpc.ApplicationKt"
}

//ktor {
//
//    docker {
//        jreVersion = JavaVersion.VERSION_21
//        localImageName = "zeroshare-krpc"
//        imageTag = "1.0.0"
//    }
//}

jib {
    from {
        image = "eclipse-temurin:21-jdk"
    }
    to {
        image = "jobinlawrance/zeroshare-krpc:1.0.10"
    }

    container {
        mainClass = "live.jkbx.zeroshare.rpc.ApplicationKt"
    }
}