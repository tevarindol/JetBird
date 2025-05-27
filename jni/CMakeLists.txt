cmake_minimum_required(VERSION 3.6.0)

project(LibSigSys)

cmake_path(SET base_path NORMALIZE "${CMAKE_CURRENT_LIST_DIR}/..")

# Set paths for reproducible builders
add_link_options("LINKER:--build-id=none")
add_compile_options("-ffile-prefix-map=$base_path=/build/repo")
add_compile_options("-ffile-prefix-map=$ENV{ANDROID_HOME}=sdk")

add_library(libsigsys SHARED libsigsys.c)

find_library(log-lib log)

target_link_libraries(libsigsys ${log-lib})
