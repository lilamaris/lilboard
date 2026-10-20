import extension.libs
import extension.requireLibrary

plugins { id("spring.platform") }

dependencies {
    testRuntimeOnly(libs.requireLibrary("h2database"))

    testImplementation(libs.requireLibrary("spring-boot-testcontainers"))
    testImplementation(libs.requireLibrary("testcontainers-h2"))

    testImplementation(libs.requireLibrary("spring-boot-starter-data-jpa-test"))
}
