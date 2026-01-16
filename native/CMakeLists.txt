cmake_minimum_required(VERSION 3.16)
project(webview_jni C CXX)

set(WEBVIEW_SRC "/home/phaack/temp/webview")
set(WEBVIEW_BUILD_SHARED_LIBRARY ON)

# Add webview as a subdirectory
add_subdirectory("${WEBVIEW_SRC}/core" webview_core EXCLUDE_FROM_ALL)

# Get the shared library target
# It should be named "webview" per the CMakeLists.txt configuration
# We need to copy it to our output directory

# Get the output directory
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(OUTPUT_SUBDIR "Debug")
else()
    set(OUTPUT_SUBDIR "Release")
endif()

# Target specifications
if(APPLE)
    set(ARCH "aarch64-macos")
    set(EXT "dylib")
elseif(UNIX AND NOT APPLE)
    set(ARCH "x86_64-linux")
    set(EXT "so")
elseif(WIN32)
    set(ARCH "x86_64-windows")
    set(EXT "dll")
endif()

# Create output directory
set(OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../core/src/main/resources/dev/webview/natives/${ARCH}")
file(MAKE_DIRECTORY "${OUTPUT_DIR}")

# Custom post-build command to copy the library
add_custom_command(
    TARGET webview_core_shared POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy 
        "$<TARGET_FILE:webview_core_shared>"
        "${OUTPUT_DIR}/libwebview.${EXT}"
    COMMENT "Copying webview shared library to Java resources"
)
