plugins {
    id("spring.base")
    alias(libs.plugins.spring.boot)
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":board-service:domain"))
    implementation(project(":board-service:application"))
    implementation(libs.spring.boot.starter.webmvc)
}
