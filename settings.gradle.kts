import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement { includeBuild("build-logic") }

rootProject.name = "Statistica"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven("https://registry.whereareiam.me/maven/packages")
        maven("https://repo.faststats.dev/releases")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        // Last, so it only answers for versions the registries do not have, such as a locally published "dev".
        mavenLocal()
    }
}

include("statistica-api")
include("statistica-common")
include("statistica-adapter:adapter-jdbc")
include("statistica-adapter:adapter-redis")
include("statistica-integration:integration-bstats")
include("statistica-integration:integration-faststats")
include("statistica-platform:platform-bungeecord")
include("statistica-platform:platform-velocity")
