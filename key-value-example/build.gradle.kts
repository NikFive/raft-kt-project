import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":raft"))
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
    implementation("io.github.microutils:kotlin-logging:2.1.23")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

tasks.withType<ShadowJar> {
    manifest.attributes.apply {
        put("Main-Class", "org.example.KeyValue")
    }
    from("src/main/resources/log4j2.xml")
}