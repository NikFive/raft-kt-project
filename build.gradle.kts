plugins {
    kotlin("jvm") version "2.0.21"
    id("jacoco")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

jacoco {
    toolVersion = "0.8.12"
}

/*
tasks.register<JacocoReport>("codeCoverageReport") {
    executionData(fileTree(project.rootDir.absolutePath).include/*("**/build/jacoco/*.exec"))

    subprojects.forEach {
        sourceSets(it.sourceSets["main"])
    }

    reports {
        xml.apply { isEnabled = true }
        xml.setDestination(file("${buildDir}/reports/jacoco"))
        html.apply {
            isEnabled = true
        }
        html.setDestination(file("${buildDir}/reports/jacoco"))
        csv.apply { isEnabled = false }
    }
}

tasks.named("codeCoverageReport").configure {
    dependsOn(subprojects.map { it.tasks.named<Test>("test") })
}
*/