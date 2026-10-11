plugins {
    id("library")
}

dependencies {
    api(project(":statistica-common"))

    compileOnly(libs.bstats.base)

    testImplementation(libs.bstats.base)
}
