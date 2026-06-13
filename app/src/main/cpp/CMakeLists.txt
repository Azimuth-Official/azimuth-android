cmake_minimum_required(VERSION 3.18)
project(azimuth_rtklib_jni C)

# Build vendored RTKLIB core as static library (Phase 3a source set)
add_subdirectory(rtklib-core)

# JNI shared library wrapping RTKLIB
add_library(azimuth_rtklib_jni SHARED rtklib_jni.c)

target_include_directories(azimuth_rtklib_jni PRIVATE rtklib-core)

target_link_libraries(azimuth_rtklib_jni
    rtklib          # static lib from rtklib-core/CMakeLists.txt
    log             # Android __android_log_print
    m               # math library
)
