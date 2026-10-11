plugins {
    id("platform")
}

// Velocity and FastStats' Velocity module require Java 21.
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }

dependencies {
    api(project(":statistica-integration:integration-bstats"))
    api(project(":statistica-integration:integration-faststats"))

    compileOnly(libs.velocity)

    "bstatsApi"(libs.bstats.velocity)

    "faststatsImplementation"(libs.faststats.velocity)
}
