pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Loomora"

include(":app")

include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:audio")
include(":core:network")
include(":core:testing")

include(":feature:onboarding")
include(":feature:home")
include(":feature:recorder")
include(":feature:library")
include(":feature:recordingdetail")
include(":feature:editor")
include(":feature:settings")
include(":feature:subscription")
