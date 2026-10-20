plugins { id("spring.web") }

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":board-service:domain"))
    implementation(project(":board-service:application"))
}
