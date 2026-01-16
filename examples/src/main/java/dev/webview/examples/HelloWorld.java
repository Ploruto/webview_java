package dev.webview.examples;

import dev.webview.Webview;

/**
 * Simple "Hello, World!" example using JNA-based webview wrapper.
 */
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Creating webview window...");

        try (Webview webview = new Webview(true)) {
            webview.setTitle("Hello from Java!");
            webview.setSize(640, 480, Webview.HINT_NONE);

            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "  <style>\n" +
                    "    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }\n" +
                    "    h1 { color: #333; }\n" +
                    "    button { padding: 10px 20px; font-size: 16px; cursor: pointer; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <h1>Hello from Webview! &#128075;</h1>\n" +
                    "  <p>This is a Java application with an embedded web browser.</p>\n" +
                    "  <button onclick=\"alert('Clicked from HTML!')\">Click Me</button>\n" +
                    "</body>\n" +
                    "</html>";

            webview.setHtml(html);

            System.out.println("Window created. Running event loop...");
            webview.run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Done!");
    }
}
