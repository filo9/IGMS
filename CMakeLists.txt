cmake_minimum_required(VERSION 3.10)

project(RecordAudio)

# 查找PortAudio库
set(CMAKE_TOOLCHAIN_FILE "C:/Users/17285/vcpkg/scripts/buildsystems/vcpkg.cmake")
find_package(PortAudio REQUIRED)

# 添加可执行文件
add_executable(record C:/Users/17285/Desktop/MFA2/record.cpp)

# 指定PortAudio的包含和链接路径
target_include_directories(record PRIVATE "C:/Users/17285/vcpkg/installed/x64-windows/include")
target_link_directories(record PRIVATE "C:/Users/17285/vcpkg/installed/x64-windows/lib")
target_link_libraries(record PRIVATE PortAudio::PortAudio)
