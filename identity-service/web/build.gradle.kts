plugins { id("spring.web") }

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.jakarta.servlet.api)
    testImplementation(libs.spring.security.test)
    implementation(project(":identity-service:application"))
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.jose)
}
