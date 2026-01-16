package dev.webview;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import java.util.Collections;

/**
 * JNA interface for webview C library.
 *
 * Declares native functions from the webview C library.
 * JNA automatically maps these to the corresponding C functions
 * in the shared library at runtime.
 */
public interface WebviewNative extends Library {

    // Don't initialize immediately; let Webview.loadNativeLibrary() handle it first
    // WebviewNative INSTANCE = Native.load("webview", WebviewNative.class);

    /**
     * Creates a new webview instance.
     *
     * @param debug Enable developer tools (1 = true, 0 = false)
     * @param window Optional native window handle (GTK window pointer, NSWindow pointer, HWND, etc)
     * @return Pointer to webview instance, or 0 on failure
     */
    long webview_create(int debug, Object window);

    /**
     * Destroys a webview instance and closes the window.
     *
     * @param w Webview instance pointer
     */
    void webview_destroy(long w);

    /**
     * Runs the main event loop until terminated.
     *
     * @param w Webview instance pointer
     */
    void webview_run(long w);

    /**
     * Stops the main event loop.
     *
     * @param w Webview instance pointer
     */
    void webview_terminate(long w);

    /**
     * Sets the window title.
     *
     * @param w Webview instance pointer
     * @param title New window title
     */
    void webview_set_title(long w, String title);

    /**
     * Sets the window size.
     *
     * @param w Webview instance pointer
     * @param width New width in pixels
     * @param height New height in pixels
     * @param hints Size hints (WEBVIEW_HINT_NONE, WEBVIEW_HINT_MIN, WEBVIEW_HINT_MAX, WEBVIEW_HINT_FIXED)
     */
    void webview_set_size(long w, int width, int height, int hints);

    /**
     * Navigates to a URL or data URI.
     *
     * @param w Webview instance pointer
     * @param url URL to navigate to
     */
    void webview_navigate(long w, String url);

    /**
     * Sets HTML content directly.
     *
     * @param w Webview instance pointer
     * @param html HTML content
     */
    void webview_set_html(long w, String html);

    /**
     * Injects JavaScript to be executed on page load.
     *
     * @param w Webview instance pointer
     * @param js JavaScript code
     */
    void webview_init(long w, String js);

    /**
     * Evaluates JavaScript asynchronously.
     *
     * @param w Webview instance pointer
     * @param js JavaScript code
     */
    void webview_eval(long w, String js);

    /**
     * Gets the native window handle.
     *
     * @param w Webview instance pointer
     * @return Native window handle (GtkWindow*, NSWindow*, HWND, etc)
     */
    long webview_get_window(long w);

    /**
     * Gets a native handle of specific kind.
     *
     * @param w Webview instance pointer
     * @param kind The kind of handle to retrieve
     * @return The native handle or 0 if not available
     */
    long webview_get_native_handle(long w, int kind);
}
