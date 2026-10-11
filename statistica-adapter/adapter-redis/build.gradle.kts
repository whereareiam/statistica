plugins {
    id("library")
}

dependencies {
    api(project(":statistica-api"))

    compileOnlyApi(libs.jedis)

    testImplementation(libs.jedis)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.redis)
}
