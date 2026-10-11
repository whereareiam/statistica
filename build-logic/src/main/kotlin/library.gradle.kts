plugins {
    `java-library`
    `maven-publish`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "me.whereareiam"
version = providers.environmentVariable("VERSION").orElse("dev").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

dependencies {
    "compileOnly"(libs.findLibrary("annotations").get())
    "compileOnly"(libs.findLibrary("lombok").get())

    "annotationProcessor"(libs.findLibrary("lombok").get())

    "testImplementation"(libs.findLibrary("junit").get())

    "testCompileOnly"(libs.findLibrary("annotations").get())

    "testRuntimeOnly"(libs.findLibrary("junit-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (!providers.gradleProperty("integrationTests").map(String::toBoolean).getOrElse(false))
            excludeTags("redis-container")
    }
}

// Members of a group are published under the group's name: adapter-jdbc in statistica-adapter
// becomes statistica-adapter-jdbc.
val publishedName: String = project.parent
    ?.takeIf { it != rootProject }
    ?.let { "${it.name}-${project.name.substringAfter('-')}" }
    ?: project.name

extra["publishedName"] = publishedName

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = publishedName
            pom {
                name.set(artifactId)
                description.set("Usage statistics and error reports for bStats and FastStats")
                url.set("https://github.com/whereareiam/statistica")
            }
        }
    }
    repositories {
        maven {
            val base = providers.environmentVariable("PUBLISH_MAVEN_BASE_URL").orElse("https://registry.whereareiam.me/maven").get()
            val repository = providers.environmentVariable("PUBLISH_MAVEN_REPOSITORY").orElse("packages").get()
            url = uri("$base/$repository")
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
    }
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(publishedName)
}
