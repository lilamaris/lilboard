plugins {
    id("spring.base")
    id("spring.test.h2")
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(project(":board-service:domain"))
    implementation(project(":board-service:application"))
    implementation(project(":kernel"))
}
