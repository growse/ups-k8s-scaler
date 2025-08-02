import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin)
  application
  alias(libs.plugins.graalvm)
  alias(libs.plugins.ktfmt)
}

group = "com.growse.k8s.upsEventHandler"

version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
  implementation(libs.k8s.client)
  implementation(libs.kotlin.result)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.clikt)
  implementation(libs.bundles.logging)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_24) } }

tasks.test { useJUnitPlatform() }

application {
  mainClass.set("com.growse.k8s.upsEventHandler.MainKt")
  applicationDefaultJvmArgs =
      listOf("-agentlib:native-image-agent=config-output-dir=/tmp/libnativeoutput")
}
