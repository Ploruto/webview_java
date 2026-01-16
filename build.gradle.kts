// Root project: aggregator only (NOT published)

group = "dev.webview"
version = "0.13.0"

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenCentral()
    }
}

project(":core") {
    // Publishable Java library
    apply(plugin = "java-library")

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

    tasks.jar {
        from(sourceSets["main"].output)
        // Include native libraries
        from(
            fileTree("src/main/resources") {
                include("**/*.so", "**/*.dll", "**/*.dylib")
            }
        )
    }
}

project(":examples") {
    // Application module — NOT published
    apply(plugin = "application")

    dependencies {
        implementation(project(":core"))
    }

    configure<JavaApplication> {
        mainClass = "dev.webview.examples.HelloWorld"
    }

    tasks.register<JavaExec>("runHello") {
        classpath = sourceSets["main"].runtimeClasspath
        mainClass = "dev.webview.examples.HelloWorld"
    }

    // Explicitly disable publishing for examples
    tasks.withType<PublishToMavenRepository>().configureEach {
        enabled = false
    }
}

