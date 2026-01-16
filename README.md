# webview_java

Java bindings for the [webview/webview](https://github.com/webview/webview) library. Cross-platform desktop applications with embedded web UIs.

## Quick Start

### Add Dependency

Add JitPack repository to `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.ploruto:webview_java:v0.16.0")
}
```

### Basic Example

```java
import dev.webview.Webview;

public class App {
    public static void main(String[] args) {
        try (Webview wv = new Webview(true)) {
            wv.setTitle("My App");
            wv.setSize(800, 600, Webview.HINT_NONE);
            wv.setHtml("<html><body><h1>Hello from Java!</h1></body></html>");
            // or wv.navigate(..url)
            wv.run();
        }
    }
}
```

### With JavaScript Bridge

```java
import dev.webview.Webview;
import dev.webview.bridge.WebviewBridge;
import dev.webview.bridge.JavascriptFunction;

public class App {
    public static void main(String[] args) {
        try (Webview wv = new Webview(true)) {
            WebviewBridge bridge = new WebviewBridge(wv);
            bridge.defineObject("App", new AppObject());
            
            wv.setTitle("Bridge Example");
            wv.setSize(800, 600, Webview.HINT_NONE);
            wv.setHtml("<html><body><button onclick='App.onClick()'>Click</button></body></html>");
            wv.run();
        }
    }

    static class AppObject {
        @JavascriptFunction
        public void onClick() {
            System.out.println("Clicked from JavaScript!");
        }
    }
}
```

## Features

- **Cross-platform**: Linux (GTK 3/4 + WebKit2GTK), Windows (WebView2), macOS (WebKit)
- **Bi-directional JS Bridge**: Call Java from JS and vice versa
- **Easy HTML rendering**: Use `setHtml()` or navigate with `navigate()`

## Documentation

See `examples/` for more complete examples including the bridge pattern with events.

## Supported Platforms

- Linux x86_64 (GTK 3/4, WebKit2GTK)
- Windows x86_64 (WebView2)
- macOS x86_64 & aarch64 (WebKit)
