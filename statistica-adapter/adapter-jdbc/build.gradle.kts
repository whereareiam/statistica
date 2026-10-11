plugins {
    id("library")
}

dependencies {
    api(project(":statistica-api"))

    // Only schema() needs it; an application that creates the table another way does not.
    compileOnlyApi(libs.strata.adapter.database)

    testImplementation(libs.h2)
    testImplementation(libs.strata)
    testImplementation(libs.strata.adapter.database)
    testImplementation(libs.sqlite)
}
