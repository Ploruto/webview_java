cmake_minimum_required(VERSION 3.16)
project(webview_jni C CXX)

# Allow WEBVIEW_SRC to be overridden via environment or command line
if(NOT DEFINED WEBVIEW_SRC)
    set(WEBVIEW_SRC "/home/phaack/temp/webview" CACHE PATH "Path to webview source")
endif()

set(WEBVIEW_BUILD_SHARED_LIBRARY ON)

# Detect architecture more accurately
if(APPLE)
    # Check for Apple Silicon vs Intel
    execute_process(
        COMMAND uname -m
        OUTPUT_VARIABLE MACHINE_ARCH
        OUTPUT_STRIP_TRAILING_WHITESPACE
    )
    if(MACHINE_ARCH STREQUAL "arm64")
        set(ARCH "aarch64-macos")
    else()
        set(ARCH "x86_64-macos")
    endif()
    set(EXT "dylib")
elseif(UNIX AND NOT APPLE)
    set(ARCH "x86_64-linux")
    set(EXT "so")
elseif(WIN32)
    set(ARCH "x86_64-windows")
    set(EXT "dll")
endif()

message(STATUS "Building for architecture: ${ARCH}")

# Option 1: If webview is available as subdirectory
if(EXISTS "${WEBVIEW_SRC}/core/CMakeLists.txt")
    add_subdirectory("${WEBVIEW_SRC}/core" webview_core EXCLUDE_FROM_ALL)
    
    # Create output directory
    set(OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../core/src/main/resources/dev/webview/natives/${ARCH}")
    file(MAKE_DIRECTORY "${OUTPUT_DIR}")
    
    # Custom post-build command to copy the library
    add_custom_command(
        TARGET webview_core_shared POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy 
            "$<TARGET_FILE:webview_core_shared>"
            "${OUTPUT_DIR}/libwebview.${EXT}"
        COMMENT "Copying webview shared library to Java resources (${ARCH})"
    )
else()
    # Option 2: Build wrapper directly (used by CI/CD)
    message(STATUS "Building wrapper directly without CMake subdirectory")
    
    # This is handled by the build scripts and CI/CD
    message(STATUS "Use build.sh or GitHub Actions for building")
endif()
