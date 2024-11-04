#include <iostream>
#include <portaudio.h>
#include <fstream>
#include <vector>
#include <cstring>
#include <locale>
#include <filesystem>

constexpr int SAMPLE_RATE = 44100;
constexpr int NUM_CHANNELS = 1;
constexpr int FRAMES_PER_BUFFER = 512;
constexpr int DURATION = 5;  // 录音时长，单位秒

struct AudioData {
    std::vector<float> samples;
};

static int recordCallback(const void *inputBuffer, void *outputBuffer,
                          unsigned long framesPerBuffer,
                          const PaStreamCallbackTimeInfo* timeInfo,
                          PaStreamCallbackFlags statusFlags,
                          void *userData) {
    AudioData *data = (AudioData*)userData;
    const float *in = (const float*)inputBuffer;

    if (inputBuffer != nullptr) {
        data->samples.insert(data->samples.end(), in, in + framesPerBuffer);
    }
    return paContinue;
}

int main() {
    Pa_Initialize();
    PaStream *stream;
    AudioData data;

    Pa_OpenDefaultStream(&stream, NUM_CHANNELS, 0, paInt16, SAMPLE_RATE,  // 使用 paInt16
                         FRAMES_PER_BUFFER, recordCallback, &data);
    Pa_StartStream(stream);
    std::cout << "recording..." << std::endl;
    Pa_Sleep(DURATION * 1000);  // 录音时间
    Pa_StopStream(stream);
    Pa_CloseStream(stream);
    Pa_Terminate();

   // 保存为 WAV 文件到指定路径
std::string outputPath = "audio_signal.wav"; // 指定文件保存路径
std::ofstream file(outputPath, std::ios::binary);
file << "RIFF";  // Chunk ID
file.write("\0\0\0\0", 4);  // Chunk Size
file << "WAVE";  // Format
file << "fmt ";  // Subchunk1 ID
int subChunk1Size = 16;
file.write(reinterpret_cast<const char*>(&subChunk1Size), 4);  // Subchunk1 Size
short audioFormat = 1;  // PCM格式
file.write(reinterpret_cast<const char*>(&audioFormat), 2);
file.write(reinterpret_cast<const char*>(&NUM_CHANNELS), 2);
file.write(reinterpret_cast<const char*>(&SAMPLE_RATE), 4);
int byteRate = SAMPLE_RATE * NUM_CHANNELS * sizeof(short); // 使用short (16-bit PCM)
file.write(reinterpret_cast<const char*>(&byteRate), 4);
short blockAlign = NUM_CHANNELS * sizeof(short); // 使用short (16-bit PCM)
file.write(reinterpret_cast<const char*>(&blockAlign), 2);
short bitsPerSample = 16;  // 16位
file.write(reinterpret_cast<const char*>(&bitsPerSample), 2);
file << "data";  // Subchunk2 ID
int subChunk2Size = data.samples.size() * sizeof(short); // 使用short (16-bit PCM)
file.write(reinterpret_cast<const char*>(&subChunk2Size), 4);
file.write(reinterpret_cast<const char*>(data.samples.data()), subChunk2Size);


    // 更新文件大小
    file.seekp(4, std::ios::beg);
    int fileSize = 36 + subChunk2Size;
    file.write(reinterpret_cast<const char*>(&fileSize), 4);
    file.close();

    std::cout << outputPath << std::endl;
    return 0;
}
