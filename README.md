# webview_java

Java bindings for the [webview](https://github.com/webview/webview) library.

Built with SWIG + JNI for cross-platform support.

## Building

```bash
mvn clean package
```

The core module will be packaged as `webview_java-core-0.13.0.jar` with native libraries included.

## Usage

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.webview</groupId>
    <artifactId>webview_java-core</artifactId>
    <version>0.13.0</version>
</dependency>
```

### Example

```java
import dev.webview.webview_java.Webview;

public class App {
    public static void main(String[] args) {
        try (Webview wv = new Webview(true)) {
            wv.setTitle("My App");
            wv.setSize(800, 600, Webview.HINT_NONE);
            wv.navigate("https://example.com");
            wv.run();
        }
    }
}
```

## Supported Platforms

- Linux x86_64 (with GTK 3/4 and WebKit2GTK)
- Windows x86_64 (with WebView2)
- macOS x86_64 and aarch64 (with WebKit)
