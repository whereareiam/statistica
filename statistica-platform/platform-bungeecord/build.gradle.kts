plugins {
    id("platform")
}

dependencies {
    api(project(":statistica-common"))

    compileOnly(libs.bungeecord)

    "bstatsApi"(project(":statistica-integration:integration-bstats"))
    "bstatsImplementation"(libs.bstats.bungeecord)

    "faststatsApi"(project(":statistica-integration:integration-faststats"))
    "faststatsImplementation"(libs.faststats.bungeecord)
}
