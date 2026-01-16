package dev.webview.bridge;

import dev.webview.Webview;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaScript bridge for Webview. Exposes Java objects to JavaScript.
 * 
 * Usage:
 * <pre>
 * Webview wv = new Webview(true);
 * WebviewBridge bridge = new WebviewBridge(wv);
 * bridge.defineObject("myObj", new MyObject());
 * </pre>
 */
public class WebviewBridge {
    private static String bridgeScript = "";
    static {
        try {
            bridgeScript = loadBridgeScript();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String loadBridgeScript() throws IOException {
        return new String(WebviewBridge.class
            .getResourceAsStream("/dev/webview/bridge/BridgeScript.js")
            .readAllBytes(), StandardCharsets.UTF_8);
    }

    private final Webview webview;
    private final Map<String, JavascriptObject> objects = new HashMap<>();

    public WebviewBridge(Webview webview) {
        this.webview = webview;
        
        // Set up the binding handler
        webview.bind("__bridgeInternal", this::handleBridgeMessage);
        
        // Inject bridge script
        rebuildInitScript();
    }

    /**
     * Exposes a Java object to JavaScript.
     */
    public void defineObject(String name, JavascriptObject obj) {
        objects.put(name, obj);
        
        // Get the init script for this object
        String initScript = obj.getInitScript(name, this);
        
        // Inject immediately via eval (for objects added after page load)
        webview.eval(initScript);
        
        // Also update the init script for future page loads
        rebuildInitScript();
    }

    void registerObject(String name, JavascriptObject obj) {
        objects.put(name, obj);
    }

    /**
     * Emit an event to JavaScript.
     * Dispatches a CustomEvent that can be listened to with Bridge.on()
     * 
     * @param eventType The event type/name
     * @param data The event data (will be JSON serialized)
     */
    public void emit(String eventType, Object data) {
        try {
            String jsonData = toJson(data);
            // Escape single quotes and backslashes for JavaScript string
            String escapedType = eventType.replace("\\", "\\\\").replace("'", "\\'");
            
            // Call Bridge.__internal.dispatch() which will fire the event
            String script = String.format(
                "if (window.Bridge && window.Bridge.__internal && window.Bridge.__internal.dispatch) {" +
                "  window.Bridge.__internal.dispatch('%s', %s);" +
                "}",
                escapedType,
                jsonData
            );
            
            webview.eval(script);
        } catch (Exception e) {
            System.err.println("[WebviewBridge] Error emitting event '" + eventType + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Emit a property update event to synchronize JavaScript property cache.
     * This automatically updates the cached property value in JavaScript.
     * 
     * @param obj The JavascriptObject whose property changed
     * @param propertyName The name of the property that changed
     * @param newValue The new value of the property
     */
    public void emitPropertyUpdate(JavascriptObject obj, String propertyName, Object newValue) {
        Map<String, Object> data = new HashMap<>();
        data.put("objectId", obj.getId());
        data.put("property", propertyName);
        data.put("value", newValue);
        emit("propertyUpdated", data);
    }

    private void rebuildInitScript() {
        List<String> init = new ArrayList<>();
        init.add(bridgeScript);
        
        // Add all root-level objects
        for (Map.Entry<String, JavascriptObject> entry : new ArrayList<>(objects.entrySet())) {
            if (!entry.getKey().contains(".")) {
                String script = entry.getValue().getInitScript(entry.getKey(), this);
                init.add(script);
            }
        }
        
        webview.setInitScript(String.join("\n\n", init), false);
    }

    private String handleBridgeMessage(String json) {
        try {
            // Parse the JSON array: [type, data]
            JSONArray args = new JSONArray(json);
            String type = args.getString(0);
            JSONObject data = args.getJSONObject(1);
            
            switch (type) {
                case "GET": {
                    String id = data.getString("id");
                    String property = data.getString("property");
                    JavascriptObject obj = findObject(id);
                    if (obj == null) {
                        System.err.println("[WebviewBridge] Object not found: " + id);
                        return null;
                    }
                    try {
                        Object result = obj.get(property);
                        String jsonResult = toJson(result);
                        System.out.println("[WebviewBridge] GET " + id + "." + property + " = " + result + " -> JSON: " + jsonResult);
                        return jsonResult;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                
                case "SET": {
                    String id = data.getString("id");
                    String property = data.getString("property");
                    Object value = data.get("newValue");
                    JavascriptObject obj = findObject(id);
                    if (obj != null) {
                        try {
                            obj.set(property, value);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
                
                case "INVOKE": {
                    String id = data.getString("id");
                    String function = data.getString("function");
                    JSONArray arguments = data.getJSONArray("arguments");
                    JavascriptObject obj = findObject(id);
                    if (obj == null) {
                        System.err.println("[WebviewBridge] Object not found: " + id);
                        return null;
                    }
                    try {
                        Object result = obj.invoke(function, jsonArrayToObjectArray(arguments));
                        return toJson(result);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                
                default:
                    System.err.println("[WebviewBridge] Unknown message type: " + type);
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JavascriptObject findObject(String id) {
        for (JavascriptObject obj : objects.values()) {
            if (obj.getId().equals(id)) return obj;
        }
        return null;
    }

    private Object[] jsonArrayToObjectArray(JSONArray arr) {
        Object[] result = new Object[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            result[i] = arr.get(i);
        }
        return result;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof String) {
            return JSONObject.quote((String) obj);
        }
        if (obj instanceof Map) {
            // Convert Map to JSONObject
            return new JSONObject((Map<?, ?>) obj).toString();
        }
        if (obj instanceof Iterable) {
            // Convert List/Collection to JSONArray
            JSONArray arr = new JSONArray();
            for (Object item : (Iterable<?>) obj) {
                arr.put(item);
            }
            return arr.toString();
        }
        // For other objects, try to wrap in JSONObject
        return new JSONObject().put("value", obj).toString();
    }
}
