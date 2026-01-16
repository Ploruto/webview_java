#!/bin/bash
# Build script for webview JNA wrapper
# Compiles the webview library for Linux x86_64, macOS, and Windows
#
# Usage:
#   ./build.sh                    # Build for current platform
#   WEBVIEW_SRC=/path build.sh    # Override webview source location
#   ./build.sh --all              # Attempt to build all platforms (requires cross-compilers)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"

# Allow overriding webview source location
if [ -z "$WEBVIEW_SRC" ]; then
    WEBVIEW_SRC="/home/phaack/temp/webview"
fi

OUTPUT_DIR="$SCRIPT_DIR/../core/src/main/resources/dev/webview/natives"

# Create output directories
mkdir -p "$BUILD_DIR"
mkdir -p "$OUTPUT_DIR/x86_64-linux"
mkdir -p "$OUTPUT_DIR/aarch64-macos"
mkdir -p "$OUTPUT_DIR/x86_64-windows"

echo "Building webview JNA wrapper..."

# Linux x86_64
if command -v g++ &> /dev/null; then
    echo "Building Linux x86_64..."
    
    # Get GTK/WebKit includes and libs
    GTK_CFLAGS=$(pkg-config --cflags gtk+-3.0 webkit2gtk-4.1 2>/dev/null || echo "-I/usr/include/gtk-3.0 -I/usr/include/webkit2gtk-4.1")
    GTK_LIBS=$(pkg-config --libs gtk+-3.0 webkit2gtk-4.1 2>/dev/null || echo "-lgtk-3 -lwebkit2gtk-4.1")
    
    # Compile with C++ compiler and proper flags for header-only library
    g++ -std=c++11 -fPIC -shared \
        -I"$WEBVIEW_SRC/core/include" \
        $GTK_CFLAGS \
        -Wl,--export-dynamic \
        -o "$BUILD_DIR/libwebview.so" \
        "$SCRIPT_DIR/webview_wrapper.cc" \
        $GTK_LIBS -ldl
    
    cp "$BUILD_DIR/libwebview.so" "$OUTPUT_DIR/x86_64-linux/"
    echo "✓ Linux x86_64 built"
fi

# macOS (requires clang)
if command -v clang &> /dev/null && [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Building macOS..."
    
    clang -fPIC -shared \
        -I"$WEBVIEW_SRC/core/include" \
        -framework Cocoa \
        -framework WebKit \
        -o "$BUILD_DIR/libwebview.dylib" \
        "$SCRIPT_DIR/webview_wrapper.c"
    
    cp "$BUILD_DIR/libwebview.dylib" "$OUTPUT_DIR/aarch64-macos/"
    echo "✓ macOS built"
fi

# Windows (requires MinGW or MSVC)
if command -v x86_64-w64-mingw32-gcc &> /dev/null; then
    echo "Building Windows..."
    
    x86_64-w64-mingw32-gcc -fPIC -shared \
        -I"$WEBVIEW_SRC/core/include" \
        -o "$BUILD_DIR/webview.dll" \
        "$SCRIPT_DIR/webview_wrapper.c" \
        -lole32 -lcomctl32 -lws2_32
    
    cp "$BUILD_DIR/webview.dll" "$OUTPUT_DIR/x86_64-windows/"
    echo "✓ Windows built"
fi

echo ""
echo "Build complete! Output in: $OUTPUT_DIR"
echo ""
echo "=== Built libraries ==="
find "$OUTPUT_DIR" -type f \( -name "*.so" -o -name "*.dylib" -o -name "*.dll" \) -exec ls -lh {} \;
echo ""
echo "Note: GitHub Actions will build all platforms automatically on push."
