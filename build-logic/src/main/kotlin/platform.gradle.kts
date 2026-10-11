plugins {
    id("library")
}

// A platform module carries the code for both backends. Each backend is an optional feature that
// brings its integration module and its library: a plugin asks for the capabilities it wants, and
// only those are pulled in.
//
//     implementation("me.whereareiam:statistica-platform-velocity:<version>") {
//         capabilities { requireCapability("me.whereareiam:statistica-platform-velocity-bstats") }
//     }
val publishedName: String by extra

listOf("bstats", "faststats").forEach { backend ->
    java.registerFeature(backend) {
        usingSourceSet(sourceSets["main"])
        capability(project.group.toString(), "$publishedName-$backend", project.version.toString())
    }
}
