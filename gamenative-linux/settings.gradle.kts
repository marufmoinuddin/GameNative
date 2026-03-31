pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    }
}

rootProject.name = "gamenative-linux"

include(":core:domain")
include(":core:runtime")
include(":core:store-steam")
include(":desktop:shell")
include(":cli")
include(":infra:config")
include(":infra:keyring")
include(":infra:network")
include(":infra:persistence")
include(":infra:notifications")
