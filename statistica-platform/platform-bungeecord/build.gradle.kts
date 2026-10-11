plugins {
    id("platform")
}

dependencies {
    api(project(":statistica-integration:integration-bstats"))
    api(project(":statistica-integration:integration-faststats"))

    compileOnly(libs.bungeecord)

    "bstatsImplementation"(libs.bstats.bungeecord)

    "faststatsImplementation"(libs.faststats.bungeecord)
}
