/*
 * Webview JNA wrapper for C API
 * 
 * This file includes the webview header which provides both the C API declarations
 * and their C++ implementations. The C API symbols are exported for JNA to use.
 */

#include "webview/webview.h"

/* Force instantiation of C API functions by creating dummy references.
 * This ensures symbols are exported for JNA to call.
 */
namespace {
    // Create a function that references all the webview C API functions.
    // This forces the compiler to instantiate them.
    void force_export() __attribute__((used));
    void force_export() {
        // These are never called, but their existence forces symbol instantiation
        (void)&webview_create;
        (void)&webview_destroy;
        (void)&webview_run;
        (void)&webview_terminate;
        (void)&webview_dispatch;
        (void)&webview_get_window;
        (void)&webview_get_native_handle;
        (void)&webview_set_title;
        (void)&webview_set_size;
        (void)&webview_navigate;
        (void)&webview_set_html;
        (void)&webview_init;
        (void)&webview_eval;
        (void)&webview_bind;
        (void)&webview_unbind;
        (void)&webview_return;
    }
}
