cmake_minimum_required(VERSION 3.6.0)

project(LibSigSys)

add_library(libsigsys SHARED libsigsys.c)

find_library(log-lib log)

target_link_libraries(libsigsys ${log-lib})
