plugins {
    id("library")
}

dependencies {
    api(project(":statistica-common"))

    compileOnly(libs.faststats.core)
    compileOnly(libs.gson)

    testImplementation(libs.faststats.config)
    testImplementation(libs.faststats.core)
    testImplementation(libs.gson)
}
