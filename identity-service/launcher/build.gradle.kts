plugins {
    id("spring.base")
    id("spring.postgresql")
    id("spring.test.h2")
    alias(libs.plugins.spring.boot)
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":identity-service:domain"))
    implementation(project(":identity-service:application"))
    implementation(project(":identity-service:jpa"))
    implementation(project(":identity-service:web"))
    implementation(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.spring.security.test)
}
