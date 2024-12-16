import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm")
    id("idea")
    id("com.google.protobuf") version "0.9.4"
}

group = "org.example"
version = "1.0-SNAPSHOT"
val grpcVersion = "1.18.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")

    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.apache.logging.log4j:log4j-api:2.17.1")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1") // Это адаптер для SLF4J
    implementation("io.github.microutils:kotlin-logging:2.1.23")  // KotlinLogging

    compileOnly("javax.annotation:javax.annotation-api:1.2")

    testImplementation("io.grpc:grpc-testing:$grpcVersion") // gRPC testing utilities
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:2.23.0")
    testImplementation("org.assertj:assertj-core:3.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")


}

idea {
    module {
        sourceDirs.plusAssign(file("${projectDir}/src/generated/main/java"))
        sourceDirs.plusAssign(file("${projectDir}/src/generated/main/grpc"))
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.7.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
        }
    }
}


kotlin {
    jvmToolchain(8)
}