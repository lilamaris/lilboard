plugins {
    id("java.base")
    id("spring.web")
    id("spring.test.h2")
    `java-library`
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":board-service:web"))
    implementation(libs.spring.boot.starter.security)
    api(libs.spring.web)
    api(libs.spring.jdbc)

    api(project(":board-service:domain"))
    api(project(":board-service:application"))

    implementation(libs.uuid.creator)
    implementation(project(":kernel"))

    testImplementation(libs.spring.boot.starter.data.jpa)
}
