plugins {
    id("platform")
}

// Velocity and FastStats' Velocity module require Java 21.
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }

dependencies {
    api(project(":statistica-common"))

    compileOnly(libs.velocity)

    "bstatsApi"(project(":statistica-integration:integration-bstats"))
    "bstatsApi"(libs.bstats.velocity)

    "faststatsApi"(project(":statistica-integration:integration-faststats"))
    "faststatsImplementation"(libs.faststats.velocity)
}
