plugins {
    id("java.base")
    id("spring.platform")
}

group = "com.lilamaris.lilboard"

version = "0.0.1-SNAPSHOT"

dependencies { compileOnly(libs.jakarta.persistence.api) }
