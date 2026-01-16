plugins {
    java
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("dev.webview.webview_java.example.Example")
}
