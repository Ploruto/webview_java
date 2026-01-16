plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    compileOnly("org.projectlombok:lombok:1.18.30")
    compileOnly("org.jetbrains:annotations:24.0.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "core"
            version = project.version.toString()
        }
    }
}

// Gradle 6–safe jar config
tasks.withType<Jar>().configureEach {
    from(sourceSets.main.get().output)
    from(
        fileTree("src/main/resources") {
            include("**/*.so", "**/*.dll", "**/*.dylib")
        }
    )
}

