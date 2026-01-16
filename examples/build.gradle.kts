plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("dev.webview.examples.BridgeExample")
}

// Never publish the examples module
tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}


