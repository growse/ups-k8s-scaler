import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin)
  application
  alias(libs.plugins.ktfmt)
  jacoco
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
  testImplementation(libs.kotest.runner)
  testImplementation(libs.kotest.assertions)
  testImplementation(libs.kotlinx.coroutines.test)
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_24) } }

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
  }
}

tasks.jacocoTestCoverageVerification {
  violationRules { rule { limit { minimum = "0.0".toBigDecimal() } } }
}

application { mainClass.set("com.growse.k8s.upsEventHandler.MainKt") }
