group = "dev.webview"
version = "0.11.0"

subprojects {

    repositories {
        mavenCentral()
    }

    // Apply plugins explicitly
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    // Java config (Gradle 6 compatible)
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

