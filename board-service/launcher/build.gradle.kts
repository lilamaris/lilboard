plugins {
    id("spring.base")
    id("spring.postgresql")
    id("spring.test.h2")
    alias(libs.plugins.spring.boot)
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":board-service:domain"))
    implementation(project(":board-service:application"))
    implementation(project(":board-service:jpa"))
    implementation(libs.spring.boot.starter.webmvc)

    testImplementation(libs.archunit.junit5)
}
