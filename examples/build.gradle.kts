plugins {
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("dev.webview.examples.HelloWorld")
}

// Never publish the examples module
tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}


