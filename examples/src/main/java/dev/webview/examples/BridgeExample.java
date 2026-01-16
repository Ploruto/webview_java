package dev.webview.examples;

import dev.webview.Webview;
import dev.webview.bridge.JavascriptFunction;
import dev.webview.bridge.JavascriptObject;
import dev.webview.bridge.JavascriptValue;
import dev.webview.bridge.WebviewBridge;

/**
 * Example demonstrating the bridge/bind functionality.
 * 
 * Run with:
 *   ./gradlew :examples:run --args="bridge"
 */
public class BridgeExample {

    public static void main(String[] args) {
        try (Webview wv = new Webview(true)) {
            WebviewBridge bridge = new WebviewBridge(wv);

            wv.setTitle("Bridge Example");
            wv.setSize(800, 600, Webview.HINT_NONE);

            // Expose a Java object to JavaScript (must be done before setHtml)
            bridge.defineObject("App", new AppObject());
            
            String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <style>\n" +
                "    body { font-family: Arial; padding: 20px; }\n" +
                "    button { padding: 8px 16px; margin: 5px; cursor: pointer; }\n" +
                "    #counter { font-size: 24px; font-weight: bold; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h1>Bridge Example</h1>\n" +
                "  <p>Counter: <span id='counter'>0</span></p>\n" +
                "  <button onclick='App.increment()'>Increment</button>\n" +
                "  <button onclick='App.decrement()'>Decrement</button>\n" +
                "  <button onclick='App.reset()'>Reset</button>\n" +
                "  <p>Message: <input type='text' id='msg' value='Hello' /></p>\n" +
                "  <button onclick='App.showMessage()'>Show Message</button>\n" +
                "  <script>\n" +
                "    function updateCounter() {\n" +
                "      document.getElementById('counter').innerText = App.count;\n" +
                "    }\n" +
                "    // Update on load\n" +
                "    setTimeout(updateCounter, 100);\n" +
                "    // Periodically check counter\n" +
                "    setInterval(updateCounter, 500);\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";

            wv.setHtml(html);
            wv.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Example Java object exposed to JavaScript.
     */
    public static class AppObject extends JavascriptObject {
        @JavascriptValue
        public int count = 0;

        @JavascriptFunction
        public void increment() {
            count++;
            System.out.println("Counter incremented to: " + count);
        }

        @JavascriptFunction
        public void decrement() {
            count--;
            System.out.println("Counter decremented to: " + count);
        }

        @JavascriptFunction
        public void reset() {
            count = 0;
            System.out.println("Counter reset to 0");
        }

        @JavascriptFunction
        public void showMessage() {
            // This would need a way to call JS from Java
            System.out.println("showMessage() called from JavaScript");
        }
    }
}
