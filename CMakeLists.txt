# 使用 find_package 查找 portaudio
find_package(portaudio CONFIG REQUIRED)

# 将 portaudio_static 链接到目标（例如 main）
target_link_libraries(main PRIVATE portaudio_static)