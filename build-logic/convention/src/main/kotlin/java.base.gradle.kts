import extension.libs
import extension.requireLibrary
import java.math.BigDecimal
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("java")
    id("jacoco")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<Test>().configureEach { useJUnitPlatform() }

jacoco { toolVersion = libs.requireLibrary("jacoco-agent").get().versionConstraint.requiredVersion }

tasks.named<Test>("test") { finalizedBy(tasks.named("jacocoTestReport")) }

val coverageMinimum =
    providers
        .gradleProperty("coverage.minimum")
        .map { BigDecimal(it) }
        .getOrElse(BigDecimal("0.80"))

val coverageExcludes =
    providers
        .gradleProperty("coverage.excludes")
        .map { value -> value.split(",").map { it.trim() } }
        .getOrElse(emptyList())

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = coverageMinimum
            }
            excludes = coverageExcludes
        }
    }
}

tasks.named("check") { dependsOn(tasks.named("jacocoTestCoverageVerification")) }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

dependencies {
    implementation(libs.requireLibrary("jspecify"))
    compileOnly(libs.requireLibrary("lombok"))
    annotationProcessor(libs.requireLibrary("lombok"))

    testImplementation(platform(libs.requireLibrary("junit-bom")))
    testImplementation(platform(libs.requireLibrary("mockito-bom")))

    testImplementation(libs.requireLibrary("junit-jupiter"))
    testRuntimeOnly(libs.requireLibrary("junit-platform-launcher"))

    testImplementation(libs.requireLibrary("assertj-core"))

    testImplementation(libs.requireLibrary("mockito-core"))
    testImplementation(libs.requireLibrary("mockito-junit-jupiter"))
}
