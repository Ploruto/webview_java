package dev.webview.examples;

import dev.webview.Webview;
import dev.webview.bridge.JavascriptFunction;
import dev.webview.bridge.JavascriptObject;
import dev.webview.bridge.JavascriptValue;
import dev.webview.bridge.WebviewBridge;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating the bridge/bind functionality with events.
 * 
 * Run with:
 *   ./gradlew :examples:run --args="bridge"
 */
public class BridgeExample {

    public static void main(String[] args) {
        try (Webview wv = new Webview(true)) {
            WebviewBridge bridge = new WebviewBridge(wv);

            wv.setTitle("Bridge Example - Events Demo");
            wv.setSize(900, 700, Webview.HINT_NONE);

            // Create the app object with a reference to the bridge for events
            AppObject app = new AppObject(bridge);
            
            // Expose a Java object to JavaScript (must be done before setHtml)
            bridge.defineObject("App", app);
            
            String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <style>\n" +
                "    body { font-family: Arial; padding: 20px; background: #f5f5f5; }\n" +
                "    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                "    h1 { color: #333; margin-top: 0; }\n" +
                "    h2 { color: #666; font-size: 18px; margin-top: 30px; border-bottom: 2px solid #eee; padding-bottom: 10px; }\n" +
                "    button { padding: 10px 20px; margin: 5px; cursor: pointer; border: none; border-radius: 4px; font-size: 14px; }\n" +
                "    button:hover { opacity: 0.8; }\n" +
                "    .btn-primary { background: #007bff; color: white; }\n" +
                "    .btn-danger { background: #dc3545; color: white; }\n" +
                "    .btn-success { background: #28a745; color: white; }\n" +
                "    .btn-info { background: #17a2b8; color: white; }\n" +
                "    #counter { font-size: 32px; font-weight: bold; color: #007bff; }\n" +
                "    #events-log { background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px; padding: 15px; max-height: 200px; overflow-y: auto; font-family: monospace; font-size: 12px; }\n" +
                "    .event-entry { margin: 5px 0; padding: 5px; border-left: 3px solid #007bff; padding-left: 10px; }\n" +
                "    .event-counterChanged { border-left-color: #28a745; }\n" +
                "    .event-notification { border-left-color: #ffc107; background: #fff3cd; }\n" +
                "    .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin: 20px 0; }\n" +
                "    .stat-card { background: #e9ecef; padding: 15px; border-radius: 4px; text-align: center; }\n" +
                "    .stat-value { font-size: 24px; font-weight: bold; color: #495057; }\n" +
                "    .stat-label { font-size: 12px; color: #6c757d; text-transform: uppercase; }\n" +
                "    input { padding: 8px; border: 1px solid #ced4da; border-radius: 4px; margin-right: 5px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class='container'>\n" +
                "    <h1>🌉 Bridge Example with Events</h1>\n" +
                "    \n" +
                "    <h2>Counter (Polling)</h2>\n" +
                "    <p>Current value: <span id='counter'>0</span></p>\n" +
                "    <div>\n" +
                "      <button class='btn-success' onclick='App.increment()'>➕ Increment</button>\n" +
                "      <button class='btn-danger' onclick='App.decrement()'>➖ Decrement</button>\n" +
                "      <button class='btn-info' onclick='App.reset()'>🔄 Reset</button>\n" +
                "    </div>\n" +
                "    \n" +
                "    <h2>Events (Push Notifications)</h2>\n" +
                "    <div class='stats'>\n" +
                "      <div class='stat-card'>\n" +
                "        <div class='stat-value' id='event-count'>0</div>\n" +
                "        <div class='stat-label'>Total Events</div>\n" +
                "      </div>\n" +
                "      <div class='stat-card'>\n" +
                "        <div class='stat-value' id='counter-events'>0</div>\n" +
                "        <div class='stat-label'>Counter Changes</div>\n" +
                "      </div>\n" +
                "      <div class='stat-card'>\n" +
                "        <div class='stat-value' id='notifications'>0</div>\n" +
                "        <div class='stat-label'>Notifications</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div>\n" +
                "      <input type='text' id='msg' value='Hello from JavaScript!' style='width: 300px;' />\n" +
                "      <button class='btn-primary' onclick='sendCustomMessage()'>📤 Send Message</button>\n" +
                "    </div>\n" +
                "    \n" +
                "    <h2>Event Log</h2>\n" +
                "    <div id='events-log'>\n" +
                "      <div style='color: #6c757d; font-style: italic;'>Waiting for events...</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  \n" +
                "  <script>\n" +
                "    let eventCount = 0;\n" +
                "    let counterEventCount = 0;\n" +
                "    let notificationCount = 0;\n" +
                "    \n" +
                "    function updateCounter() {\n" +
                "      document.getElementById('counter').innerText = App.count;\n" +
                "    }\n" +
                "    \n" +
                "    function addEventToLog(eventType, data) {\n" +
                "      const log = document.getElementById('events-log');\n" +
                "      const entry = document.createElement('div');\n" +
                "      entry.className = 'event-entry event-' + eventType;\n" +
                "      \n" +
                "      const timestamp = new Date().toLocaleTimeString();\n" +
                "      entry.innerHTML = `<strong>[${timestamp}]</strong> ${eventType}: ${JSON.stringify(data)}`;\n" +
                "      \n" +
                "      // Clear 'waiting' message if present\n" +
                "      if (log.children.length === 1 && log.children[0].style.fontStyle === 'italic') {\n" +
                "        log.innerHTML = '';\n" +
                "      }\n" +
                "      \n" +
                "      log.insertBefore(entry, log.firstChild);\n" +
                "      \n" +
                "      // Keep only last 20 entries\n" +
                "      while (log.children.length > 20) {\n" +
                "        log.removeChild(log.lastChild);\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    function updateStats() {\n" +
                "      document.getElementById('event-count').innerText = eventCount;\n" +
                "      document.getElementById('counter-events').innerText = counterEventCount;\n" +
                "      document.getElementById('notifications').innerText = notificationCount;\n" +
                "    }\n" +
                "    \n" +
                "    // Listen for counter change events from Java\n" +
                "    Bridge.on('counterChanged', (data) => {\n" +
                "      console.log('[JS] counterChanged event received:', data);\n" +
                "      eventCount++;\n" +
                "      counterEventCount++;\n" +
                "      addEventToLog('counterChanged', data);\n" +
                "      updateStats();\n" +
                "      \n" +
                "      // Update counter display immediately (no polling needed!)\n" +
                "      document.getElementById('counter').innerText = data.newValue;\n" +
                "    });\n" +
                "    \n" +
                "    // Listen for notification events from Java\n" +
                "    Bridge.on('notification', (data) => {\n" +
                "      console.log('[JS] notification event received:', data);\n" +
                "      eventCount++;\n" +
                "      notificationCount++;\n" +
                "      addEventToLog('notification', data);\n" +
                "      updateStats();\n" +
                "      \n" +
                "      // Show browser notification if available\n" +
                "      if ('Notification' in window && Notification.permission === 'granted') {\n" +
                "        new Notification('Bridge Notification', { body: data.message });\n" +
                "      }\n" +
                "    });\n" +
                "    \n" +
                "    // Send custom message to Java\n" +
                "    async function sendCustomMessage() {\n" +
                "      const msg = document.getElementById('msg').value;\n" +
                "      await App.sendMessage(msg);\n" +
                "    }\n" +
                "    \n" +
                "    // Initial counter update\n" +
                "    setTimeout(updateCounter, 100);\n" +
                "    \n" +
                "    // Fallback polling (only needed because properties don't auto-update yet)\n" +
                "    // In production, you'd use events for everything!\n" +
                "    setInterval(updateCounter, 1000);\n" +
                "    \n" +
                "    console.log('[JS] Bridge example initialized with event listeners');\n" +
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
     * Example Java object exposed to JavaScript with event emission.
     */
    public static class AppObject extends JavascriptObject {
        private final WebviewBridge bridge;
        
        @JavascriptValue
        public int count = 0;

        public AppObject(WebviewBridge bridge) {
            this.bridge = bridge;
        }

        @JavascriptFunction
        public void increment() {
            int oldValue = count;
            count++;
            System.out.println("Counter incremented to: " + count);
            
            // Update property cache automatically (prevents flickering!)
            bridge.emitPropertyUpdate(this, "count", count);
            
            // Also emit custom event with more details
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("oldValue", oldValue);
            eventData.put("newValue", count);
            eventData.put("operation", "increment");
            bridge.emit("counterChanged", eventData);
        }

        @JavascriptFunction
        public void decrement() {
            int oldValue = count;
            count--;
            System.out.println("Counter decremented to: " + count);
            
            // Update property cache automatically (prevents flickering!)
            bridge.emitPropertyUpdate(this, "count", count);
            
            // Also emit custom event with more details
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("oldValue", oldValue);
            eventData.put("newValue", count);
            eventData.put("operation", "decrement");
            bridge.emit("counterChanged", eventData);
        }

        @JavascriptFunction
        public void reset() {
            int oldValue = count;
            count = 0;
            System.out.println("Counter reset to 0");
            
            // Update property cache automatically (prevents flickering!)
            bridge.emitPropertyUpdate(this, "count", count);
            
            // Also emit custom event with more details
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("oldValue", oldValue);
            eventData.put("newValue", count);
            eventData.put("operation", "reset");
            bridge.emit("counterChanged", eventData);
        }

        @JavascriptFunction
        public void sendMessage(String message) {
            System.out.println("Message received from JavaScript: " + message);
            
            // Send notification event back to JavaScript
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("message", "Java received: " + message);
            notificationData.put("timestamp", System.currentTimeMillis());
            bridge.emit("notification", notificationData);
        }
    }
}
