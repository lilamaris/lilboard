import extension.libs
import extension.requireLibrary

plugins { id("spring.base") }

dependencies {
    implementation(libs.requireLibrary("spring-boot-starter-actuator"))
    implementation(libs.requireLibrary("spring-boot-starter-opentelemetry"))
    implementation(libs.requireLibrary("micrometer-registry-prometheus"))

    implementation(libs.requireLibrary("datasource-micrometer-spring-boot"))
    implementation(libs.requireLibrary("datasource-micrometer-opentelemetry"))
}
