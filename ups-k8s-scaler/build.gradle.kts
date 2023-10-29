import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    application
    alias(libs.plugins.graalvm)
    alias(libs.plugins.ktlint)
}

group = "com.growse.k8s.upsEventHandler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.k8s.client)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.result)
    implementation(libs.slf4j)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.clikt)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
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

ktlint {
    version.set("1.0.1")
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
}
