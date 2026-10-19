plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management.gradle.plugin)
}
