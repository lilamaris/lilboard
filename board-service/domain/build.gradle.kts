plugins {
    id("java.base")
    id("spring.platform")
    `java-library`
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    compileOnlyApi(libs.jakarta.persistence.api)

    implementation(project(":kernel"))
}
