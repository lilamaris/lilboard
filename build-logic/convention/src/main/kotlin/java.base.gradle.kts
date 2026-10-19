import extension.libs
import extension.requireLibrary

plugins { id("java") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<Test>().configureEach { useJUnitPlatform() }

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
