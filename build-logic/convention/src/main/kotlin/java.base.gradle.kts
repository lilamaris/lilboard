import extension.libs
import extension.requireLibrary
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("java")
    id("jacoco")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<Test>().configureEach { useJUnitPlatform() }

jacoco { toolVersion = libs.requireLibrary("jacoco-agent").get().versionConstraint.requiredVersion }

tasks.named<Test>("test") { finalizedBy(tasks.named("jacocoTestReport")) }

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
