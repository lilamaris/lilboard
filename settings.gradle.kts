pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories { mavenCentral() }
}

rootProject.name = "lilboard"

include("kernel")

include("board-service:domain")

include("board-service:application")

include("board-service:launcher")
