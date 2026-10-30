plugins { id("spring.base") }

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":identity-service:domain"))
    implementation(libs.spring.security.core)
    implementation(libs.spring.security.crypto)
}
