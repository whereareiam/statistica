plugins {
    id("library")
}

dependencies {
    api(project(":statistica-api"))
    api(libs.strata.adapter.database)

    testImplementation(libs.h2)
    testImplementation(libs.strata)
    testImplementation(libs.sqlite)
}
