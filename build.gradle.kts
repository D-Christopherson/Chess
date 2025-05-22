plugins {
    kotlin("jvm") version "2.0.20"
    id("org.springframework.boot") version "3.4.3"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

apply(plugin = "io.spring.dependency-management")
repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.mockito:mockito-inline:2.8.47")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
}

tasks.test {
    useJUnitPlatform()
}