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
        // For other objects, try to convert to JSON
        return new JSONObject().put("value", obj).get("value").toString();
    }
}
