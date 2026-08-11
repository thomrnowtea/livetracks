cmake_minimum_required(VERSION 3.22.1)
project(livetracks_audio LANGUAGES CXX)

find_package(oboe REQUIRED CONFIG)

add_library(
    livetracks_audio
    SHARED
    AudioEngine.cpp
    JniBridge.cpp
)

target_compile_features(livetracks_audio PRIVATE cxx_std_20)
target_include_directories(livetracks_audio PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(livetracks_audio PRIVATE oboe::oboe android log)

