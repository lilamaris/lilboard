plugins {
    id("spring.base")
    id("spring.test.h2")
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    testImplementation(libs.spring.boot.starter.flyway)
    implementation(project(":identity-service:domain"))
    implementation(project(":identity-service:application"))
    implementation(libs.spring.boot.starter.data.jpa)
}
