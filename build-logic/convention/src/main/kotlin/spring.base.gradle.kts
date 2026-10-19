import extension.libs
import extension.requireLibrary

plugins {
    id("java.base")
    id("spring.platform")
}

dependencies {
    implementation(libs.requireLibrary("spring-tx"))
    implementation(libs.requireLibrary("spring-context"))
}
