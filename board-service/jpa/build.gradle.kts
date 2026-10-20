plugins {
    id("spring.base")
    id("spring.postgresql")
    id("spring.test.h2")
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":board-service:domain"))
    implementation(project(":board-service:application"))
    implementation(libs.spring.boot.starter.data.jpa)
}
