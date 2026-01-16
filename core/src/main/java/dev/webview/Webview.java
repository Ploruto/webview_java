package dev.webview;

import com.sun.jna.Native;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Java wrapper for webview using JNA (Java Native Access).
 *
 * Provides a user-friendly API for creating and managing webview windows.
 * Uses JNA for dynamic C library binding, offering better compatibility
 * and stability compared to JNI.
 */
public class Webview implements Closeable {

    private static final WebviewNative NATIVE;

    static {
        NATIVE = loadNativeLibrary();
    }

    private static WebviewNative loadNativeLibrary() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String javaArch = System.getProperty("sun.arch.data.model");
        
        System.err.println("[webview] OS: " + osName + ", Architecture: " + arch + ", Java bits: " + javaArch);
        
        String resourcePath = null;
        String ext = null;
        String libName = "webview";

        // Determine platform
        if (osName.contains("linux")) {
            ext = ".so";
            libName = "webview";
            resourcePath = "/dev/webview/natives/x86_64-linux/libwebview.so";
        } else if (osName.contains("mac")) {
            ext = ".dylib";
            libName = "webview";
            // Detect macOS architecture
            if (arch.contains("aarch64") || arch.contains("arm")) {
                resourcePath = "/dev/webview/natives/aarch64-macos/libwebview.dylib";
            } else {
                resourcePath = "/dev/webview/natives/x86_64-macos/libwebview.dylib";
            }
        } else if (osName.contains("win")) {
            ext = ".dll";
            libName = "webview";
            resourcePath = "/dev/webview/natives/x86_64-windows/webview.dll";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }

        // Try to load bundled version from jar first
        if (resourcePath != null) {
            try {
                InputStream in = Webview.class.getResourceAsStream(resourcePath);
                if (in != null) {
                    File tempLib = File.createTempFile("webview", ext);
                    tempLib.deleteOnExit();
                    Files.copy(in, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    in.close();
                    System.err.println("[webview] Extracted bundled library to: " + tempLib.getAbsolutePath());
                    System.err.println("[webview] Bundled DLL size: " + tempLib.length() + " bytes");
                    
                    // Load the library directly into the process
                    System.load(tempLib.getAbsolutePath());
                    System.err.println("[webview] Successfully loaded native library via System.load()");
                    
                    // Now create JNA proxy pointing to the absolute path (already loaded)
                    return Native.load(tempLib.getAbsolutePath(), WebviewNative.class);
                } else {
                    System.err.println("[webview] Resource not found at: " + resourcePath);
                }
            } catch (IOException | UnsatisfiedLinkError ex) {
                System.err.println("[webview] Failed to load from jar: " + ex.getMessage());
                ex.printStackTrace();
                // Fall through to try system library
            }
        }

        // If bundled version failed, try system library
        System.err.println("[webview] Attempting to load system library: " + libName);
        return Native.load(libName, WebviewNative.class);
    }

    private long pointer;
    private boolean closed = false;
    private String initScript = "";
    private final Map<String, Function<String, String>> bindings = new HashMap<>();
    private final Map<String, WebviewNative.BindCallback> nativeCallbacks = new HashMap<>();

    /**
     * Creates a new Webview instance.
     *
     * @param debug Enable developer tools if true
     */
    public Webview(boolean debug) {
        System.err.println("[webview] Creating webview instance (debug=" + debug + ")");
        this.pointer = NATIVE.webview_create(debug ? 1 : 0, null);
        System.err.println("[webview] webview_create returned pointer: " + pointer);
        if (this.pointer == 0) {
            throw new RuntimeException("Failed to create webview instance. Check WebView2 runtime is installed on Windows.");
        }
    }

    /**
     * Destroys the webview instance.
     */
    public void destroy() {
        if (!closed && pointer != 0) {
            NATIVE.webview_destroy(pointer);
            closed = true;
        }
    }

    /**
     * Runs the main event loop (blocking).
     */
    public void run() {
        NATIVE.webview_run(pointer);
    }

    /**
     * Terminates the event loop.
     */
    public void terminate() {
        NATIVE.webview_terminate(pointer);
    }

    /**
     * Sets the window title.
     */
    public void setTitle(String title) {
        NATIVE.webview_set_title(pointer, title);
    }

    /**
     * Sets the window size.
     *
     * @param width  Window width in pixels
     * @param height Window height in pixels
     * @param hint   Size hint (HINT_NONE, HINT_MIN, HINT_MAX, HINT_FIXED)
     */
    public void setSize(int width, int height, int hint) {
        NATIVE.webview_set_size(pointer, width, height, hint);
    }

    /**
     * Navigates to a URL.
     */
    public void navigate(String url) {
        NATIVE.webview_navigate(pointer, url);
    }

    /**
     * Sets HTML content directly.
     */
    public void setHtml(String html) {
        NATIVE.webview_set_html(pointer, html);
    }

    /**
     * Injects JavaScript to be executed on page load.
     */
    public void init(String js) {
        NATIVE.webview_init(pointer, js);
    }

    /**
     * Sets the init script (executed before page load).
     * @param script JavaScript code to inject
     * @param prepend If true, prepends to existing init script; if false, replaces it
     */
    public void setInitScript(String script, boolean prepend) {
        if (prepend) {
            this.initScript = script + "\n" + this.initScript;
        } else {
            this.initScript = script;
        }
        if (pointer != 0) {
            NATIVE.webview_init(pointer, this.initScript);
        }
    }

    /**
     * Registers a binding callable from JavaScript.
     * @param name Name of the binding (accessible as window[name]())
     * @param handler Function that receives JSON string of arguments, returns JSON string result (or null)
     */
    public void bind(String name, Function<String, String> handler) {
        bindings.put(name, handler);
        
        // Create and store the callback to prevent garbage collection
        WebviewNative.BindCallback callback = (seq, req, arg) -> {
            try {
                String result = handler.apply(req);
                // Return the result asynchronously - empty string means undefined in JS
                NATIVE.webview_return(pointer, seq, 0, result != null ? result : "");
            } catch (Exception e) {
                e.printStackTrace();
                NATIVE.webview_return(pointer, seq, 1, "");
            }
        };
        nativeCallbacks.put(name, callback);
        
        // Register with native webview (pass null for arg since we don't use it)
        int errorCode = NATIVE.webview_bind(pointer, name, callback, null);
        if (errorCode != 0) {
            throw new RuntimeException("Failed to bind '" + name + "': error code " + errorCode);
        }
    }

    /**
     * Evaluates JavaScript asynchronously.
     */
    public void eval(String js) {
        NATIVE.webview_eval(pointer, js);
    }

    /**
     * Gets the native window handle (GtkWindow, NSWindow, HWND, etc).
     */
    public long getWindow() {
        return NATIVE.webview_get_window(pointer);
    }

    /**
     * Gets a native handle of specific kind.
     *
     * @param kind The kind of handle (NATIVE_HANDLE_UI_WINDOW, etc)
     * @return The native handle or 0 if not available
     */
    public long getNativeHandle(int kind) {
        return NATIVE.webview_get_native_handle(pointer, kind);
    }

    @Override
    public void close() throws IOException {
        destroy();
    }

    // Window size hint constants
    public static final int HINT_NONE = 0;
    public static final int HINT_MIN = 1;
    public static final int HINT_MAX = 2;
    public static final int HINT_FIXED = 3;

    // Native handle kind constants
    public static final int NATIVE_HANDLE_UI_WINDOW = 0;
    public static final int NATIVE_HANDLE_UI_WIDGET = 1;
    public static final int NATIVE_HANDLE_BROWSER_CONTROLLER = 2;
}
