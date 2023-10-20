import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    application
    id("org.graalvm.buildtools.native") version "0.9.28"
}

group = "com.growse.k8s.upsEventHandler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.kubernetes:client-java:18.0.1")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.github.ajalt.clikt:clikt:3.5.4")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("com.growse.k8s.upsEventHandler.MainKt")
    applicationDefaultJvmArgs = listOf("-agentlib:native-image-agent=config-output-dir=/tmp/libnativeoutput")
}
