plugins {
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    //mainClassName = "dev.webview.examples.HelloWorld"
}

// Disable publishing completely
tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}

