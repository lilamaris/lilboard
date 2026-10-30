plugins { id("spring.web") }

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.jakarta.servlet.api)
    testImplementation(libs.spring.security.test)
    implementation(project(":board-service:domain"))
    implementation(project(":board-service:application"))
    implementation(project(":kernel"))
}
