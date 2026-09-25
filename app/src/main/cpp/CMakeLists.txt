cmake_minimum_required(VERSION 3.22.1)
project("chessin")

# Define Stockfish core static library from submodule sources
set(STOCKFISH_DIR "${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src")

# Collect all Stockfish .cpp files except main.cpp
file(GLOB STOCKFISH_SOURCES "${STOCKFISH_DIR}/*.cpp")
list(FILTER STOCKFISH_SOURCES EXCLUDE REGEX "main\\.cpp$")

# Add NNUE sources
file(GLOB STOCKFISH_NNUE_SOURCES "${STOCKFISH_DIR}/nnue/*.cpp")
file(GLOB STOCKFISH_NNUE_FEATURES_SOURCES "${STOCKFISH_DIR}/nnue/features/*.cpp")
list(APPEND STOCKFISH_SOURCES ${STOCKFISH_NNUE_SOURCES} ${STOCKFISH_NNUE_FEATURES_SOURCES})

# Add Syzygy sources
file(GLOB STOCKFISH_SYZYGY_SOURCES "${STOCKFISH_DIR}/syzygy/*.cpp")
list(APPEND STOCKFISH_SOURCES ${STOCKFISH_SYZYGY_SOURCES})

# Add universal sources (exclude architecture-specific and embedded NNUE)
file(GLOB STOCKFISH_UNIVERSAL_SOURCES "${STOCKFISH_DIR}/universal/*.cpp")
list(FILTER STOCKFISH_UNIVERSAL_SOURCES EXCLUDE REGEX "entry_x86\\.cpp$")
list(FILTER STOCKFISH_UNIVERSAL_SOURCES EXCLUDE REGEX "entry_riscv64\\.cpp$")
list(FILTER STOCKFISH_UNIVERSAL_SOURCES EXCLUDE REGEX "nnue_embed\\.cpp$")
list(APPEND STOCKFISH_SOURCES ${STOCKFISH_UNIVERSAL_SOURCES})

add_library(
        stockfish-core
        STATIC
        ${STOCKFISH_SOURCES}
)

# Set include directory for Stockfish
target_include_directories(
        stockfish-core
        PUBLIC
        "${STOCKFISH_DIR}"
)

# Set C++ standard and compilation flags for Stockfish
target_compile_options(
        stockfish-core
        PRIVATE
        -std=c++17
        -O2
        -fexceptions
)

# JNI shared library that exposes only explicit JNI entry points
add_library(
        stockfish-jni
        SHARED
        native-lib_real.cpp
)

# Include directories for JNI library
target_include_directories(
        stockfish-jni
        PRIVATE
        "${STOCKFISH_DIR}"
)

find_library(
        log-lib
        log
)

# Link JNI library against stockfish-core and Android log
target_link_libraries(
        stockfish-jni
        stockfish-core
        ${log-lib}
)

