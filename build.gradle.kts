plugins {
    kotlin("jvm") version "2.4.0"
    id("org.springframework.boot") version "4.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

apply(plugin = "io.spring.dependency-management")
repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}
val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // I'll be using the /actuator/health endpoint to return happy noises to the ALB's health checker
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.0")
    testImplementation("org.springframework.boot:spring-boot-resttestclient:4.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient:4.1.0")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

tasks.test {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("chess.jar")
}